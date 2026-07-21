package com.aerionsoft.application.config;

import java.lang.reflect.Type;
import java.util.Set;

import com.aerionsoft.application.context.UserTimezoneContext;
import com.aerionsoft.application.dto.HasUserTimeOffset;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice
public class UserTimezoneBodyAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        applyBodyOffset(body);
        return body;
    }

    private void applyBodyOffset(Object body) {
        if (body instanceof HasUserTimeOffset offsetAware) {
            String bodyOffset = offsetAware.getUserTimeOffset();
            if (bodyOffset != null && !bodyOffset.isBlank()) {
                UserTimezoneContext.set(bodyOffset);
            }
            return;
        }

        if (body instanceof Set<?> set) {
            set.forEach(this::applyBodyOffset);
        }
    }
}
