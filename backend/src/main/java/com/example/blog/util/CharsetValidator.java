package com.example.blog.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 字符集验证工具类
 * 用于检查和处理字符编码问题
 */
public class CharsetValidator {
    
    /**
     * 检查字符串是否是有效的UTF-8编码
     * 
     * @param input 需要检查的字符串
     * @return 如果是有效的UTF-8编码，返回true；否则返回false
     */
    public static boolean isValidUtf8(String input) {
        if (input == null) {
            return false;
        }
        
        try {
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            new String(bytes, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 将字符串转换为有效的UTF-8编码
     * 
     * @param input 需要转换的字符串
     * @return 转换后的UTF-8编码字符串
     */
    public static String toValidUtf8(String input) {
        if (input == null) {
            return "";
        }
        
        try {
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果发生错误，尝试使用替代编码
            try {
                byte[] bytes = input.getBytes(Charset.defaultCharset());
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return ""; // 如果所有尝试都失败，返回空字符串
            }
        }
    }
    
    /**
     * 过滤字符串中不可打印或不合法的字符
     * 
     * @param input 需要过滤的字符串
     * @return 过滤后的字符串
     */
    public static String filterInvalidChars(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 保留可打印字符和中文字符
            if ((c > 31 && c != 127) || (c >= '\u4e00' && c <= '\u9fa5')) {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 检查数据库连接是否正确配置为UTF-8
     * 
     * @param jdbcUrl 数据库连接URL
     * @return 如果配置正确，返回true；否则返回false
     */
    public static boolean isUtf8Configured(String jdbcUrl) {
        if (jdbcUrl == null) {
            return false;
        }
        
        return jdbcUrl.contains("characterEncoding=utf8") || 
               jdbcUrl.contains("useUnicode=true") ||
               jdbcUrl.contains("characterSetResults=utf8") ||
               jdbcUrl.contains("utf8mb4");
    }
    
    /**
     * 检查字符串是否包含中文字符
     * 
     * @param input 需要检查的字符串
     * @return 如果包含中文字符，返回true；否则返回false
     */
    public static boolean containsChineseChars(String input) {
        if (input == null) {
            return false;
        }
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fa5') {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算字符串中中文字符的数量
     * 
     * @param input 需要计算的字符串
     * @return 中文字符的数量
     */
    public static int countChineseChars(String input) {
        if (input == null) {
            return 0;
        }
        
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fa5') {
                count++;
            }
        }
        
        return count;
    }
} 