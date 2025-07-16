package com.configtool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连接测试服务
 */
public class DatabaseTestService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseTestService.class);
    
    /**
     * 测试数据库连接
     */
    public DatabaseTestResult testDatabaseConnection(Map<String, String> databaseConfig) {
        try {
            logger.info("开始测试数据库连接");
            
            // 提取数据库配置
            String dbType = databaseConfig.get("数据库类型");
            String address = databaseConfig.get("数据库服务器地址");
            String port = databaseConfig.get("数据库端口");
            String databaseName = databaseConfig.get("数据库名称");
            String username = databaseConfig.get("数据库用户名");
            String password = databaseConfig.get("数据库密码");
            
            // 构造连接字符串和驱动
            String url;
            String driverClassName;
            
            if ("0".equals(dbType)) {
                // SQL Server
                url = buildSqlServerUrl(address, port, databaseName);
                driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            } else if ("9".equals(dbType)) {
                // MySQL
                url = buildMysqlUrl(address, port, databaseName);
                driverClassName = "com.mysql.cj.jdbc.Driver";
            } else {
                return DatabaseTestResult.failure("未知的数据库类型: " + dbType);
            }
            
            logger.info("测试连接 - URL: {}, Driver: {}, Username: {}", url, driverClassName, username);
            
            // 加载驱动
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                return DatabaseTestResult.failure("数据库驱动未找到: " + driverClassName);
            }
            
            // 测试连接
            Connection connection = null;
            try {
                // 设置连接超时
                DriverManager.setLoginTimeout(5);
                
                connection = DriverManager.getConnection(url, username, password);
                
                // 测试连接是否有效
                if (connection != null && connection.isValid(5)) {
                    logger.info("数据库连接测试成功");
                    return DatabaseTestResult.success("数据库连接成功");
                } else {
                    return DatabaseTestResult.failure("数据库连接无效");
                }
                
            } catch (SQLException e) {
                logger.error("数据库连接测试失败", e);
                return DatabaseTestResult.failure("数据库连接失败: " + e.getMessage());
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        logger.warn("关闭数据库连接时出错", e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("数据库连接测试异常", e);
            return DatabaseTestResult.failure("数据库连接测试异常: " + e.getMessage());
        }
    }
    
    /**
     * 构造SQL Server连接URL
     */
    private String buildSqlServerUrl(String address, String port, String databaseName) {
        if (address == null || address.trim().isEmpty()) {
            address = "localhost";
        }
        if (port == null || port.trim().isEmpty()) {
            port = "1433";
        }
        if (databaseName == null || databaseName.trim().isEmpty()) {
            databaseName = "master";
        }
        
        return String.format("jdbc:sqlserver://%s:%s;DatabaseName=%s", 
            address, port, databaseName);
    }
    
    /**
     * 构造MySQL连接URL
     */
    private String buildMysqlUrl(String address, String port, String databaseName) {
        if (address == null || address.trim().isEmpty()) {
            address = "localhost";
        }
        if (port == null || port.trim().isEmpty()) {
            port = "3306";
        }
        if (databaseName == null || databaseName.trim().isEmpty()) {
            databaseName = "mysql";
        }
        
        return String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf-8&generateSimpleParameterMetadata=true&zeroDateTimeBehavior=convertToNull&serverTimezone=GMT%%2B8", 
            address, port, databaseName);
    }
    
    /**
     * 数据库测试结果
     */
    public static class DatabaseTestResult {
        private boolean success;
        private String message;
        private long testTime;
        
        private DatabaseTestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.testTime = System.currentTimeMillis();
        }
        
        public static DatabaseTestResult success(String message) {
            return new DatabaseTestResult(true, message);
        }
        
        public static DatabaseTestResult failure(String message) {
            return new DatabaseTestResult(false, message);
        }
        
        // Getters
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTestTime() {
            return testTime;
        }
    }
} 