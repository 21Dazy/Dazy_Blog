package com.example.blog.service.impl;

import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.RegisterRequest;
import com.example.blog.model.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        // 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setName(registerRequest.getName());
        
        // 生成一个简单的令牌
        user.setToken(UUID.randomUUID().toString());
        
        return userRepository.save(user);
    }

    @Override
    public String login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 刷新令牌
        String token = UUID.randomUUID().toString();
        user.setToken(token);
        userRepository.save(user);
        
        return token;
    }
    
    @Override
    public Map<String, Object> loginAndGetUserInfo(LoginRequest loginRequest) {
        try {
            System.out.println("开始登录: 用户名=" + loginRequest.getUsername());
            
            // 查找用户
            User user = null;
            try {
                user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
                System.out.println("用户存在: ID=" + user.getId());
            } catch (Exception e) {
                System.err.println("查找用户失败: " + e.getMessage());
                throw new RuntimeException("用户名或密码错误");
            }

            // 验证密码
            try {
                if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                    System.err.println("密码验证失败");
                    throw new RuntimeException("用户名或密码错误");
                }
                System.out.println("密码验证成功");
            } catch (Exception e) {
                System.err.println("密码验证出错: " + e.getMessage());
                throw new RuntimeException("用户名或密码错误");
            }

            // 刷新令牌
            String token = UUID.randomUUID().toString();
            user.setToken(token);
            try {
                userRepository.save(user);
                System.out.println("令牌已更新: " + token.substring(0, 8) + "...");
            } catch (Exception e) {
                System.err.println("保存用户令牌失败: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("登录过程中出现错误");
            }
            
            // 创建响应对象
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            
            // 创建用户信息对象（不包含敏感信息如密码）
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("name", user.getName());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("bio", user.getBio());
            
            response.put("user", userInfo);
            
            System.out.println("登录成功: 用户ID=" + user.getId());
            return response;
        } catch (Exception e) {
            System.err.println("登录过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    @Override
    public User findByToken(String token) {
        return userRepository.findByToken(token)
                .orElse(null);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        // 如果密码不为空，更新密码
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        user.setBio(userDetails.getBio());
        user.setAvatar(userDetails.getAvatar());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
} 