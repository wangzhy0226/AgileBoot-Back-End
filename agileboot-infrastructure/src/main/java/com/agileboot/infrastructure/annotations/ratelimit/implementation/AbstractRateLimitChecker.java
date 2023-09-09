package com.agileboot.infrastructure.annotations.ratelimit.implementation;

import com.agileboot.infrastructure.annotations.ratelimit.RateLimit;

/**
 * 接口限流检查器者抽象类
 * @author valarchie
 */
public abstract class AbstractRateLimitChecker {

    /**
     * 检查是否超出限流
     *
     * @param rateLimiter RateLimit
     */
    public abstract void check(RateLimit rateLimiter);

}
