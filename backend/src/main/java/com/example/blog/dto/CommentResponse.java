package com.example.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private UserSummary user;
    private Long blogId;
    private Long parentId;
    private LocalDateTime createdAt;

    @Data
    public static class UserSummary {
        private Long id;
        private String username;
        private String avatar;
    }
} 