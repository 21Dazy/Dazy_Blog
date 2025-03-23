package com.example.blog.service;

import com.example.blog.model.Category;
import com.example.blog.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    public List<Category> findAll() {
        try {
            return categoryRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取分类列表失败: " + e.getMessage());
        }
    }

    public Optional<Category> findById(Long id) {
        try {
            return categoryRepository.findById(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取分类详情失败: " + e.getMessage());
        }
    }
    
    public Category updateCategory(Category category) {
        try {
            return categoryRepository.save(category);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("更新分类失败: " + e.getMessage());
        }
    }
    
    public void deleteCategory(Long id) {
        try {
            categoryRepository.deleteById(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("删除分类失败: " + e.getMessage());
        }
    }
} 