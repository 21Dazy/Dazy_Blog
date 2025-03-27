package com.example.blog.repository;

import com.example.blog.model.Blog;
import com.example.blog.model.Category;
import com.example.blog.model.Tag;
import com.example.blog.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Page<Blog> findByAuthor(User author, Pageable pageable);
    Page<Blog> findByAuthorAndStatus(User author, Blog.BlogStatus status, Pageable pageable);
    
    @Query("SELECT b FROM Blog b JOIN FETCH b.category WHERE b.author = :author")
    List<Blog> findByAuthorWithCategory(@Param("author") User author);
    
    Page<Blog> findByCategory(Category category, Pageable pageable);
    Page<Blog> findByCategoryAndStatus(Category category, Blog.BlogStatus status, Pageable pageable);
    
    Page<Blog> findByTags(Tag tag, Pageable pageable);
    
    @Query("SELECT b FROM Blog b WHERE b.title LIKE %:keyword% OR b.summary LIKE %:keyword% OR b.content LIKE %:keyword%")
    Page<Blog> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 按创建时间范围查询博客
    Page<Blog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
} 