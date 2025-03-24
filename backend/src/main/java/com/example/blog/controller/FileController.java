package com.example.blog.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

@RestController
//@RequestMapping("/blogs")
@CrossOrigin(origins = "localhost:8082", maxAge = 3600)
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/blogs/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=========== 博客图片上传开始 ===========");
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize() + " 字节");
            System.out.println("文件类型: " + file.getContentType());
            System.out.println("配置的上传目录: " + uploadDir);
            
            // 检查上传文件是否为空
            if (file.isEmpty()) {
                System.err.println("上传失败：文件为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("上传失败：文件为空");
            }
            
            // 1. 创建上传目录并检查权限
            File uploadDirFile = new File(uploadDir);
            testDirectoryPermissions(uploadDirFile);
            
            if (!uploadDirFile.exists()) {
                boolean created = uploadDirFile.mkdirs();
                System.out.println("创建上传目录结果: " + created);
                if (!created) {
                    System.err.println("无法创建上传目录: " + uploadDirFile.getAbsolutePath());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("无法创建上传目录");
                }
            }
            
            File blogsDir = new File(uploadDirFile, "blogs");
            testDirectoryPermissions(blogsDir);
            
            if (!blogsDir.exists()) {
                boolean created = blogsDir.mkdirs();
                System.out.println("创建blogs目录结果: " + created);
                if (!created) {
                    System.err.println("无法创建blogs目录: " + blogsDir.getAbsolutePath());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("无法创建blogs目录");
                }
            }
            
            // 进行文件写入测试
            testDirectoryWriteAccess(blogsDir);
            
            // 2. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            
            // 3. 保存文件
            File destFile = new File(blogsDir, newFileName);
            System.out.println("目标文件路径: " + destFile.getAbsolutePath());
            
            try {
                // 先尝试创建目标文件，检查是否可写
                if (!destFile.createNewFile()) {
                    System.out.println("目标文件已存在，将被覆盖");
                }
                
                try (InputStream inputStream = file.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(destFile)) {
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;
                    int updateInterval = 1024 * 1024; // 每读取1MB打印一次进度
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        if (totalBytesRead % updateInterval < 1024) {
                            System.out.println("已写入: " + (totalBytesRead / 1024 / 1024) + "MB");
                        }
                    }
                    outputStream.flush();
                    
                    System.out.println("文件保存成功: " + destFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("文件流操作失败:");
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("文件保存失败: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("无法创建目标文件:");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("无法创建目标文件: " + e.getMessage());
            }
            
            // 4. 检查文件是否成功保存
            if (!destFile.exists() || destFile.length() == 0) {
                System.err.println("文件未能成功保存或为空文件");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件保存失败: 文件未能成功写入或为空文件");
            }
            
            // 5. 生成URL
            String fileUrl = "/uploads/blogs/" + newFileName;
            System.out.println("生成的访问URL: " + fileUrl);
            System.out.println("=========== 博客图片上传结束 ===========");
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("=========== 博客图片上传异常 ===========");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常信息: " + e.getMessage());
            e.printStackTrace();
            
            // 获取完整的堆栈跟踪
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();
            System.err.println("完整堆栈: " + stackTrace);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "文件上传失败: " + e.getMessage());
            errorResponse.put("stackTrace", stackTrace);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/avatar/upload")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            System.out.println("=========== 头像上传开始 ===========");
            System.out.println("认证头: " + (authHeader != null ? "存在" : "不存在"));
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize() + " 字节");
            System.out.println("文件类型: " + file.getContentType());
            System.out.println("配置的上传目录: " + uploadDir);
            
            // 检查上传文件是否为空
            if (file.isEmpty()) {
                System.err.println("上传失败：文件为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("上传失败：文件为空");
            }
            
            // 1. 创建上传目录并检查权限
            File uploadDirFile = new File(uploadDir);
            testDirectoryPermissions(uploadDirFile);
            
            if (!uploadDirFile.exists()) {
                boolean created = uploadDirFile.mkdirs();
                System.out.println("创建上传目录结果: " + created);
                if (!created) {
                    System.err.println("无法创建上传目录: " + uploadDirFile.getAbsolutePath());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("无法创建上传目录");
                }
            }
            
            File avatarDir = new File(uploadDirFile, "avatar");
            testDirectoryPermissions(avatarDir);
            
            if (!avatarDir.exists()) {
                boolean created = avatarDir.mkdirs();
                System.out.println("创建avatar目录结果: " + created);
                if (!created) {
                    System.err.println("无法创建avatar目录: " + avatarDir.getAbsolutePath());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("无法创建avatar目录");
                }
            }
            
            // 进行文件写入测试
            testDirectoryWriteAccess(avatarDir);
            
            // 2. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            
            // 3. 保存文件
            File destFile = new File(avatarDir, newFileName);
            System.out.println("目标文件路径: " + destFile.getAbsolutePath());
            
            try {
                // 先尝试创建目标文件，检查是否可写
                if (!destFile.createNewFile()) {
                    System.out.println("目标文件已存在，将被覆盖");
                }
                
                try (InputStream inputStream = file.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(destFile)) {
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;
                    int updateInterval = 1024 * 1024; // 每读取1MB打印一次进度
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        if (totalBytesRead % updateInterval < 1024) {
                            System.out.println("已写入: " + (totalBytesRead / 1024 / 1024) + "MB");
                        }
                    }
                    outputStream.flush();
                    
                    System.out.println("文件保存成功: " + destFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("文件流操作失败:");
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("文件保存失败: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("无法创建目标文件:");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("无法创建目标文件: " + e.getMessage());
            }
            
            // 4. 检查文件是否成功保存
            if (!destFile.exists() || destFile.length() == 0) {
                System.err.println("文件未能成功保存或为空文件");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("文件保存失败: 文件未能成功写入或为空文件");
            }
            
            // 5. 生成URL
            String fileUrl = "/uploads/avatar/" + newFileName;
            System.out.println("生成的访问URL: " + fileUrl);
            System.out.println("=========== 头像上传结束 ===========");
            
            Map<String, Object> response = new HashMap<>();
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("=========== 头像上传异常 ===========");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常信息: " + e.getMessage());
            e.printStackTrace();
            
            // 获取完整的堆栈跟踪
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();
            System.err.println("完整堆栈: " + stackTrace);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "头像上传失败: " + e.getMessage());
            errorResponse.put("stackTrace", stackTrace);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 测试目录权限
     */
    private void testDirectoryPermissions(File directory) {
        System.out.println("----------测试目录权限: " + directory.getAbsolutePath() + "----------");
        
        // 检查目录是否存在
        if (directory.exists()) {
            System.out.println("目录存在");
            
            // 检查是否是目录
            if (directory.isDirectory()) {
                System.out.println("是目录");
            } else {
                System.err.println("不是目录！这是一个文件！");
            }
            
            // 检查读权限
            if (directory.canRead()) {
                System.out.println("可读");
            } else {
                System.err.println("不可读！");
            }
            
            // 检查写权限
            if (directory.canWrite()) {
                System.out.println("可写");
            } else {
                System.err.println("不可写！");
            }
            
            // 检查执行权限
            if (directory.canExecute()) {
                System.out.println("可执行");
            } else {
                System.err.println("不可执行！");
            }
            
            // 检查父目录
            File parent = directory.getParentFile();
            if (parent != null) {
                System.out.println("父目录: " + parent.getAbsolutePath());
                if (parent.canWrite()) {
                    System.out.println("父目录可写");
                } else {
                    System.err.println("父目录不可写！");
                }
            }
        } else {
            System.out.println("目录不存在");
            
            // 检查是否可以创建
            File parent = directory.getParentFile();
            if (parent != null && parent.exists()) {
                if (parent.canWrite()) {
                    System.out.println("父目录可写，可创建目录");
                } else {
                    System.err.println("父目录不可写，无法创建目录！");
                }
            } else {
                System.err.println("父目录不存在，无法创建目录！");
            }
        }
        
        System.out.println("----------目录权限测试结束----------");
    }
    
    /**
     * 测试目录写入权限
     */
    private void testDirectoryWriteAccess(File directory) {
        System.out.println("----------测试目录写入权限: " + directory.getAbsolutePath() + "----------");
        
        if (!directory.exists()) {
            System.err.println("目录不存在，无法测试写入");
            return;
        }
        
        try {
            // 尝试创建一个临时文件
            File tempFile = new File(directory, "test_write_" + System.currentTimeMillis() + ".tmp");
            boolean created = tempFile.createNewFile();
            System.out.println("创建临时测试文件结果: " + created);
            
            if (created) {
                // 尝试写入一些数据
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write("测试写入".getBytes());
                    System.out.println("写入测试数据成功");
                } catch (IOException e) {
                    System.err.println("写入测试数据失败: " + e.getMessage());
                }
                
                // 删除临时文件
                boolean deleted = tempFile.delete();
                System.out.println("删除临时测试文件结果: " + deleted);
            }
        } catch (IOException e) {
            System.err.println("目录写入测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("----------目录写入测试结束----------");
    }
}