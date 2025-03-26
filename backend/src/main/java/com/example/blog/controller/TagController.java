package com.example.blog.controller;

import com.example.blog.model.Tag;
import com.example.blog.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class TagController {

    private final TagRepository tagRepository;

    @Autowired
    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @GetMapping("/tags")
    public ResponseEntity<?> getAllTags() {
        try {
            List<Tag> tags = tagRepository.findAll();
            
            // 为每个标签添加关联的博客数量
            List<Map<String, Object>> tagsWithCount = tags.stream().map(tag -> {
                Map<String, Object> tagMap = new HashMap<>();
                tagMap.put("id", tag.getId());
                tagMap.put("name", tag.getName());
                tagMap.put("count", tag.getBlogs().size());
                tagMap.put("createdAt", tag.getCreatedAt());
                tagMap.put("updatedAt", tag.getUpdatedAt());
                return tagMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(tagsWithCount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取标签列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/tags/{id}")
    public ResponseEntity<?> getTagById(@PathVariable Long id) {
        try {
            Optional<Tag> tagOptional = tagRepository.findById(id);
            if (tagOptional.isPresent()) {
                Tag tag = tagOptional.get();
                Map<String, Object> tagMap = new HashMap<>();
                tagMap.put("id", tag.getId());
                tagMap.put("name", tag.getName());
                tagMap.put("count", tag.getBlogs().size());
                tagMap.put("createdAt", tag.getCreatedAt());
                tagMap.put("updatedAt", tag.getUpdatedAt());
                return ResponseEntity.ok(tagMap);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("标签不存在");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取标签详情失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/tags")
    public ResponseEntity<?> createTag(@RequestBody Map<String, String> tagData) {
        try {
            String tagName = tagData.get("name");
            if (tagName == null || tagName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("标签名称不能为空");
            }
            
            // 检查标签是否已存在
            if (tagRepository.existsByName(tagName)) {
                return ResponseEntity.badRequest().body("标签已存在");
            }
            
            Tag tag = new Tag();
            tag.setName(tagName);
            Tag savedTag = tagRepository.save(tag);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTag);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建标签失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/tags/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        try {
            Optional<Tag> tagOptional = tagRepository.findById(id);
            if (!tagOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("标签不存在");
            }
            
            Tag tag = tagOptional.get();
            
            // 检查标签是否关联了博客
            if (!tag.getBlogs().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("该标签已关联博客，无法删除");
            }
            
            tagRepository.delete(tag);
            return ResponseEntity.ok("标签删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除标签失败: " + e.getMessage());
        }
    }
}