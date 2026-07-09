package com.atguigu.lease.web.app.custom.config;

import com.atguigu.lease.web.app.custom.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringMVCConfiguration implements WebMvcConfigurer {
    @Value("${web.app.auth-include}")
    private String[] includes;

    @Value("${web.app.auth-exclude}")
    private String[] excludes;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebMvcConfigurer.super.addInterceptors(registry);
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns(includes)
                .excludePathPatterns(excludes);
    }
}
