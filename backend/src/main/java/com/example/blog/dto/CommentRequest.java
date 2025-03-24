package com.example.blog.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CommentRequest {
    
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 500, message = "评论内容必须在1-500字符之间")
    private String content;
    
    // 以下字段用于匿名评论
    private String username;
    private String userAvatar;
    
    // 以下字段用于回复某个评论
    private Long parentCommentId;

    // 构造函数
    public CommentRequest() {
    }
    
    public CommentRequest(String content, String username, String userAvatar, Long parentCommentId) {
        this.content = content;
        this.username = username;
        this.userAvatar = userAvatar;
        this.parentCommentId = parentCommentId;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUserAvatar() {
        return userAvatar;
    }
    
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
    
    public Long getParentCommentId() {
        return parentCommentId;
    }
    
    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
} 