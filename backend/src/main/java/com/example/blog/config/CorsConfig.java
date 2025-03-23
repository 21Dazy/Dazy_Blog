package com.example.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许所有来源
        config.addAllowedOriginPattern("*");
        
        // 允许所有HTTP方法
        config.addAllowedMethod("*");
        
        // 允许所有头部
        config.addAllowedHeader("*");
        
        // 允许携带凭证信息（例如Cookie）
        config.setAllowCredentials(true);
        
        // 预检请求的有效期，单位秒
        config.setMaxAge(3600L);
        
        // 对所有路径应用这些CORS配置
        source.registerCorsConfiguration("/**", config);
        
        System.out.println("CORS配置已加载：允许所有来源、方法和头部");
        return new CorsFilter(source);
    }
} 