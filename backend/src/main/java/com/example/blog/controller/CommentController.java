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
                    throw new RuntimeException("无效的用户令牌");
                }
            } catch (Exception e) {
                System.err.println("评论控制器查找用户时出错: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("处理用户令牌时出错: " + e.getMessage());
            }
        } else {
            System.err.println("评论控制器令牌格式不正确或为空");
            throw new RuntimeException("未提供有效的认证令牌");
        }
    }

    @PostMapping("/auth/comments/blog/{blogId}")// 创建评论要求是
    public ResponseEntity<?> createComment(
            @PathVariable Long blogId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId,
            @RequestHeader("Authorization") String token) {
        try {
            // 从token中获取用户ID
            Long userId = getUserIdFromToken(token);
            
            // 如果有父评论ID，则是回复评论
            Comment comment;
            if (parentId != null) {
                // 创建子评论（回复）
                comment = commentService.createReply(blogId, userId, content, parentId);
            } else {
                // 创建普通评论
                comment = commentService.createComment(blogId, userId, content);
            }
            
            // 确保用户信息完整
            if (comment.getUser() != null) {
                System.out.println("创建评论成功，用户ID: " + comment.getUser().getId() + 
                    ", 用户名: " + comment.getUser().getUsername());
            } else {
                System.err.println("警告：创建的评论没有关联用户信息");
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
        } catch (RuntimeException e) {
            System.err.println("创建评论时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/comments/{id}")
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

    @DeleteMapping("/comments/{id}")
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

    @GetMapping("/comments/blog/{blogId}")
    public ResponseEntity<?> getCommentsByBlogId(
            @PathVariable Long blogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Comment> comments = commentService.findByBlogId(blogId, pageable);
            
            // 确保响应中包含完整数据
            for (Comment comment : comments.getContent()) {
                if (comment.getUser() != null) {
                    System.out.println("评论用户信息: ID=" + comment.getUser().getId() + 
                                      ", 用户名=" + comment.getUser().getUsername());
                } else {
                    System.err.println("警告: 评论ID=" + comment.getId() + " 没有关联用户信息");
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", comments.getContent()); // 使用content作为评论列表的键
            response.put("currentPage", comments.getNumber());
            response.put("totalItems", comments.getTotalElements());
            response.put("totalPages", comments.getTotalPages());
            
            System.out.println("返回评论数: " + comments.getContent().size());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("获取评论时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    // 点赞评论
    @PostMapping("/comments/{id}/like")
    public ResponseEntity<?> likeComment(@PathVariable Long id) {
        try {
            commentService.likeComment(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    // 取消点赞评论
    @PostMapping("/comments/{id}/unlike")
    public ResponseEntity<?> unlikeComment(@PathVariable Long id) {
        try {
            commentService.unlikeComment(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
} 