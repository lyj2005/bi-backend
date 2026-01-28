package com.lyj.bi.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.lyj.bi.biMqConfig.BIMessageProducer;
import com.lyj.bi.common.ErrorCode;
import com.lyj.bi.componet.WebSocketServer;
import com.lyj.bi.constant.ModelConstant;
import com.lyj.bi.exception.BusinessException;
import com.lyj.bi.exception.ThrowUtils;
import com.lyj.bi.manager.AiManager;
import com.lyj.bi.manager.RedisLimiterManager;
import com.lyj.bi.mapper.ChartMapper;
import com.lyj.bi.mapper.UserMapper;
import com.lyj.bi.model.dto.chart.ChartQueryRequest;
import com.lyj.bi.model.dto.chart.GenChartByAiRequest;
import com.lyj.bi.model.entity.Chart;
import com.lyj.bi.model.entity.User;
import com.lyj.bi.model.enums.ChartStatusEnum;
import com.lyj.bi.model.vo.BiResponse;
import com.lyj.bi.service.ChartService;
import com.lyj.bi.utils.ExcelUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lyj.bi.common.ErrorCode.NOT_FOUND_ERROR;
import static com.lyj.bi.constant.RedisConstant.*;

@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    Gson gson = new Gson();

    //redis
    @Resource
    private RedisLimiterManager redisLimiterManager;
    //引入我们新定义的线程池
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    //
    @Resource
    private BIMessageProducer biMessageProducer;
    //redis
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    //注入
    @Resource
    private ModelConstant modelConstant;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ChartMapper chartMapper;
    //websocket
    @Resource
    private WebSocketServer webSocketService;
    //ai
    @Resource
    private AiManager aiManager;
    //临时数据结构存储
    List<ZSetOperations.TypedTuple<String>> chartListZset=new ArrayList<>();

    /**
     * AI智能分析
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public BiResponse genChartByAiService(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) throws FileNotFoundException {
        //输入信息
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //文件校验
        vaildFile(multipartFile);
        long biModelID = modelConstant.getModelId();

        /**
         * 将分析需求转成代码——
         *
         * ——分析需求：
         *         分析网站用户的增长情况
         *         原始数据：
         *         日期，用户数
         *         1号，10
         *         2号，20
         *         3号，30
         */
        //系统预设词
        //String promote = AIManager.SYSTEM_PROMPT + "分析需求 " + goal + " \n原始数据如下: " + cvsData + "\n生成图标的类型是: " + chartType;

        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");
        //压缩后的数据
        // 将excel文件转换为csv文件
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        // 将csv文件内容添加到userInput中
        userInput.append(csvData).append("\n");
        // 调用aiManager的doChart方法，传入biModelID和userInput
        String result = aiManager.doChat(biModelID, userInput.toString());

        // 将返回结果按"【【【【【"分割
        String[] splits = result.split("【【【【【");
        // 如果分割后的结果长度小于3，抛出异常
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        // 获取生成的图表和结果
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        //插入到数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setName(name);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        int saveResult = chartMapper.insert(chart);
        ThrowUtils.throwIf(saveResult != 1, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 创建BIResponse对象
        BiResponse biResponse = new BiResponse();
        // 设置生成的图表和结果
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    /**
     * AI模型智能分析
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public BiResponse genChartBySparkAiService(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) throws FileNotFoundException {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();

        //文件校验
        vaildFile(multipartFile);
        long biModelID = modelConstant.getModelId();

        //压缩后的数据
        // 将excel文件转换为csv文件
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        //分析对象 = 预设 + 分析需求（goal） + 原始数据（cvsData）+ 图表类型（chartType）
        String promote = AiManager.SYSTEM_PROMPT + "分析需求 " + goal + " \n原始数据如下: " + csvData + "\n生成图标的类型是: " + chartType;

        String resultData = aiManager.doChat(biModelID,promote);
        log.info("AI 生成的信息: {}", resultData);
        String[] splits = resultData.split("【【【【【");
        ThrowUtils.throwIf(splits.length < 3, ErrorCode.SYSTEM_ERROR);
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        //插入到数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setName(name);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(userId);
        int saveResult = chartMapper.insert(chart);
        ThrowUtils.throwIf(saveResult != 1, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 创建BIResponse对象
        BiResponse biResponse = new BiResponse();
        // 设置生成的图表和结果
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        //Aop动态代理
        ChartService currentProxy = (ChartService) AopContext.currentProxy();
        //AI调用次数扣减
        currentProxy.genChartByAICount(userId);
        return biResponse;
    }

    /**
     * 智能分析（异步线程池）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @return
     */
    @Override
    public BiResponse genChartByAiAsycnService(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) throws FileNotFoundException {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //文件校验
        vaildFile(multipartFile);
        long biModelID = modelConstant.getModelId();
        /**
         * 将分析需求转成代码——
         *
         * ——分析需求：
         *         分析网站用户的增长情况
         *         原始数据：
         *         日期，用户数
         *         1号，10
         *         2号，20
         *         3号，30
         */
        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");
        //压缩后的数据
        // 将excel文件转换为csv文件
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        // 将csv文件内容添加到userInput中
        userInput.append(csvData).append("\n");


        //将图表插入到数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setName(name);
        chart.setChartType(chartType);
        //插入图表时，还没生成结束，先去掉这两个
        //chart.setGenChart(genChart);
        //chart.setGenResult(genResult);
        //设置任务状态为等待中
        chart.setStatus(ChartStatusEnum.WAITING.getValue());
        chart.setUserId(loginUser.getId());
        int saveResult = chartMapper.insert(chart);
        ThrowUtils.throwIf(saveResult <= 0, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        //在最终返回结果前提交一个任务
        //todo 建议：处理任务队列满了以后抛异常的情况
        CompletableFuture.runAsync(() -> {
            //先修改图表任务状态为“”执行中”。等执行成功后，修改为“已完成”、保存执行结果；执行失败后，状态修改为“失败”，记录任务失败信息。
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(ChartStatusEnum.RUNNING.getValue());
            int b = chartMapper.updateById(updateChart);
            if (b <= 0) {
                handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }

            // 调用AI,aiManager的doChart方法，传入biModelID和userInput
            String result = aiManager.doChat(biModelID, userInput.toString());

            // 将返回结果按"【【【【【"分割
            String[] splits = result.split("【【【【【");
            // 如果分割后的结果长度小于3，抛出异常
            if (splits.length < 3) {
                handleChartUpdateError(chart.getId(), "AI 生成错误");
            }
            // 获取生成的图表和结果
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(ChartStatusEnum.SUCCEED.getValue());
            int updateResult = chartMapper.updateById(updateChartResult);
            if (updateResult <= 0) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }
        }, threadPoolExecutor);
        // 创建BIResponse对象
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    /**
     * 智能分析（异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     */
    @Override
    public BiResponse genChartByAiMQService(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) throws FileNotFoundException {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();
        //文件校验
        vaildFile(multipartFile);

        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        //压缩后的数据
        // 将excel文件转换为csv文件
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        // 将csv文件内容添加到userInput中
        userInput.append(csvData).append("\n");

        //将图表插入到数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setName(name);
        chart.setChartType(chartType);
        //设置任务状态为等待中
        chart.setStatus(ChartStatusEnum.WAITING.getValue());
        chart.setUserId(userId);
        int saveResult = chartMapper.insert(chart);
        ThrowUtils.throwIf(saveResult <= 0, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        Long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));
        // 创建BIResponse对象
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    /**
     * 图表列表信息缓存
     *
     * @return
     */
    @Override
    public List<Chart> listChartByCacheService() {
        //从缓存中获取key中的chart表json格式数据
        String chartjson = stringRedisTemplate.opsForValue().get(CHART_LIST_CACHE_KEY);
        if (StrUtil.isNotBlank(chartjson)) {
            //用于将JSON数据转换为Java对象
            List<Chart> chartList = JSONUtil.toList(chartjson, Chart.class);
            log.info("从缓存中获取chart表数据");
            return chartList;
        }
        //数据库中查询chartList数据
        List<Chart> chartList02 = chartMapper.selectList(null);
        //判断chartList02不为空
        if (chartList02 == null) {
            throw new BusinessException(NOT_FOUND_ERROR);
        }
        //将数据库中的数据进行缓存
        //1、先将Java对象转换成json格式
        String cachechartjson = JSONUtil.toJsonStr(chartList02);
        //判断cachechartjson不为空
        if (cachechartjson == null) {
            throw new BusinessException(NOT_FOUND_ERROR);
        }
        stringRedisTemplate.opsForValue().set(CHART_LIST_CACHE_KEY, cachechartjson
                , CHART_LIST_CACHE_TIME, TimeUnit.MINUTES);
        log.info("成功将chart表数据存入缓存中");
        return chartList02;
    }

    /**
     * 分页获取图表列表缓存信息
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @Override
    public List<Chart> listChartByCachePage(ChartQueryRequest chartQueryRequest,
                                            HttpServletRequest request) {
        long pageNumber = chartQueryRequest.getCurrent();//当前页数
        long pageSize = chartQueryRequest.getPageSize();// 每一页的数量

        long startIndex = (pageNumber - 1) * pageSize;
        long endIndex = startIndex + pageSize - 1;
        Set<String> elements = stringRedisTemplate.opsForZSet().range(CHART_LIST_CHCHE_KEY, startIndex, endIndex);
        if (elements != null && elements.size() > 0) {
            // 使用map操作将每个字符串转换为Chart对象，并收集到List<Chart>中
            List<Chart> CacheChartList = elements.stream()
                    //将json字符串转换成chart对象
                    .map(Chart -> gson.fromJson(Chart, Chart.class)
                    ).collect(Collectors.toList());
            log.info("从缓存中获取chart表数据");
            return CacheChartList;
        }

        //数据库中查询chartList数据
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        List<Chart> chartList = chartMapper.selectList(queryWrapper);
        //判断chartList02不为空
        if (chartList == null) {
            log.error("图表列表信息不存在！！！");
            throw new BusinessException(NOT_FOUND_ERROR);
        }
        for (int i = 0; i < chartList.size(); i++) {
            Chart chart = chartList.get(i);
            // 将chart对象转换为字符串，这里假设你有一个方法可以将Chart对象转换为JSON字符串
            String chartJson = gson.toJson(chart);
            // 将chartJson添加到ZSet中，分数为i
            //stringRedisTemplate.opsForZSet().add(CHART_LIST_CHCHE_KEY, chartJson, i);
            chartListZset.add(new DefaultTypedTuple<>(chartJson, (double) i));
            log.info("chart图表数据导入成功,id:{}",chart.getId());
        }
        // 将 List 转换为 Set
        Set<ZSetOperations.TypedTuple<String>> chartDataZset = new HashSet<>(chartListZset);
        stringRedisTemplate.opsForZSet().add(CHART_LIST_CHCHE_KEY,chartDataZset);
        Page<Chart> chartPageList = this.page(new Page<>(pageNumber, pageSize), queryWrapper);
        log.info("chart图表数据导入成功,总条数:{}",chartList.size());
        return chartPageList.getRecords();
    }

    /**
     * 获取单个图表缓存
     *
     * @param id
     * @return
     */
    @Override
    public Chart getChartByIdCache(long id) {

        //判断id是否为空
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //拼接chartCacheId
        String chartCacheId = CHART_CHCHE_ID + id;
        //从redis中获取chartCacheData
        String chartCacheData = stringRedisTemplate.opsForValue().get(chartCacheId);
        //判断chartId不为空
        if (StrUtil.isNotBlank(chartCacheData)) {
            ////创建ObjectMapper对象
            //ObjectMapper objectMapper = new ObjectMapper();
            ////将chartCacheData转换成Chart对象
            //Chart chart = objectMapper.readValue(chartCacheData, Chart.class);
            Chart chart = JSONUtil.toBean(chartCacheData, Chart.class);
            //Chart chart = gson.fromJson(chartCacheData, Chart.class);
            //返回成功结果
            log.info("成功从Redis缓存中获取到数据，图表Key：{}",chartCacheId);
            return chart;
        }

        //根据id查询图表
        Chart chart = chartMapper.selectById(id);
        //判断图表是否为空
        if (chart == null) {
            throw new BusinessException(NOT_FOUND_ERROR);
        }
        //将图表转换成json字符串
        String chartDataJson = JSONUtil.toJsonStr(chart);
        //将图表缓存到redis中
        stringRedisTemplate.opsForValue().set(chartCacheId, chartDataJson, CHART_CACHE_TIME, TimeUnit.MINUTES);
        log.info("成功将图表缓存到Redis中，图表Key：{}",chartCacheId);
        return chart;
    }

    /**
     * ai调用次数统计
     * <p color="yellow">检查用户ID是否为空和次数扣减应该为一个整体，保证数据一致性</p>
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public Integer genChartByAICount(Long userId) {
        //判断chartId是否为空
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图表不存在！");
        }
        //根据用户ID查询用户并进行次数扣减
        //SQL语句：update user set count=count-1 where id = #{userId} and count > 0
        UpdateWrapper <User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",userId);
        updateWrapper.gt("number",0);
        updateWrapper.setSql("number=number-1");
        int updateResult = userMapper.update(null, updateWrapper);
        //判断更新结果
        if (updateResult != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI调用次数扣减失败！");
        }
        //webSocket消息实时推送
        webSocketService.sendOneMessage(userId,"AI调用次数已扣减！");
        return updateResult;
    }



    /**
     * 校验文件
     *
     * @param multipartFile
     */
    private void vaildFile(MultipartFile multipartFile) {
        // 获取文件大小
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大,超过1M");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> vaildFileSuffixList = Arrays.asList("png", "jpg", "jpeg", "svg", "webp", "xlsx");
        ThrowUtils.throwIf(!vaildFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
    }

    /**
     * 上面接口很多用到异常
     * 创建一个回调函数，对图表状态失败这一情况进行集中异常处理
     */
    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setExecMessage("execMessage");
        updateChartResult.setStatus(ChartStatusEnum.FAILED.getValue());
        int updateResult = chartMapper.updateById(updateChartResult);
        if (updateResult <= 0) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }

}




