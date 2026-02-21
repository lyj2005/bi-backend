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

}




