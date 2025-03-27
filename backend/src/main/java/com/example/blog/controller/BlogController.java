package com.example.blog.controller;

import com.example.blog.dto.BlogRequest;
import com.example.blog.model.Blog;
import com.example.blog.model.User;
import com.example.blog.service.BlogService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class BlogController {

    private final BlogService blogService;
    private final UserService userService;
    
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    public BlogController(BlogService blogService, UserService userService) {
        this.blogService = blogService;
        this.userService = userService;
    }
    
    // 从token中提取用户ID
    private Long getUserIdFromToken(String token) {
        System.out.println("接收到的令牌：" + (token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : "null"));
        
        if (token != null && token.startsWith("Bearer ")) {
            // 移除"Bearer "前缀
            String actualToken = token.substring(7);
            
            try {
                // 使用token查找用户
                User user = userService.findByToken(actualToken);
                if (user != null) {
                    System.out.println("找到用户ID: " + user.getId() + ", 用户名: " + user.getUsername());
                    return user.getId();
                } else {
                    System.err.println("未找到与令牌关联的用户");
                }
            } catch (Exception e) {
                System.err.println("查找用户时出错: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("令牌格式不正确或为空");
        }
        
        // 默认返回ID为1的用户（仅用于开发测试）
        System.out.println("使用默认用户ID: 1");
        return 1L;
    }

    @PostMapping("/auth/blogs")
    public ResponseEntity<?> createBlog(@Valid @RequestBody BlogRequest blogRequest,//valid注释表示参数校验，若参数不符合要求，则会返回400错误
                                       @RequestHeader("Authorization") String token) {
        try {
            // 记录请求内容以便调试
            System.out.println("接收到创建博客请求: " + blogRequest.getTitle());
            System.out.println("博客内容长度: " + (blogRequest.getContent() != null ? blogRequest.getContent().length() : 0));
            // 打印内容的前100个字符，用于检查中文是否正确接收
            if (blogRequest.getContent() != null && blogRequest.getContent().length() > 0) {
                String contentPreview = blogRequest.getContent().substring(0, Math.min(100, blogRequest.getContent().length()));
                System.out.println("内容预览: " + contentPreview);
                // 检查是否包含中文字符
                boolean containsChinese = contentPreview.matches(".*[\\u4e00-\\u9fa5].*");
                System.out.println("是否包含中文: " + containsChinese);
            }
            
            // 从token中获取用户ID
            Long userId = getUserIdFromToken(token);
            Blog blog = blogService.createBlog(blogRequest, userId);
            
            // 转换为BlogResponse
            com.example.blog.dto.BlogResponse blogResponse = new com.example.blog.dto.BlogResponse(blog);
            return ResponseEntity.status(HttpStatus.CREATED).body(blogResponse);
        } catch (Exception e) {
            // 记录详细异常信息
            e.printStackTrace();
            System.err.println("创建博客失败: " + e.getMessage());
            // 如果是数据库异常，可能与字符编码有关
            if (e.getCause() != null) {
                System.err.println("异常原因: " + e.getCause().getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("创建博客失败: " + e.getMessage());
        }
    }

    @PutMapping("/auth/blogs/{id}")
    public ResponseEntity<?> updateBlog(@PathVariable Long id,
                                       @Valid @RequestBody BlogRequest blogRequest,
                                       @RequestHeader("Authorization") String token) {
        try {
            // 记录请求内容以便调试
            System.out.println("接收到更新博客请求: " + blogRequest.getTitle() + ", ID: " + id);
            System.out.println("博客内容长度: " + (blogRequest.getContent() != null ? blogRequest.getContent().length() : 0));
            // 打印内容的前100个字符，用于检查中文是否正确接收
            if (blogRequest.getContent() != null && blogRequest.getContent().length() > 0) {
                String contentPreview = blogRequest.getContent().substring(0, Math.min(100, blogRequest.getContent().length()));
                System.out.println("内容预览: " + contentPreview);
                // 检查是否包含中文字符
                boolean containsChinese = contentPreview.matches(".*[\\u4e00-\\u9fa5].*");
                System.out.println("是否包含中文: " + containsChinese);
            }
            
            // 从token中获取用户ID
            Long userId = getUserIdFromToken(token);
            Blog blog = blogService.updateBlog(id, blogRequest, userId);
            
            // 转换为BlogResponse
            com.example.blog.dto.BlogResponse blogResponse = new com.example.blog.dto.BlogResponse(blog);
            return ResponseEntity.ok(blogResponse);
        } catch (Exception e) {
            // 记录详细异常信息
            e.printStackTrace();
            System.err.println("更新博客失败: " + e.getMessage());
            // 如果是数据库异常，可能与字符编码有关
            if (e.getCause() != null) {
                System.err.println("异常原因: " + e.getCause().getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新博客失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/auth/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) {
        try {
            // 从token中获取用户ID
            Long userId = getUserIdFromToken(token);
            blogService.deleteBlog(id, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/auth/blogs/{id}")
    public ResponseEntity<?> getBlogByIdAuth(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            Optional<Blog> blogOptional = blogService.findById(id);
            if (blogOptional.isPresent()) {
                Blog blog = blogOptional.get();
                blogService.incrementViews(id);
                // 转换为BlogResponse
                com.example.blog.dto.BlogResponse blogResponse = new com.example.blog.dto.BlogResponse(blog);
                return ResponseEntity.ok(blogResponse);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("博客不存在");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/blogs/{id}")
    public ResponseEntity<?> getBlogById(@PathVariable Long id) {
        try {
            Optional<Blog> blogOptional = blogService.findById(id);
            if (blogOptional.isPresent()) {
                Blog blog = blogOptional.get();
                blogService.incrementViews(id);
                // 转换为BlogResponse
                com.example.blog.dto.BlogResponse blogResponse = new com.example.blog.dto.BlogResponse(blog);
                return ResponseEntity.ok(blogResponse);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("博客不存在");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/blogs")
    public ResponseEntity<?> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            System.out.println("获取所有博客开始: page=" + page + ", size=" + size);
            if (startDate != null) {
                System.out.println("开始日期: " + startDate);
            }
            if (endDate != null) {
                System.out.println("结束日期: " + endDate);
            }
            
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            
            Page<Blog> blogs;
            if (startDate != null && endDate != null) {
                // 使用日期范围过滤
                blogs = blogService.findByDateRange(startDate, endDate, pageable);
            } else {
                // 获取所有博客
                blogs = blogService.findAll(pageable);
            }
            
            System.out.println("查询到博客数量: " + blogs.getContent().size());
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = new ArrayList<>();
            for (Blog blog : blogs.getContent()) {
                try {
                    com.example.blog.dto.BlogResponse response = new com.example.blog.dto.BlogResponse(blog);
                    blogResponses.add(response);
                    System.out.println("转换博客成功: ID=" + blog.getId() + ", 标题=" + blog.getTitle());
                } catch (Exception e) {
                    System.err.println("转换博客失败: ID=" + blog.getId() + ", 错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("转换完成，准备返回" + blogResponses.size() + "个博客");
            
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("获取所有博客失败: " + e.getMessage());
            Throwable rootCause = getRootCause(e);
            System.err.println("根本原因: " + rootCause.getClass().getName() + ": " + rootCause.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取博客列表失败: " + e.getMessage() + ", 根本原因: " + rootCause.getMessage());
        }
    }

    // 获取异常的根本原因
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null) {
            return throwable;
        }
        return getRootCause(cause);
    }

    @GetMapping("/auth/author/{authorId}")
    public ResponseEntity<?> getBlogsByAuthorId(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Blog> blogs = blogService.findByAuthorId(authorId, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getBlogsByCategoryId(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Blog> blogs = blogService.findByCategoryId(categoryId, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/tag/{tagId}")
    public ResponseEntity<?> getBlogsByTagId(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            Page<Blog> blogs = blogService.findByTagId(tagId, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchBlogs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            Page<Blog> blogs = blogService.searchByKeyword(query, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/blogs/{id}/likes")
    public ResponseEntity<?> getBlogLikes(@PathVariable Long id) {
        try {
            long likes = blogService.getLikes(id);
            return ResponseEntity.ok(likes);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    @PostMapping("/blogs/{id}/like")
    public ResponseEntity<?> likeBlog(@PathVariable Long id, @RequestParam(required = false) String username ,@RequestParam(defaultValue = "true") Boolean isLiked) {
        try {
            // 目前博客服务中的likeBlog方法不需要userId参数
            if(isLiked){
                blogService.likeBlog(id);
            }else{
                blogService.unlikeBlog(id);
            }
            long likes = blogService.getLikes(id);
            return ResponseEntity.ok(likes);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    @GetMapping("/auth/blogs/user/{userId}")
    public ResponseEntity<?> getBlogsByUserIdAuth(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestHeader("Authorization") String token) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            Page<Blog> blogs = blogService.findByAuthorId(userId, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
                    
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/blogs/user/{userId}")
    public ResponseEntity<?> getBlogsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            Page<Blog> blogs = blogService.findPublicByAuthorId(userId, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
                    
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/auth/blogs/category/{categoryId}")
    public ResponseEntity<?> getBlogsByCategoryIdAuth(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestHeader("Authorization") String token) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            Page<Blog> blogs = blogService.findByCategoryId(categoryId, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
                    
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/blogs/category/{categoryId}")
    public ResponseEntity<?> getBlogsByCategoryIdPublic(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            Page<Blog> blogs = blogService.findPublicByCategoryId(categoryId, pageable);
            
            // 转换为BlogResponse对象
            List<com.example.blog.dto.BlogResponse> blogResponses = blogs.getContent().stream()
                    .map(com.example.blog.dto.BlogResponse::new)
                    .collect(Collectors.toList());
                    
            Map<String, Object> response = new HashMap<>();
            response.put("blogs", blogResponses);
            response.put("currentPage", blogs.getNumber());
            response.put("totalItems", blogs.getTotalElements());
            response.put("totalPages", blogs.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/auth/author/{authorId}/stats")
    public ResponseEntity<?> getUserBlogStats(@PathVariable Long authorId) {
        try {
            // 查找该作者的所有博客
            User author = userService.findById(authorId)
                .orElseThrow(() -> new RuntimeException("作者不存在"));
            
            // 获取作者的所有博客
            List<Blog> blogs = blogService.findByAuthorWithCategory(author);
            
            // 计算总阅读量
            int totalViews = blogs.stream()
                .mapToInt(Blog::getViews)
                .sum();
                
            // 计算总点赞数
            int totalLikes = blogs.stream()
                .mapToInt(Blog::getLikes)
                .sum();
                
            // 构建统计数据响应
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBlogs", blogs.size());
            stats.put("totalViews", totalViews);
            stats.put("totalLikes", totalLikes);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("获取用户博客统计失败: " + e.getMessage());
        }
    }
} 