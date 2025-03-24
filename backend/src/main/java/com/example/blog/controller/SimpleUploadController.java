package com.example.blog.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SimpleUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("开始处理图片上传...");
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize() + " 字节");
            System.out.println("文件类型: " + file.getContentType());
            
            // 1. 创建上传目录
            File uploadPath = new File(uploadDir);
            if (!uploadPath.exists()) {
                if (!uploadPath.mkdirs()) {
                    System.err.println("无法创建上传目录: " + uploadPath.getAbsolutePath());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("上传失败: 无法创建上传目录");
                }
            }
            
            // 2. 创建blogs子目录
            File blogsDir = new File(uploadPath, "blogs");
            if (!blogsDir.exists()) {
                if (!blogsDir.mkdirs()) {
                    System.err.println("无法创建blogs目录: " + blogsDir.getAbsolutePath());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("上传失败: 无法创建blogs目录");
                }
            }
            
            // 3. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            
            // 4. 保存文件
            File destFile = new File(blogsDir, newFileName);
            System.out.println("保存文件到: " + destFile.getAbsolutePath());
            
            try {
                // 直接保存文件
                file.transferTo(destFile);
                System.out.println("文件保存成功");
            } catch (IOException e) {
                System.err.println("保存文件失败: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("上传失败: 文件保存出错 (" + e.getMessage() + ")");
            }
            
            // 5. 返回URL
            String fileUrl = "/uploads/blogs/" + newFileName;
            Map<String, Object> response = new HashMap<>();
            response.put("url", fileUrl);
            System.out.println("上传成功，URL: " + fileUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("图片上传发生异常: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("图片上传失败: " + e.getMessage());
        }
    }
} 