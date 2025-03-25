package com.example.blog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.blog.model.Category;
import com.example.blog.dto.CategoryResponse;
import com.example.blog.service.CategoryService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;

@CrossOrigin(origins = {"http://localhost:8082", "http://127.0.0.1:8082"}, maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/auth")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories(){
        try {
            System.out.println("获取所有分类开始");
            List<Category> categories = categoryService.findAll();
            System.out.println("获取所有分类成功: " + categories.size() + "个分类");
            
            // 将分类转换为CategoryResponse
            List<CategoryResponse> categoryResponses = new ArrayList<>();
            for (Category category : categories) {
                try {
                    CategoryResponse response = new CategoryResponse(category);
                    categoryResponses.add(response);
                    System.out.println("转换分类成功: ID=" + category.getId() + ", 名称=" + category.getName());
                } catch (Exception e) {
                    System.err.println("转换分类失败: ID=" + category.getId() + ", 错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
                
            System.out.println("转换完成，准备返回" + categoryResponses.size() + "个分类");
            return ResponseEntity.ok(categoryResponses);
        } catch (Exception e) {
            e.printStackTrace(); 
            System.err.println("获取所有分类失败: " + e.getMessage());
            Throwable rootCause = getRootCause(e);
            System.err.println("根本原因: " + rootCause.getClass().getName() + ": " + rootCause.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取分类失败: " + e.getMessage() + ", 根本原因: " + rootCause.getMessage());
        }
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id){
        try {
            return categoryService.findById(id)
                    .map(category -> {
                        CategoryResponse response = new CategoryResponse(category);
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取分类详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Category category){
        try {
            categoryService.createCategory(category);
            // 转换为CategoryResponse
            CategoryResponse response = new CategoryResponse(category);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建分类失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            return categoryService.findById(id)
                    .map(existingCategory -> {
                        existingCategory.setName(category.getName());
                        existingCategory.setDescription(category.getDescription());
                        categoryService.updateCategory(existingCategory);
                        // 转换为CategoryResponse
                        CategoryResponse response = new CategoryResponse(existingCategory);
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新分类失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            return categoryService.findById(id)
                    .map(category -> {
                        categoryService.deleteCategory(id);
                        return ResponseEntity.ok().build();
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除分类失败: " + e.getMessage());
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
}   
