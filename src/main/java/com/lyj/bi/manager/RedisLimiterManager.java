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

    //1. 创建一个实例
    @Resource
    private RedissonClient redisson;

    /**
     * 2. 限流
     * @param key
     */
    public void doRateLimiter(String key) {
        // 1. 获取 RateLimiter限流器
        RRateLimiter rateLimiter = redisson.getRateLimiter(key);

        // 2. 初始化限流规则（每秒 5 个请求）
        rateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.SECONDS);

        //3. 当操作来了，取令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        if (!canOp) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

    }

}
