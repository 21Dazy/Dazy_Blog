package com.example.blog.service;

import com.example.blog.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CommentService {
    Comment createComment(Long blogId, Long userId, String content);
    Comment updateComment(Long id, Long userId, String content);
    void deleteComment(Long id, Long userId);
    Optional<Comment> findById(Long id);
    Page<Comment> findByBlogId(Long blogId, Pageable pageable);
    void likeComment(Long id);
    void unlikeComment(Long id);
    Comment createReply(Long blogId, Long userId, String content, Long parentId);
    Page<Comment> findByParentId(Long parentId, Pageable pageable);
    Page<Comment> findRootCommentsByBlogId(Long blogId, Pageable pageable);
} 