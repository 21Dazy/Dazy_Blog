package com.example.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileSystemInitializer implements CommandLineRunner {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void run(String... args) {
        System.out.println("\n=== 文件系统初始化开始 ===");
        initializeUploadDirectories();
        testDirectoryPermissions();
        System.out.println("=== 文件系统初始化结束 ===\n");
    }

    private void initializeUploadDirectories() {
        try {
            // 创建主目录
            Path mainDir = Paths.get(uploadDir);
            if (!Files.exists(mainDir)) {
                Files.createDirectories(mainDir);
                System.out.println("创建主上传目录: " + mainDir.toAbsolutePath());
            }

            // 创建blogs子目录
            Path blogsDir = Paths.get(uploadDir, "blogs");
            if (!Files.exists(blogsDir)) {
                Files.createDirectories(blogsDir);
                System.out.println("创建blogs目录: " + blogsDir.toAbsolutePath());
            }

            // 创建avatar子目录
            Path avatarDir = Paths.get(uploadDir, "avatar");
            if (!Files.exists(avatarDir)) {
                Files.createDirectories(avatarDir);
                System.out.println("创建avatar目录: " + avatarDir.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("初始化上传目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testDirectoryPermissions() {
        try {
            // 测试主目录
            testDirectoryWritePermission(Paths.get(uploadDir).toFile());
            
            // 测试blogs子目录
            testDirectoryWritePermission(Paths.get(uploadDir, "blogs").toFile());
            
            // 测试avatar子目录
            testDirectoryWritePermission(Paths.get(uploadDir, "avatar").toFile());
        } catch (Exception e) {
            System.err.println("测试目录权限失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testDirectoryWritePermission(File directory) {
        System.out.println("测试目录写入权限: " + directory.getAbsolutePath());
        
        if (!directory.exists()) {
            System.err.println("目录不存在，无法测试写入");
            return;
        }
        
        try {
            // 尝试创建一个临时文件
            File tempFile = new File(directory, "test_" + System.currentTimeMillis() + ".tmp");
            boolean created = tempFile.createNewFile();
            System.out.println("创建临时测试文件结果: " + created);
            
            if (created) {
                // 尝试写入一些数据
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write("测试写入".getBytes());
                    System.out.println("写入测试数据成功");
                }
                
                // 删除临时文件
                boolean deleted = tempFile.delete();
                System.out.println("删除临时测试文件结果: " + deleted);
            }
        } catch (Exception e) {
            System.err.println("目录写入测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}