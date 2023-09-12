package com.agileboot.infrastructure.annotations.ratelimit;

import com.agileboot.infrastructure.annotations.ratelimit.implementation.MapRateLimitChecker;
import com.agileboot.infrastructure.annotations.ratelimit.implementation.RedisRateLimitChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 限流切面处理
 *
 * @author valarchie
 */
@Aspect
@Component
@Slf4j
// 配置文件中，agileboot.embedded.redis 的值是不为 true。
@ConditionalOnExpression("'${agileboot.embedded.redis}' != 'true'")
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final RedisRateLimitChecker redisRateLimitChecker;

    private final MapRateLimitChecker mapRateLimitChecker;


    // 拦截有@RateLimit 注解的方法
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimit rateLimiter) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        log.info("当前限流方法:" + method.toGenericString());

        switch (rateLimiter.cacheType()) {
            case REDIS:
                redisRateLimitChecker.check(rateLimiter);
                break;
            case Map:
                mapRateLimitChecker.check(rateLimiter);
                return;
            default:
                redisRateLimitChecker.check(rateLimiter);
        }

    }

}
