package com.innowise.orderservice.feign.fallbackfactory;

import com.innowise.orderservice.exception.ServiceUnavailableException;
import com.innowise.orderservice.feign.UserServiceClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {
    @Override
    public UserServiceClient create(Throwable cause) {
        return client -> {
            throw new ServiceUnavailableException(cause.getMessage());
        };
    }
}
