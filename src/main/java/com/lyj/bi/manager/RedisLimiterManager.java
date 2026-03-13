package com.lyj.bi.manager;

import com.lyj.bi.common.ErrorCode;
import com.lyj.bi.exception.BusinessException;
import jakarta.annotation.Resource;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 提供Redis限流基础服务的（提供通用的服务）
 */
@Service
public class RedisLimiterManager {

    //1. 拿到redissonClient客户端
    @Resource
    private RedissonClient redissonClient;


    public void doRateLimit(String key) {
        //1. 创建名为key的限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        //2. 设置限流器,设置每秒最多访问2次
        //参数解释：
        // 限流器的统计规则(每秒2个请求;连续的请求,最多只能有1个请求被允许通过)
        // RateType.OVERALL表示速率限制作用于整个令牌桶,即限制所有请求的速率
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        //3. 从限流器1次取1个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        //4. 没有令牌，抛出异常
        if (!canOp) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }

}
