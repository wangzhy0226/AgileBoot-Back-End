package com.agileboot.infrastructure.annotations.unrepeatable;

import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.infrastructure.cache.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 重复提交拦截器 如果涉及前后端加解密的话  也可以通过继承RequestBodyAdvice来实现
 *
 * @author valarchie
 */
@ControllerAdvice(basePackages = "com.agileboot")
@Slf4j
@RequiredArgsConstructor
public class UnrepeatableInterceptor extends RequestBodyAdviceAdapter {

    private final RedisUtil redisUtil;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType) {
        // 当方法含有 @Unrepeatable 注解时拦截这个请求 comment by wangzhy 2023.9.12
        return methodParameter.hasMethodAnnotation(Unrepeatable.class);
    }

    /**
     * @param body 仅获取有 @RequestBody注解的参数
     */
    @NotNull
    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
        Class<? extends HttpMessageConverter<?>> converterType) {
        // 仅获取有RequestBody注解的参数
        MD5 md5 = MD5.create();
        String currentRequest = JSONUtil.toJsonStr(body);
        // 在 redis 中 存放 hash 值， 不存放原始数据，以节省 redis 空间。 comment by wangzhy 2023.9.12
        currentRequest = md5.digestHex(currentRequest);
        Unrepeatable resubmitAnno = parameter.getMethodAnnotation(Unrepeatable.class);
        if (resubmitAnno != null) {
            String redisKey = resubmitAnno.checkType().generateResubmitRedisKey(parameter.getMethod());

            log.info("请求重复提交拦截，当前key:{}, 当前参数：{}", redisKey, currentRequest);

            String preRequest = redisUtil.getCacheObject(redisKey);
            if (preRequest != null) {
                boolean isSameRequest = Objects.equals(currentRequest, preRequest);

                if (isSameRequest) {
                    throw new ApiException(ErrorCode.Client.COMMON_REQUEST_RESUBMIT);
                }
            }
            redisUtil.setCacheObject(redisKey, currentRequest, resubmitAnno.interval(), TimeUnit.SECONDS);
        }

        return body;
    }

}
