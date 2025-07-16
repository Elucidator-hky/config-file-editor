package com.configtool.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 环境配置类
 * 定义自动生成模板时需要的环境变量和路径配置
 */
public class EnvironmentConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);
    
    /**
     * 环境变量配置文件路径
     */
    public static final String ENV_CONFIG_FILE = "data/environment-vars.ini";
    
    /**
     * 默认环境变量列表（当配置文件不存在时使用）
     */
    private static final String[] DEFAULT_ENV_VARS = {
        "NGINX_HOME",
        "JAVA_HOME", 
        "TOMCAT_HOME",
        "REDIS_HOME",
        "MYSQL_HOME",
        "NODE_HOME"
    };
    
    /**
     * 从配置文件读取环境变量列表
     */
    public static String[] getRequiredEnvVars() {
        try {
            File configFile = new File(ENV_CONFIG_FILE);
            if (!configFile.exists()) {
                logger.warn("环境变量配置文件不存在: {}, 使用默认配置", ENV_CONFIG_FILE);
                return DEFAULT_ENV_VARS;
            }
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
            
            String envVarsStr = props.getProperty("environment_variables", "");
            if (envVarsStr.trim().isEmpty()) {
                logger.warn("配置文件中未找到environment_variables配置，使用默认配置");
                return DEFAULT_ENV_VARS;
            }
            
            // 解析环境变量列表
            String[] envVars = envVarsStr.split(",");
            List<String> trimmedVars = new ArrayList<>();
            for (String var : envVars) {
                String trimmed = var.trim();
                if (!trimmed.isEmpty()) {
                    trimmedVars.add(trimmed);
                }
            }
            
            if (trimmedVars.isEmpty()) {
                logger.warn("配置文件中环境变量列表为空，使用默认配置");
                return DEFAULT_ENV_VARS;
            }
            
            String[] result = trimmedVars.toArray(new String[0]);
            logger.info("从配置文件读取到 {} 个环境变量: {}", result.length, Arrays.toString(result));
            return result;
            
        } catch (IOException e) {
            logger.error("读取环境变量配置文件失败: {}", ENV_CONFIG_FILE, e);
            logger.warn("使用默认环境变量配置");
            return DEFAULT_ENV_VARS;
        }
    }
    
    /**
     * 需要检查的环境变量列表
     * 动态从配置文件读取
     */
    public static String[] REQUIRED_ENV_VARS = getRequiredEnvVars();
    
    /**
     * 刷新环境变量配置
     * 重新从配置文件读取环境变量列表
     */
    public static void refreshEnvVars() {
        logger.info("刷新环境变量配置...");
        REQUIRED_ENV_VARS = getRequiredEnvVars();
        logger.info("环境变量配置已刷新");
    }
    
    /**
     * 半成品模板存放目录
     */
    public static final String SKELETON_TEMPLATES_DIR = "data/skeletons/";
    
    /**
     * DFM半成品模板文件名
     */
    public static final String DFM_SKELETON_TEMPLATE = "dfm-skeleton.json";
    
    /**
     * KMVUE半成品模板文件名
     */
    public static final String KMVUE_SKELETON_TEMPLATE = "kmvue-skeleton.json";
    
    /**
     * 相对路径标记前缀
     */
    public static final String RELATIVE_PATH_MARKER = "{{RELATIVE}}";
    
    /**
     * 环境变量路径标记前缀
     */
    public static final String ENV_PATH_MARKER_PREFIX = "{{ENV:";
    
    /**
     * 环境变量路径标记后缀  
     */
    public static final String ENV_PATH_MARKER_SUFFIX = "}}";
} 