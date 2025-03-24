package com.example.blog.model;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "blogs")
public class Blog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 200)
    private String summary;

    @NotBlank
    @Column(columnDefinition = "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;

    private String coverImage;

    @Enumerated(EnumType.STRING)
    private BlogStatus status = BlogStatus.PUBLISHED;

    private int views = 0;

    private int likes = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    //@com.fasterxml.jackson.annotation.JsonBackReference(value = "category-blogs")
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "blog_tags",
            joinColumns = @JoinColumn(name = "blog_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private Set<Comment> comments = new HashSet<>();

    @CreationTimestamp
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @com.fasterxml.jackson.annotation.JsonProperty("category")
    public Map<String, Object> getCategoryInfo() {
        if (category != null) {
            Map<String, Object> categoryInfo = new HashMap<>();
            categoryInfo.put("id", category.getId());
            categoryInfo.put("name", category.getName());
            return categoryInfo;
        }
        return null;
    }

    // 构造函数
    public Blog() {
    }
    
    public Blog(Long id, String title, String summary, String content, String coverImage, 
                BlogStatus status, int views, int likes, User author, Category category) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.coverImage = coverImage;
        this.status = status;
        this.views = views;
        this.likes = likes;
        this.author = author;
        this.category = category;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getCoverImage() {
        return coverImage;
    }
    
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
    
    public BlogStatus getStatus() {
        return status;
    }
    
    public void setStatus(BlogStatus status) {
        this.status = status;
    }
    
    public int getViews() {
        return views;
    }
    
    public void setViews(int views) {
        this.views = views;
    }
    
    public int getLikes() {
        return likes;
    }
    
    public void setLikes(int likes) {
        this.likes = likes;
    }
    
    public User getAuthor() {
        return author;
    }
    
    public void setAuthor(User author) {
        this.author = author;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public Set<Tag> getTags() {
        return tags;
    }
    
    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
    
    public Set<Comment> getComments() {
        return comments;
    }
    
    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum BlogStatus {
        PUBLISHED, DRAFT
    }
} 