package com.example.blog.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@Component
public class FileSystemChecker implements CommandLineRunner {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void run(String... args) {
        System.out.println("\n=== 文件系统检查开始 ===");
        checkUploadDirectory();
        System.out.println("=== 文件系统检查结束 ===\n");
    }

    private void checkUploadDirectory() {
        System.out.println("检查上传目录: " + uploadDir);
        
        // 1. 检查目录是否存在
        File dir = new File(uploadDir);
        if (dir.exists()) {
            System.out.println("√ 上传目录存在");
        } else {
            System.out.println("× 上传目录不存在，尝试创建...");
            boolean created = dir.mkdirs();
            System.out.println(created ? "√ 目录创建成功" : "× 目录创建失败");
        }
        
        // 2. 检查权限
        System.out.println("检查目录权限:");
        System.out.println("- 可读: " + (dir.canRead() ? "是" : "否"));
        System.out.println("- 可写: " + (dir.canWrite() ? "是" : "否"));
        System.out.println("- 可执行: " + (dir.canExecute() ? "是" : "否"));
        
        // 3. 创建blogs子目录
        File blogsDir = new File(dir, "blogs");
        if (blogsDir.exists()) {
            System.out.println("√ blogs子目录存在");
        } else {
            System.out.println("× blogs子目录不存在，尝试创建...");
            boolean created = blogsDir.mkdirs();
            System.out.println(created ? "√ blogs子目录创建成功" : "× blogs子目录创建失败");
        }
        
        // 4. 尝试写入测试文件
        try {
            File testFile = new File(blogsDir, "test_" + System.currentTimeMillis() + ".txt");
            System.out.println("尝试写入测试文件: " + testFile.getAbsolutePath());
            
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                String content = "测试文件 - 创建于 " + new Date();
                fos.write(content.getBytes());
            }
            
            if (testFile.exists() && testFile.length() > 0) {
                System.out.println("√ 测试文件写入成功");
                // 清理测试文件
                testFile.delete();
            } else {
                System.out.println("× 测试文件写入失败");
            }
        } catch (IOException e) {
            System.err.println("× 写入测试文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 5. 检查目录是否绝对路径
        Path path = Paths.get(uploadDir);
        System.out.println("目录是否绝对路径: " + (path.isAbsolute() ? "是" : "否"));
        
        // 6. 显示绝对路径
        System.out.println("绝对路径: " + path.toAbsolutePath().toString());
        
        // 7. 检查磁盘空间
        try {
            long freeSpace = Files.getFileStore(path).getUsableSpace();
            System.out.println("可用磁盘空间: " + (freeSpace / 1024 / 1024) + " MB");
        } catch (IOException e) {
            System.err.println("× 无法获取磁盘空间信息: " + e.getMessage());
        }
    }
} 