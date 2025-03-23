package com.example.blog.service;


public interface TokenService {
    boolean validToken(String token,String id);
    Long getIdFromToken(String token);
} 