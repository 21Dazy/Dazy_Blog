package com.example.blog.service.impl;

import com.example.blog.model.Blog;
import com.example.blog.model.Comment;
import com.example.blog.model.User;
import com.example.blog.repository.BlogRepository;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository, 
                             BlogRepository blogRepository,
                             UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Comment createComment(Long blogId, Long userId, String content) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
                
        Comment comment = new Comment();
        comment.setBlog(blog);
        comment.setUser(user);
        comment.setContent(content);
        
        Comment savedComment = commentRepository.save(comment);
        
        // 确保用户信息被完全加载
        User fullUser = savedComment.getUser();
        fullUser.getId();
        fullUser.getUsername();
        fullUser.getAvatar();
        
        return savedComment;
    }

    @Override
    @Transactional
    public Comment updateComment(Long id, Long userId, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
                
        // 确认是评论的作者在修改
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("没有权限修改此评论");
        }
        
        comment.setContent(content);
        Comment updatedComment = commentRepository.save(comment);
        
        // 确保用户信息被完全加载
        User user = updatedComment.getUser();
        user.getId();
        user.getUsername();
        user.getAvatar();
        
        return updatedComment;
    }

    @Override
    @Transactional
    public void deleteComment(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
                
        // 确认是评论的作者在删除
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("没有权限删除此评论");
        }
        
        commentRepository.delete(comment);
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    @Override
    public Page<Comment> findByBlogId(Long blogId, Pageable pageable) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
                
        Page<Comment> comments = commentRepository.findByBlog(blog, pageable);
        
        // 确保每个评论的用户信息被完全加载
        comments.getContent().forEach(comment -> {
            if (comment.getUser() != null) {
                // 触发加载用户的所有必要信息，解决懒加载问题
                User user = comment.getUser();
                user.getId();
                user.getUsername();
                user.getAvatar();
                
                // 如果评论有父评论，也加载父评论的用户信息
                if (comment.getParent() != null) {
                    User parentUser = comment.getParent().getUser();
                    if (parentUser != null) {
                        parentUser.getId();
                        parentUser.getUsername();
                        parentUser.getAvatar();
                    }
                }
            }
        });
        
        return comments;
    }

    @Override
    @Transactional
    public void likeComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 增加点赞数
        Integer currentLikes = comment.getLikes();
        if (currentLikes == null) {
            currentLikes = 0;
        }
        comment.setLikes(currentLikes + 1);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void unlikeComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 确保点赞数不为负
        Integer currentLikes = comment.getLikes();
        if (currentLikes == null || currentLikes <= 0) {
            comment.setLikes(0);
        } else {
            comment.setLikes(currentLikes - 1);
        }
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public Comment createReply(Long blogId, Long userId, String content, Long parentId) {
        // 调用有replyToUsername参数的方法，传入null
        return createReply(blogId, userId, content, parentId, null);
    }

    @Override
    @Transactional
    public Comment createReply(Long blogId, Long userId, String content, Long parentId, String replyToUsername) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
                
        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("父评论不存在"));
                
        // 确保父评论属于同一个博客
        if (!parentComment.getBlog().getId().equals(blogId)) {
            throw new RuntimeException("父评论与当前博客不匹配");
        }
        
        Comment reply = new Comment();
        reply.setBlog(blog);
        reply.setUser(user);
        reply.setContent(content);
        reply.setParent(parentComment);  // 设置父评论
        reply.setReplyToUsername(replyToUsername); // 设置回复用户名
        
        Comment savedReply = commentRepository.save(reply);
        
        // 确保用户信息被完全加载
        User fullUser = savedReply.getUser();
        fullUser.getId();
        fullUser.getUsername();
        fullUser.getAvatar();
        
        // 加载父评论用户信息
        if (savedReply.getParent() != null && savedReply.getParent().getUser() != null) {
            User parentUser = savedReply.getParent().getUser();
            parentUser.getId();
            parentUser.getUsername();
            parentUser.getAvatar();
        }
        
        return savedReply;
    }

    @Override
    public Page<Comment> findByParentId(Long parentId, Pageable pageable) {
        // 首先查找父评论，确保它存在
        Optional<Comment> parentComment = commentRepository.findById(parentId);
        if (!parentComment.isPresent()) {
            throw new RuntimeException("父评论不存在");
        }
        
        // 使用Repository的findByParentId方法，但需要转换为Page对象
        List<Comment> childComments = commentRepository.findByParentId(parentId);
        
        // 手动进行分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), childComments.size());
        
        // 确保索引有效
        if (start > childComments.size()) {
            start = 0;
            end = 0;
        }
        
        List<Comment> pageContent = (start < end) 
            ? childComments.subList(start, end) 
            : Collections.emptyList();
        
        return new PageImpl<>(pageContent, pageable, childComments.size());
    }

    @Override
    public Page<Comment> findRootCommentsByBlogId(Long blogId, Pageable pageable) {
        // 查找博客，确保它存在
        Optional<Blog> blog = blogRepository.findById(blogId);
        if (!blog.isPresent()) {
            throw new RuntimeException("博客不存在，ID: " + blogId);
        }
        
        // 这里需要创建一个Repository方法来查找没有父评论的评论
        // 我们可以先使用原生的JPQL查询实现
        Page<Comment> rootComments = commentRepository.findByBlogAndParentIsNull(blog.get(), pageable);
        
        return rootComments;
    }
} 