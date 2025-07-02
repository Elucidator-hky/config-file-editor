package com.configtool.config;

/**
 * 环境配置类
 * 定义自动生成模板时需要的环境变量和路径配置
 */
public class EnvironmentConfig {
    
    /**
     * 需要检查的环境变量列表
     * 可根据实际需要添加或修改
     */
    public static final String[] REQUIRED_ENV_VARS = {
        "NGINX_HOME",
        "JAVA_HOME", 
        "TOMCAT_HOME",
        "REDIS_HOME",
        "MYSQL_HOME",
        "NODE_HOME"
    };
    
    /**
     * 半成品模板存放目录
     */
    public static final String SKELETON_TEMPLATES_DIR = "data/skeletons/";
    
    /**
     * DFM半成品模板文件名
     */
    public static final String DFM_SKELETON_TEMPLATE = "dfm-skeleton.json";
    
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