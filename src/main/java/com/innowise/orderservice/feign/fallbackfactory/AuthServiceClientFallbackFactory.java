package com.innowise.orderservice.feign.fallbackfactory;

import com.innowise.orderservice.feign.AuthServiceClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallbackFactory implements FallbackFactory<AuthServiceClient> {
    @Override
    public AuthServiceClient create(Throwable cause) {
        return client -> {
            if(cause instanceof RuntimeException exception) {
                throw exception;
            }

            throw new RuntimeException(cause);
        };
    }
}
