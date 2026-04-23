package com.roadinspection.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Value("${file.result-path:./results}")
    private String resultPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/upload/**")
                .addResourceLocations("file:" + uploadPath + "/");
        registry.addResourceHandler("/files/result/**")
                .addResourceLocations("file:" + resultPath + "/");
    }
}
