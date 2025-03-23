package com.example.blog.service;

import com.example.blog.model.User;
import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.RegisterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface UserService {
    User registerUser(RegisterRequest registerRequest);
    String login(LoginRequest loginRequest);
    Map<String, Object> loginAndGetUserInfo(LoginRequest loginRequest);
    User findByUsername(String username);
    User findByToken(String token);
    Optional<User> findById(Long id);
    Page<User> findAll(Pageable pageable);
    User updateUser(Long id, User userDetails);
    User save(User user);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
} 