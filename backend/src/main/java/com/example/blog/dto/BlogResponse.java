package com.example.blog.dto;

import com.example.blog.model.Blog;
import com.example.blog.model.Comment;
import com.example.blog.model.Tag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class BlogResponse {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private Blog.BlogStatus status;
    private Integer views;
    private Integer likes;
    private Map<String, Object> author;
    private Map<String, Object> category;
    private Set<Map<String, Object>> tags;
    private Set<Map<String, Object>> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BlogResponse(Blog blog) {
        try {
            System.out.println("开始构建博客响应: ID=" + blog.getId() + ", 标题=" + blog.getTitle());
            this.id = blog.getId();
            this.title = blog.getTitle();
            this.summary = blog.getSummary();
            this.content = blog.getContent();
            this.coverImage = blog.getCoverImage();
            this.status = blog.getStatus();
            this.views = blog.getViews();
            this.likes = blog.getLikes();
            this.createdAt = blog.getCreatedAt();
            this.updatedAt = blog.getUpdatedAt();

            // 处理作者信息
            try {
                if (blog.getAuthor() != null) {
                    System.out.println("处理作者信息: ID=" + blog.getAuthor().getId());
                    this.author = new HashMap<>();
                    this.author.put("id", blog.getAuthor().getId());
                    this.author.put("username", blog.getAuthor().getUsername());
                    this.author.put("name", blog.getAuthor().getName());
                    this.author.put("avatar", blog.getAuthor().getAvatar());
                    System.out.println("作者信息处理完成");
                }
            } catch (Exception e) {
                System.err.println("处理作者信息出错: " + e.getMessage());
                e.printStackTrace();
                // 提供默认值
                this.author = new HashMap<>();
                this.author.put("id", 0L);
                this.author.put("username", "未知");
                this.author.put("name", "未知");
                this.author.put("avatar", "");
            }

            // 处理分类信息
            try {
                if (blog.getCategory() != null) {
                    System.out.println("处理分类信息: ID=" + blog.getCategory().getId());
                    this.category = new HashMap<>();
                    this.category.put("id", blog.getCategory().getId());
                    this.category.put("name", blog.getCategory().getName());
                    this.category.put("description", blog.getCategory().getDescription());
                    System.out.println("分类信息处理完成");
                }
            } catch (Exception e) {
                System.err.println("处理分类信息出错: " + e.getMessage());
                e.printStackTrace();
                // 提供默认值
                this.category = new HashMap<>();
                this.category.put("id", 0L);
                this.category.put("name", "未分类");
                this.category.put("description", "");
            }

            // 处理标签信息
            try {
                if (blog.getTags() != null && !blog.getTags().isEmpty()) {
                    System.out.println("处理标签信息: 数量=" + blog.getTags().size());
                    this.tags = blog.getTags().stream()
                            .map(tag -> {
                                Map<String, Object> tagMap = new HashMap<>();
                                tagMap.put("id", tag.getId());
                                tagMap.put("name", tag.getName());
                                return tagMap;
                            })
                            .collect(Collectors.toSet());
                    System.out.println("标签信息处理完成");
                }
            } catch (Exception e) {
                System.err.println("处理标签信息出错: " + e.getMessage());
                e.printStackTrace();
                // 提供空集合
                this.tags = new HashSet<>();
            }

            // 处理评论信息
            try {
                if (blog.getComments() != null && !blog.getComments().isEmpty()) {
                    System.out.println("处理评论信息: 数量=" + blog.getComments().size());
                    this.comments = blog.getComments().stream()
                            .map(comment -> {
                                Map<String, Object> commentMap = new HashMap<>();
                                commentMap.put("id", comment.getId());
                                commentMap.put("content", comment.getContent());
                                commentMap.put("createdAt", comment.getCreatedAt());
                                
                                if (comment.getUser() != null) {
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("id", comment.getUser().getId());
                                    userMap.put("username", comment.getUser().getUsername());
                                    userMap.put("name", comment.getUser().getName());
                                    userMap.put("avatar", comment.getUser().getAvatar());
                                    commentMap.put("user", userMap);
                                }
                                
                                return commentMap;
                            })
                            .collect(Collectors.toSet());
                    System.out.println("评论信息处理完成");
                }
            } catch (Exception e) {
                System.err.println("处理评论信息出错: " + e.getMessage());
                e.printStackTrace();
                // 提供空集合
                this.comments = new HashSet<>();
            }
            
            System.out.println("博客响应构建完成: ID=" + this.id);
        } catch (Exception e) {
            System.err.println("构建博客响应时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让上层处理
        }
    }
} 