package com.example.blog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class TestController {
    
    @GetMapping("/test")
    public ResponseEntity<String> testPublicEndpoint() {
        return ResponseEntity.ok("公开测试接口工作正常！");
    }
    
    @GetMapping("/auth/test")
    public ResponseEntity<String> testAuthEndpoint() {
        return ResponseEntity.ok("Auth测试接口工作正常！");
    }
} 