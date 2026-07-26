package com.homework.web.app.config;

import com.homework.web.app.converter.StringToBaseEnumConverterFactory;
import com.homework.web.app.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final StringToBaseEnumConverterFactory stringToBaseEnumConverterFactory;

    public WebMvcConfig(LoginInterceptor loginInterceptor,
                        StringToBaseEnumConverterFactory stringToBaseEnumConverterFactory) {
        this.loginInterceptor = loginInterceptor;
        this.stringToBaseEnumConverterFactory = stringToBaseEnumConverterFactory;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(stringToBaseEnumConverterFactory);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/app/**")
                .excludePathPatterns("/api/app/auth/**");
    }
}
