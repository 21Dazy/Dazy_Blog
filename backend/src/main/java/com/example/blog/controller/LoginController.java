package com.example.blog.controller;

import com.example.blog.dto.LoginRequest;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:8082", "http://127.0.0.1:8082"}, maxAge = 3600, allowCredentials = "true")
@RestController
public class LoginController {
    
    private final UserService userService;
    
    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("收到登录请求: 用户名=" + loginRequest.getUsername());
            Map<String, Object> response = userService.loginAndGetUserInfo(loginRequest);
            System.out.println("登录成功, 返回token和用户信息");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("登录失败: " + e.getMessage());
            e.printStackTrace();
            // 获取根本原因
            Throwable rootCause = getRootCause(e);
            System.err.println("根本原因: " + rootCause.getClass().getName() + ": " + rootCause.getMessage());
            
            // 根据错误类型返回不同的状态码
            if (e.getMessage().contains("用户名或密码错误")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("用户名或密码错误");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("登录过程中发生错误: " + e.getMessage());
            }
        }
    }
    
    // 获取异常的根本原因
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null) {
            return throwable;
        }
        return getRootCause(cause);
    }
}