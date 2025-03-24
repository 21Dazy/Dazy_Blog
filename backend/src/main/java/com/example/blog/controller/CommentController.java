package com.example.blog.controller;

import com.example.blog.model.Comment;
import com.example.blog.model.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;
    
    @Value("${jwt.secret}")
    private String jwtSecret;

    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }
    
    // 从token中提取用户ID
    private Long getUserIdFromToken(String token) {
        System.out.println("评论控制器接收到的令牌：" + (token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : "null"));
        
        if (token != null && token.startsWith("Bearer ")) {
            // 移除"Bearer "前缀
            String actualToken = token.substring(7);
            
            try {
                // 使用token查找用户
                User user = userService.findByToken(actualToken);
                if (user != null) {
                    System.out.println("评论控制器找到用户ID: " + user.getId() + ", 用户名: " + user.getUsername());
                    return user.getId();
                } else {
                    System.err.println("评论控制器未找到与令牌关联的用户");
                }
            } catch (Exception e) {
                System.err.println("评论控制器查找用户时出错: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("评论控制器令牌格式不正确或为空");
        }
        
        // 默认返回ID为1的用户（仅用于开发测试）
        System.out.println("评论控制器使用默认用户ID: 1");
        return 1L;
    }

    @PostMapping("/blog/{blogId}")
    public ResponseEntity<?> createComment(
            @PathVariable Long blogId,
            @RequestParam String content,
            @RequestHeader("Authorization") String token) {
        try {
            // 从token中获取用户ID
            Long userId = getUserIdFromToken(token);
            Comment comment = commentService.createComment(blogId, userId, content);
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long id,
            @RequestParam String content,
            @RequestHeader("Authorization") String token) {
        try {
            // 从token中获取用户ID
            Long userId = getUserIdFromToken(token);
            Comment comment = commentService.updateComment(id, userId, content);
            return ResponseEntity.ok(comment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            // 从token中获取用户ID
            Long userId = getUserIdFromToken(token);
            commentService.deleteComment(id, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/blog/{blogId}")
    public ResponseEntity<?> getCommentsByBlogId(
            @PathVariable Long blogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Comment> comments = commentService.findByBlogId(blogId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comments", comments.getContent());
            response.put("currentPage", comments.getNumber());
            response.put("totalItems", comments.getTotalElements());
            response.put("totalPages", comments.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
} 