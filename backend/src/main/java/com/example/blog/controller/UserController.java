package com.example.blog.controller;

import com.example.blog.model.User;
import com.example.blog.service.UserService;
import com.example.blog.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = {"http://localhost:8082", "http://127.0.0.1:8082"}, maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            System.out.println("收到获取当前用户请求，Authorization头: " + (token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : "null"));
            
            // 从Authorization头中提取token
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // 移除"Bearer "前缀
                System.out.println("已提取token: " + token.substring(0, Math.min(token.length(), 10)) + "...");
            } else {
                System.err.println("Authorization头格式不正确，应以'Bearer '开头");
            }
            
            // 查找与该token关联的用户
            User user = userService.findByToken(token);
            
            if (user != null) {
                System.out.println("找到用户: ID=" + user.getId() + ", 用户名=" + user.getUsername());
                
                // 创建用户信息响应（不包含敏感信息）
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("name", user.getName());
                userInfo.put("avatar", user.getAvatar());
                userInfo.put("bio", user.getBio());
                
                return ResponseEntity.ok(userInfo);
            } else {
                System.err.println("找不到与令牌关联的用户");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无效的令牌");
            }
        } catch (Exception e) {
            System.err.println("验证用户身份时出错: " + e.getMessage());
            e.printStackTrace();
            Throwable rootCause = getRootCause(e);
            System.err.println("根本原因: " + rootCause.getClass().getName() + ": " + rootCause.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无法验证用户身份: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> userOptional = userService.findById(id);
            if (userOptional.isPresent()) {
                return ResponseEntity.ok(userOptional.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<User> users = userService.findAll(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users.getContent());
            response.put("currentPage", users.getNumber());
            response.put("totalItems", users.getTotalElements());
            response.put("totalPages", users.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader("Authorization") String token) {
            try {
                // 这里应该验证token是否属于要更新的用户
                token = token.substring(7); // 移除"Bearer "前缀
                User user = userService.findByToken(token);
                
                if (user == null || !user.getId().equals(id)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("无效的令牌");
                }

                // 只更新提供的字段
                if (updates.containsKey("email")) {
                    user.setEmail((String) updates.get("email"));
                }
                if (updates.containsKey("name")) {
                    user.setName((String) updates.get("name"));
                }
                if (updates.containsKey("avatar")) {
                    user.setAvatar((String) updates.get("avatar"));
                }
                if (updates.containsKey("bio")) {
                    user.setBio((String) updates.get("bio"));
                }
                
                User updatedUser = userService.save(user);
                return ResponseEntity.ok(updatedUser);
            } 
            catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            // 这里应该验证token是否属于要删除的用户或管理员
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
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