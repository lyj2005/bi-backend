package com.lyj.bi.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.lyj.bi.annotation.AuthCheck;
import com.lyj.bi.bizmq.bi.BIMessageProducer;
import com.lyj.bi.common.BaseResponse;
import com.lyj.bi.common.DeleteRequest;
import com.lyj.bi.common.ErrorCode;
import com.lyj.bi.common.ResultUtils;
import com.lyj.bi.constant.CommonConstant;
import com.lyj.bi.constant.UserConstant;
import com.lyj.bi.exception.BusinessException;
import com.lyj.bi.exception.ThrowUtils;
import com.lyj.bi.manager.AiManager;
import com.lyj.bi.manager.RedisLimiterManager;
import com.lyj.bi.model.dto.chart.*;
import com.lyj.bi.model.entity.Chart;
import com.lyj.bi.model.entity.User;
import com.lyj.bi.model.vo.BiResponse;
import com.lyj.bi.service.ChartService;
import com.lyj.bi.service.UserService;
import com.lyj.bi.utils.ExcelUtils;
import com.lyj.bi.utils.SqlUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BIMessageProducer biMessageProducer;


    // region 增删改查代码

    /**
     * 创建图表
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        //1. 校验输入
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 创建新对象
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        //3. 获取当前用户，填入id
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        //4. 保存到数据库
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        //5. 返回
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除图表
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        //1. 校验输入
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 获取当前用户
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        //3. 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        //4. 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        //5. 返回
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        //1. 校验输入
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 创建新对象
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        //3.  判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        //4. 修改数据库
        boolean result = chartService.updateById(chart);
        //5. 返回
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        //1. 校验输入
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 查询数据库
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //3. 返回
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        //1. 获取当前图表和大小
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        //2. 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //3. 查询
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        //4. 返回
        return ResultUtils.success(chartPage);
    }

    /**
     * 我的图表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        //1. 校验输入
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 获取当前用户
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        //3. 获取当前图表和大小
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        //4. 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //5. 查询数据库
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        //6. 返回
        return ResultUtils.success(chartPage);
    }


    // endregion


    /**
     * AI分析图表
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genCharByAi(@RequestPart("file") MultipartFile multipartFile,
                                                GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        //1. 参数校验
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        //①分析目标不为空，否则参数异常，并提示
        ThrowUtils.throwIf(StringUtils.isEmpty(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        //②图表名称不为空，且字数超过100字，并提示
        ThrowUtils.throwIf(StringUtils.isEmpty(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "图表名称为空");
        //③校验用户，必须登录才能使用，拿到用户id
        User loginUser = userService.getLoginUser(request);

        //优化2：限流，防止用户狂刷AI
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //优化1：上传的文件安全性
        //1. 文件大小校验  ---  判断文件大小是否超过1MB，超过则提示
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过1M");
        //2. 文件后缀校验   --  判断文件后缀是否在合法的文件后缀中，不是则提示
        //①拿到文件名
        String originalFilename = multipartFile.getOriginalFilename();
        //②拿到文件后缀  --  利用FileUtils
        String suffix = FileUtil.getSuffix(originalFilename);
        //③判断文件后缀是否在合法的文件后缀中，不是则提示
        final List<String> validSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        //2. 构建用户输入  --  根据prompt编写
        /**（参考）
         分析需求：
         (分析网站用户的增长情况)[，请使用雷达图]
         原始数据：
         (日期，用户数
         1号，10
         2号，20
         3号，30)
         */
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("/n");
        //①拼接分析目标
        String userGoal = goal;
        //②拼接图表类型,如果为非空，才拼接在最后
        if (!StringUtils.isEmpty(chartType)) {
            userGoal = userGoal + "，请使用" + chartType;
        }
        userInput.append(userGoal).append("/n");
        //③拼接转换后的图表
        userInput.append("原始数据:").append("/n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("/n");

        //3. 调用AI,得到结果（genChart,genResult）
        String result = aiManager.doChat(userInput.toString());

        //4. 处理结果
        //①依照【【【【【拆分结果，得到字符数组splits
        String[] splits = result.split("【【【【【");
        //②校验
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
        }
        //③得到（genChart,genResult）,需要去掉多余空格，使用trim方法
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        //5. 保存图表
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        //6. 返回结果
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);


    }


    /**
     * AI分析图表(线程池异步化）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genCharByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                     GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        //1. 参数校验
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        //①分析目标不为空，否则参数异常，并提示
        ThrowUtils.throwIf(StringUtils.isEmpty(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        //②图表名称不为空，且字数超过100字，并提示
        ThrowUtils.throwIf(StringUtils.isEmpty(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "图表名称为空");
        //③校验用户，必须登录才能使用，拿到用户id
        User loginUser = userService.getLoginUser(request);

        //优化2：限流，防止用户狂刷AI
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //优化1：上传的文件安全性
        //1. 文件大小校验  ---  判断文件大小是否超过1MB，超过则提示
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过1M");
        //2. 文件后缀校验   --  判断文件后缀是否在合法的文件后缀中，不是则提示
        //①拿到文件名
        String originalFilename = multipartFile.getOriginalFilename();
        //②拿到文件后缀  --  利用FileUtils
        String suffix = FileUtil.getSuffix(originalFilename);
        //③判断文件后缀是否在合法的文件后缀中，不是则提示
        final List<String> validSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        //2. 构建用户输入  --  根据prompt编写
        /**（参考）
         分析需求：
         (分析网站用户的增长情况)[，请使用雷达图]
         原始数据：
         (日期，用户数
         1号，10
         2号，20
         3号，30)
         */
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("/n");
        //①拼接分析目标
        String userGoal = goal;
        //②拼接图表类型,如果为非空，才拼接在最后
        if (!StringUtils.isEmpty(chartType)) {
            userGoal = userGoal + "，请使用" + chartType;
        }
        userInput.append(userGoal).append("/n");
        //③拼接转换后的图表
        userInput.append("原始数据:").append("/n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("/n");


        //优化3 异步化改造（从这里改）

        //3. 先保存图表
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        //还没有生成，将这里改为设置状态  --  排队中wait
//        chart.setGenChart(genChart);
//        chart.setGenResult(genResult);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        //4. 使用线程池执行异步化任务  --  在最终的返回结果前提交一个任务
        CompletableFuture.runAsync(() -> {

            //先修改图表任务状态为“执行中”。等执行成功后，修改为“已完成”、保存执行结果；执行失败后，状态修
            //改为“失败”，记录任务失败信息。（为了防止同一个任务被多次执行）

            //1. 修改图表任务状态为“执行中”，提交到数据库
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean updateResult = chartService.updateById(updateChart);
            //如果提交失败，数据库出问题了
            if (!updateResult) {
                handleChartUpdateException(chart.getId(), "修改图表任务状态为“执行中”失败");
                return;//终止当前任务，注意不是员工  --  线程
            }

//2. 调用AI,得到结果（genChart,genResult）
            String result = aiManager.doChat(userInput.toString());

            //4. 处理结果
            //①依照【【【【【拆分结果，得到字符数组splits
            String[] splits = result.split("【【【【【");
            //②校验
            if (splits.length < 3) {
                handleChartUpdateException(chart.getId(), "AI生成错误");
            }
            //③得到（genChart,genResult）,需要去掉多余空格，使用trim方法
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            //3. 再次更新数据库
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean b = chartService.updateById(updateChartResult);
            if (!b) {
                handleChartUpdateException(chart.getId(), "更新数据库失败");
            }


        }, threadPoolExecutor);

        //5. 返回结果
        BiResponse response =new BiResponse();
        response.setChartId(chart.getId());
        return ResultUtils.success(response);


    }




        /**
         * 处理图表更新异常工具类,更新图表执行信息
         */
        private void handleChartUpdateException ( long chartId, String execMessage){
            //1.保存到数据库
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            updateChartResult.setStatus("failed");
            updateChartResult.setExecMessage(execMessage);
            boolean result = chartService.updateById(updateChartResult);
            //2. 如果更新失败，打日志
            if (!result) {
                log.error("更新图表执行信息失败，chartId:{},execMessage:{}", chartId, execMessage);
            }
        }




    /**
     * AI分析图表(MQ异步化）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genCharByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                       GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        //1. 参数校验
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        //①分析目标不为空，否则参数异常，并提示
        ThrowUtils.throwIf(StringUtils.isEmpty(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        //②图表名称不为空，且字数超过100字，并提示
        ThrowUtils.throwIf(StringUtils.isEmpty(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "图表名称为空");
        //③校验用户，必须登录才能使用，拿到用户id
        User loginUser = userService.getLoginUser(request);

        //优化2：限流，防止用户狂刷AI
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //优化1：上传的文件安全性
        //1. 文件大小校验  ---  判断文件大小是否超过1MB，超过则提示
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过1M");
        //2. 文件后缀校验   --  判断文件后缀是否在合法的文件后缀中，不是则提示
        //①拿到文件名
        String originalFilename = multipartFile.getOriginalFilename();
        //②拿到文件后缀  --  利用FileUtils
        String suffix = FileUtil.getSuffix(originalFilename);
        //③判断文件后缀是否在合法的文件后缀中，不是则提示
        final List<String> validSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        //2. 构建用户输入  --  根据prompt编写
        /**（参考）
         分析需求：
         (分析网站用户的增长情况)[，请使用雷达图]
         原始数据：
         (日期，用户数
         1号，10
         2号，20
         3号，30)
         */
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("/n");
        //①拼接分析目标
        String userGoal = goal;
        //②拼接图表类型,如果为非空，才拼接在最后
        if (!StringUtils.isEmpty(chartType)) {
            userGoal = userGoal + "，请使用" + chartType;
        }
        userInput.append(userGoal).append("/n");
        //③拼接转换后的图表
        userInput.append("原始数据:").append("/n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("/n");


        //优化3 异步化改造（从这里改）

        //3. 先保存图表
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        //还没有生成，将这里改为设置状态  --  排队中wait
//        chart.setGenChart(genChart);
//        chart.setGenResult(genResult);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        long newChartId = chart.getId();
        //4. 使用MQ执行异步化任务  --  在最终的返回结果前提交一个任务
        biMessageProducer.sendMessage(String.valueOf(newChartId));


        //5. 返回结果
        BiResponse response =new BiResponse();
        response.setChartId(newChartId);
        return ResultUtils.success(response);


    }








    /**
         * 获取查询包装类
         *
         * @param chartQueryRequest
         * @return
         */
        private QueryWrapper<Chart> getQueryWrapper (ChartQueryRequest chartQueryRequest){
            //1. 创建容器
            QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
            //2. 校验
            if (chartQueryRequest == null) {
                return queryWrapper;
            }
            //3. 获取数据
            Long id = chartQueryRequest.getId();
            String name = chartQueryRequest.getName();
            String goal = chartQueryRequest.getGoal();
            String chartType = chartQueryRequest.getChartType();
            Long userId = chartQueryRequest.getUserId();
            String sortField = chartQueryRequest.getSortField();
            String sortOrder = chartQueryRequest.getSortOrder();
            //4. 构建sql
            queryWrapper.eq(id != null && id > 0, "id", id);
            queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
            queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
            queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
            queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
            queryWrapper.eq("isDelete", false);
            queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                    sortField);
            //5. 返回结果
            return queryWrapper;
        }


    }
