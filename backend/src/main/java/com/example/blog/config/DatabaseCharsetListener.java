package com.example.blog.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 数据库字符集检查监听器
 * 在应用启动时检查数据库和表的字符集设置
 */
@Component
public class DatabaseCharsetListener implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        System.out.println("================================================================");
        System.out.println("正在检查数据库字符集配置...");
        
        // 检查JDBC URL是否包含字符集配置
        if (!datasourceUrl.contains("characterEncoding=utf8") &&
            !datasourceUrl.contains("useUnicode=true") &&
            !datasourceUrl.contains("utf8mb4")) {
            System.out.println("警告: 数据库连接URL未明确指定UTF-8字符集");
        } else {
            System.out.println("数据库连接URL包含UTF-8字符集配置");
        }
        
        try {
            // 检查系统变量
            List<Map<String, Object>> variables = jdbcTemplate.queryForList(
                    "SHOW VARIABLES LIKE '%character%'");
            
            System.out.println("数据库字符集配置:");
            for (Map<String, Object> row : variables) {
                String name = (String) row.get("Variable_name");
                String value = (String) row.get("Value");
                System.out.println(name + " = " + value);
                
                // 确认关键字符集变量
                if (name.equals("character_set_database") || 
                    name.equals("character_set_server") ||
                    name.equals("character_set_client") ||
                    name.equals("character_set_connection") ||
                    name.equals("character_set_results")) {
                    
                    if (!value.toLowerCase().contains("utf8")) {
                        System.out.println("警告: " + name + " 未设置为UTF-8!");
                    }
                }
            }
            
            // 检查blogs表的字符集
            System.out.println("检查blogs表的字符集配置:");
            List<Map<String, Object>> tableInfo = jdbcTemplate.queryForList(
                    "SHOW TABLE STATUS WHERE Name = 'blogs'");
            
            if (!tableInfo.isEmpty()) {
                Map<String, Object> tableRow = tableInfo.get(0);
                String collation = (String) tableRow.get("Collation");
                System.out.println("blogs表的排序规则: " + collation);
                
                if (collation == null || !collation.toLowerCase().contains("utf8")) {
                    System.out.println("警告: blogs表的排序规则不是UTF-8!");
                }
                
                // 检查content列的字符集
                List<Map<String, Object>> columnInfo = jdbcTemplate.queryForList(
                        "SHOW FULL COLUMNS FROM blogs WHERE Field = 'content'");
                
                if (!columnInfo.isEmpty()) {
                    Map<String, Object> contentColumn = columnInfo.get(0);
                    String contentCollation = (String) contentColumn.get("Collation");
                    System.out.println("content列的排序规则: " + contentCollation);
                    
                    if (contentCollation == null || !contentCollation.toLowerCase().contains("utf8")) {
                        System.out.println("警告: content列的排序规则不是UTF-8!");
                    }
                }
            }
            
            System.out.println("数据库字符集检查完成");
        } catch (Exception e) {
            System.err.println("检查数据库字符集时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("================================================================");
    }
} 