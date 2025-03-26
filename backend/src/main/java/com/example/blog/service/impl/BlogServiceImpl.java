package com.example.blog.service.impl;

import com.example.blog.dto.BlogRequest;
import com.example.blog.model.Blog;
import com.example.blog.model.Category;
import com.example.blog.model.Tag;
import com.example.blog.model.User;
import com.example.blog.repository.BlogRepository;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.TagRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository, UserRepository userRepository,
                           CategoryRepository categoryRepository, TagRepository tagRepository) {
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public Blog createBlog(BlogRequest blogRequest, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Category category = categoryRepository.findById(blogRequest.getCategoryId())
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        Blog blog = new Blog();
        blog.setTitle(blogRequest.getTitle());
        blog.setSummary(blogRequest.getSummary());
        blog.setContent(blogRequest.getContent());
        blog.setCoverImage(blogRequest.getCoverImage());
        blog.setAuthor(author);
        blog.setCategory(category);
        blog.setStatus(blogRequest.getStatus());

        // 处理标签
        if (blogRequest.getTags() != null && !blogRequest.getTags().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (BlogRequest.TagDto tagDto : blogRequest.getTags()) {
                Tag tag;
                if (tagDto.getId() != null) {
                    // 使用已有标签
                    tag = tagRepository.findById(tagDto.getId())
                            .orElseThrow(() -> new RuntimeException("标签不存在: " + tagDto.getId()));
                } else if (tagDto.getName() != null && !tagDto.getName().trim().isEmpty()) {
                    // 根据名称查找标签，不存在则创建
                    String tagName = tagDto.getName().trim();
                    Optional<Tag> existingTag = tagRepository.findByName(tagName);
                    if (existingTag.isPresent()) {
                        tag = existingTag.get();
                    } else {
                        tag = new Tag();
                        tag.setName(tagName);
                        tag = tagRepository.save(tag);
                    }
                } else {
                    continue; // 跳过无效标签
                }
                tags.add(tag);
            }
            blog.setTags(tags);
        }

        return blogRepository.save(blog);
    }

    @Override
    @Transactional
    public Blog updateBlog(Long id, BlogRequest blogRequest, Long userId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
        
        // 确认是作者本人在修改
        if (!blog.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("没有权限修改此博客");
        }
        
        // 更新分类
        if (blogRequest.getCategoryId() != null) {
            Category category = categoryRepository.findById(blogRequest.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("分类不存在"));
            blog.setCategory(category);
        }

        // 更新基本信息
        blog.setTitle(blogRequest.getTitle());
        blog.setSummary(blogRequest.getSummary());
        blog.setContent(blogRequest.getContent());
        if (blogRequest.getCoverImage() != null) {
            blog.setCoverImage(blogRequest.getCoverImage());
        }
        if (blogRequest.getStatus() != null) {
            blog.setStatus(blogRequest.getStatus());
        }

        // 更新标签
        if (blogRequest.getTags() != null) {
            // 清除旧标签关联
            blog.getTags().clear();
            
            for (BlogRequest.TagDto tagDto : blogRequest.getTags()) {
                Tag tag;
                if (tagDto.getId() != null) {
                    // 使用已有标签
                    tag = tagRepository.findById(tagDto.getId())
                            .orElseThrow(() -> new RuntimeException("标签不存在: " + tagDto.getId()));
                } else if (tagDto.getName() != null && !tagDto.getName().trim().isEmpty()) {
                    // 根据名称查找标签，不存在则创建
                    String tagName = tagDto.getName().trim();
                    Optional<Tag> existingTag = tagRepository.findByName(tagName);
                    if (existingTag.isPresent()) {
                        tag = existingTag.get();
                    } else {
                        tag = new Tag();
                        tag.setName(tagName);
                        tag = tagRepository.save(tag);
                    }
                } else {
                    continue; // 跳过无效标签
                }
                blog.getTags().add(tag);
            }
        }
        
        return blogRepository.save(blog);
    }

    @Override
    @Transactional
    public void deleteBlog(Long id, Long userId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("博客不存在"));

        // 确认是作者本人在删除
        if (!blog.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("没有权限删除此博客");
        }

        blogRepository.delete(blog);
    }

    @Override
    public Optional<Blog> findById(Long id) {
        return blogRepository.findById(id);
    }

    @Override
    public Page<Blog> findAll(Pageable pageable) {
        return blogRepository.findAll(pageable);
    }

    @Override
    public Page<Blog> findByAuthorId(Long authorId, Pageable pageable) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return blogRepository.findByAuthor(author, pageable);
    }

    @Override
    public Page<Blog> findPublicByAuthorId(Long authorId, Pageable pageable) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 只返回已发布状态的博客
        return blogRepository.findByAuthorAndStatus(author, Blog.BlogStatus.PUBLISHED, pageable);
    }

    @Override
    public Page<Blog> findByCategoryId(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        return blogRepository.findByCategory(category, pageable);
    }

    @Override
    public Page<Blog> findPublicByCategoryId(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        // 只返回已发布状态的博客
        return blogRepository.findByCategoryAndStatus(category, Blog.BlogStatus.PUBLISHED, pageable);
    }

    @Override
    public Page<Blog> findByTagId(Long tagId, Pageable pageable) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        return blogRepository.findByTags(tag, pageable);
    }

    @Override
    public Page<Blog> searchByKeyword(String keyword, Pageable pageable) {
        return blogRepository.searchByKeyword(keyword, pageable);
    }

    @Override
    @Transactional
    public void incrementViews(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
        blog.setViews(blog.getViews() + 1);
        blogRepository.save(blog);
    }

    @Override
    @Transactional//这个注解表示这个方法是一个事务，事务是数据库中的一个概念，表示一组操作，要么全部成功，要么全部失败
    public void likeBlog(Long id) {
        System.out.println("点赞博客ID: " + id);
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
        blog.setLikes(blog.getLikes() + 1);
        blogRepository.save(blog);
    }

    @Override
    @Transactional//这个注解表示这个方法是一个事务，事务是数据库中的一个概念，表示一组操作，要么全部成功，要么全部失败
    public long getLikes(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
        return blog.getLikes();
    }

    @Override
    @Transactional
    public void unlikeBlog(Long id) {
        System.out.println("取消点赞博客ID: " + id);
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("博客不存在"));
        blog.setLikes(blog.getLikes() - 1);
        blogRepository.save(blog);
    }
    

} 