package com.example.blog.service;

import com.example.blog.dto.BlogRequest;
import com.example.blog.dto.BlogResponse;
import com.example.blog.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BlogService {
    Blog createBlog(BlogRequest blogRequest, Long userId);
    Blog updateBlog(Long id, BlogRequest blogRequest, Long userId);
    void deleteBlog(Long id, Long userId);
    Optional<Blog> findById(Long id);
    Page<Blog> findAll(Pageable pageable);
    Page<Blog> findByAuthorId(Long authorId, Pageable pageable);
    Page<Blog> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Blog> findByTagId(Long tagId, Pageable pageable);
    Page<Blog> searchByKeyword(String keyword, Pageable pageable);
    Page<Blog> findPublicByAuthorId(Long authorId, Pageable pageable);
    Page<Blog> findPublicByCategoryId(Long categoryId, Pageable pageable);
    void incrementViews(Long id);
    void likeBlog(Long id);
    void unlikeBlog(Long id);
    long getLikes(Long id);
} 