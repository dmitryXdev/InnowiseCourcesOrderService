package com.innowise.orderservice.feign.fallbackfactory;

import com.innowise.orderservice.exception.ServiceUnavailableException;
import com.innowise.orderservice.feign.AuthServiceClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallbackFactory implements FallbackFactory<AuthServiceClient> {
    @Override
    public AuthServiceClient create(Throwable cause) {
        return client -> {
            throw new ServiceUnavailableException(cause.getMessage());
        };
    }
}
