package com.example.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${cors.max-age}")
    private long maxAge;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保上传目录存在
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            boolean dirCreated = uploadDirFile.mkdirs();
            System.out.println("创建上传目录结果: " + dirCreated + ", 路径: " + uploadDirFile.getAbsolutePath());
        }
        
        // 获取上传目录的绝对路径
        String uploadAbsolutePath = uploadDirFile.getAbsolutePath();
        
        // 确保路径以/结尾
        if (!uploadAbsolutePath.endsWith("/") && !uploadAbsolutePath.endsWith("\\")) {
            uploadAbsolutePath = uploadAbsolutePath + File.separator;
        }
        
        System.out.println("配置文件上传路径: " + uploadAbsolutePath);
        
        // 添加资源处理器，映射 /uploads/** 到实际上传目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath);
                
        // 添加资源处理器，映射 /api/uploads/** 到实际上传目录
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath);
                
        System.out.println("静态资源映射已配置:");
        System.out.println("虚拟路径: /uploads/** 和 /api/uploads/** 映射到物理路径: file:" + uploadAbsolutePath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 不使用*，使用特定的起源列表
                .allowedOriginPatterns("http://localhost:8082", "http://127.0.0.1:8082")
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(allowedHeaders.split(","))
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converters.add(converter);
    }
} 