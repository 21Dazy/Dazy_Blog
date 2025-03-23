package com.example.blog.dto;

import com.example.blog.model.Blog;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

public class BlogRequest {
    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 200)
    private String summary;

    @NotBlank
    private String content;

    private String coverImage;

    @NotNull
    private Long categoryId;

    private Set<TagDto> tags;

    private Blog.BlogStatus status = Blog.BlogStatus.PUBLISHED;
    
    // Getters and Setters
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
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public Set<TagDto> getTags() {
        return tags;
    }
    
    public void setTags(Set<TagDto> tags) {
        this.tags = tags;
    }
    
    public Blog.BlogStatus getStatus() {
        return status;
    }
    
    public void setStatus(Blog.BlogStatus status) {
        this.status = status;
    }

    public static class TagDto {
        private Long id;
        private String name;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
} 