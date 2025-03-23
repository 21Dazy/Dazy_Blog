package com.example.blog.service.impl;

import com.example.blog.model.User; // 确保导入User类
import com.example.blog.repository.UserRepository;
import com.example.blog.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.logging.Logger;


@Service
public class TokenServiceImpl implements TokenService {

    private final UserRepository userRepository;
    private final Logger logger = Logger.getLogger(TokenServiceImpl.class.getName());
    public TokenServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean validToken(String token, String id) {
        return userRepository.findById(Long.parseLong(id))
                .map(user -> user.getToken().equals(token))
                .orElse(false);
    }

    @Override
    public Long getIdFromToken(String token) {
        logger.info("收到 Token: " + token); // 打印收到的 Token

        return userRepository.findByToken(token)
                .map(user -> {
                    logger.info("查询到的用户 ID: " + user.getId()); // 打印查询到的用户 ID
                    return user.getId();
                })
                .orElseGet(() -> {
                    logger.warning("未找到匹配的用户 ID");
                    return null;
                });
    }



}
