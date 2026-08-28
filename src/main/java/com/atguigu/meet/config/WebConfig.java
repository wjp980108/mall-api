package com.atguigu.meet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description
 * @Date 2026-05-14 11:29
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /** 本地存储根目录，与 application.yml 中 upload.base-path 共用（x-file-storage local-1 同源） */
    @Value("${upload.base-path}")
    private String uploadBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 访问 /upload/** 时, 映射到本地存储根目录
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadBasePath);
    }
}
