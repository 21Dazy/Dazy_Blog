package com.example.blog.repository;

import com.example.blog.model.Blog;
import com.example.blog.model.Comment;
import com.example.blog.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBlog(Blog blog);
    
    Page<Comment> findByBlog(Blog blog, Pageable pageable);
    
    List<Comment> findByUser(User user);
    
    Page<Comment> findByUser(User user, Pageable pageable);
    
    List<Comment> findByParentId(Long parentId);
    
    Page<Comment> findByBlogAndParentIsNull(Blog blog, Pageable pageable);
} 