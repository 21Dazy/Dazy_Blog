package com.example.blog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class BlogApplication {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
    
    @Bean
    CommandLineRunner init() {
        return args -> {
            // 创建上传根目录
            Path rootPath = Paths.get(uploadDir);
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
                System.out.println("创建上传根目录: " + rootPath.toAbsolutePath());
            }
            
            // 创建blogs子目录
            Path blogsPath = Paths.get(uploadDir, "blogs");
            if (!Files.exists(blogsPath)) {
                Files.createDirectories(blogsPath);
                System.out.println("创建blogs上传目录: " + blogsPath.toAbsolutePath());
            }
            
            // 创建avatar子目录
            Path avatarPath = Paths.get(uploadDir, "avatar");
            if (!Files.exists(avatarPath)) {
                Files.createDirectories(avatarPath);
                System.out.println("创建avatar上传目录: " + avatarPath.toAbsolutePath());
            }
            
            System.out.println("文件上传目录初始化完成.");
        };
    }
} 