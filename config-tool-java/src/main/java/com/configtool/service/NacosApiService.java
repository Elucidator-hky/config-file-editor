package com.configtool.service;

import com.configtool.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Nacos API服务类
 * 提供与Nacos配置中心的交互功能
 */
public class NacosApiService {
    private static final Logger logger = LoggerFactory.getLogger(NacosApiService.class);
    
    private String nacosUrl;
    private String namespace;
    private String username;
    private String password;
    private boolean enabled;
    
    public NacosApiService() {
        loadNacosConfig();
    }
    
    /**
     * 加载Nacos配置
     */
    private void loadNacosConfig() {
        try {
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/nacos-config.properties"));
            
            this.nacosUrl = props.getProperty("nacos.url", "http://localhost:8848");
            this.namespace = props.getProperty("nacos.namespace", "public");
            this.username = props.getProperty("nacos.username", "nacos");
            this.password = props.getProperty("nacos.password", "nacos");
            this.enabled = Boolean.parseBoolean(props.getProperty("nacos.enabled", "false"));
            
            logger.info("Nacos配置加载成功 - URL: {}, Namespace: {}, Enabled: {}", 
                nacosUrl, namespace, enabled);
                
        } catch (Exception e) {
            logger.warn("加载Nacos配置失败，将使用默认配置: {}", e.getMessage());
            this.nacosUrl = "http://localhost:8848";
            this.namespace = "public";
            this.username = "nacos";
            this.password = "nacos";
            this.enabled = false;
        }
    }
    
    /**
     * 检查Nacos服务是否可用
     */
    public boolean isNacosAvailable() {
        if (!enabled) {
            return false;
        }
        
        try {
            URL url = new URL(nacosUrl + "/nacos/v1/ns/operator/metrics");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            int responseCode = conn.getResponseCode();
            boolean available = responseCode == 200;
            
            logger.info("Nacos服务状态检查: {} (HTTP {})", available ? "可用" : "不可用", responseCode);
            return available;
            
        } catch (Exception e) {
            logger.warn("Nacos服务不可用: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取配置
     */
    public String getConfig(String dataId, String group) {
        if (!enabled || !isNacosAvailable()) {
            return null;
        }
        
        try {
            // 处理tenant参数：public命名空间使用空字符串
            String tenantParam = "public".equals(namespace) ? "" : namespace;
            String urlStr = String.format("%s/nacos/v1/cs/configs?dataId=%s&group=%s&tenant=%s",
                nacosUrl, 
                URLEncoder.encode(dataId, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(group, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(tenantParam, StandardCharsets.UTF_8.toString()));
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                
                String config = response.toString().trim();
                logger.info("获取Nacos配置成功: dataId={}, group={}, 长度={}", dataId, group, config.length());
                return config;
            } else {
                logger.warn("获取Nacos配置失败: dataId={}, group={}, HTTP {}", dataId, group, responseCode);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("获取Nacos配置异常: dataId={}, group={}", dataId, group, e);
            return null;
        }
    }
    
    /**
     * 发布配置
     */
    public boolean publishConfig(String dataId, String group, String content) {
        if (!enabled || !isNacosAvailable()) {
            logger.warn("Nacos未启用或不可用，跳过配置发布");
            return false;
        }
        
        try {
            String urlStr = nacosUrl + "/nacos/v1/cs/configs";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            // 构建请求参数
            StringBuilder params = new StringBuilder();
            params.append("dataId=").append(URLEncoder.encode(dataId, StandardCharsets.UTF_8.toString()));
            params.append("&group=").append(URLEncoder.encode(group, StandardCharsets.UTF_8.toString()));
            
            // 处理tenant参数：public命名空间使用空字符串
            String tenantParam = "public".equals(namespace) ? "" : namespace;
            params.append("&tenant=").append(URLEncoder.encode(tenantParam, StandardCharsets.UTF_8.toString()));
            params.append("&content=").append(URLEncoder.encode(content, StandardCharsets.UTF_8.toString()));
            
            // 添加type参数，指定为yaml格式
            params.append("&type=yaml");
            
            // 发送请求
            OutputStream os = conn.getOutputStream();
            os.write(params.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String response = reader.readLine();
                reader.close();
                
                boolean success = "true".equals(response);
                logger.info("发布Nacos配置{}: dataId={}, group={}, 内容长度={}", 
                    success ? "成功" : "失败", dataId, group, content.length());
                return success;
            } else {
                logger.warn("发布Nacos配置失败: dataId={}, group={}, HTTP {}", dataId, group, responseCode);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("发布Nacos配置异常: dataId={}, group={}", dataId, group, e);
            return false;
        }
    }
    
    /**
     * 删除配置
     */
    public boolean deleteConfig(String dataId, String group) {
        if (!enabled || !isNacosAvailable()) {
            return false;
        }
        
        try {
            String urlStr = nacosUrl + "/nacos/v1/cs/configs";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            // 构建请求参数
            StringBuilder params = new StringBuilder();
            params.append("dataId=").append(URLEncoder.encode(dataId, StandardCharsets.UTF_8.toString()));
            params.append("&group=").append(URLEncoder.encode(group, StandardCharsets.UTF_8.toString()));
            params.append("&tenant=").append(URLEncoder.encode(namespace, StandardCharsets.UTF_8.toString()));
            
            // 发送请求
            OutputStream os = conn.getOutputStream();
            os.write(params.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            
            int responseCode = conn.getResponseCode();
            boolean success = responseCode == 200;
            
            logger.info("删除Nacos配置{}: dataId={}, group={}", success ? "成功" : "失败", dataId, group);
            return success;
            
        } catch (Exception e) {
            logger.error("删除Nacos配置异常: dataId={}, group={}", dataId, group, e);
            return false;
        }
    }
    
    /**
     * 更新YAML配置中的数据库配置
     */
    public boolean updateDatabaseConfig(String dataId, String group, String url, String username, String password, String driverClassName) {
        try {
            logger.info("开始更新数据库配置: dataId={}, group={}", dataId, group);
            logger.info("配置参数 - URL: {}, Username: {}, Password: [隐藏], Driver: {}", url, username, driverClassName);
            
            // 获取现有配置
            String currentConfig = getConfig(dataId, group);
            if (currentConfig == null) {
                logger.warn("无法获取现有配置，将创建新配置: dataId={}, group={}", dataId, group);
                currentConfig = "";
            } else {
                logger.info("获取到现有配置，长度: {}", currentConfig.length());
            }
            
            // 更新数据库配置
            String updatedConfig = updateYamlDatabaseConfig(currentConfig, url, username, password, driverClassName);
            logger.info("更新后的配置长度: {}", updatedConfig.length());
            logger.info("更新后的配置内容:\n{}", updatedConfig);
            
            // 发布更新后的配置
            boolean result = publishConfig(dataId, group, updatedConfig);
            logger.info("配置发布结果: {}", result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("更新数据库配置失败: dataId={}, group={}", dataId, group, e);
            return false;
        }
    }
    
    /**
     * 更新YAML格式的数据库配置
     */
    private String updateYamlDatabaseConfig(String yamlContent, String url, String username, String password, String driverClassName) {
        logger.info("开始更新YAML配置，原配置长度: {}", yamlContent.length());
        
        // 如果原配置为空，创建完整配置
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            logger.info("原配置为空，创建新的完整配置");
            StringBuilder newConfig = new StringBuilder();
            newConfig.append("spring:\n");
            newConfig.append("  datasource:\n");
            newConfig.append("    url: ").append(url).append("\n");
            newConfig.append("    username: ").append(username).append("\n");
            newConfig.append("    password: ").append(password).append("\n");
            newConfig.append("    driver-class-name: ").append(driverClassName).append("\n");
            
            String result = newConfig.toString();
            logger.info("创建的新配置:\n{}", result);
            return result;
        }
        
        StringBuilder result = new StringBuilder();
        String[] lines = yamlContent.split("\n");
        
        boolean inSpring = false;
        boolean inDatasource = false;
        boolean foundSpring = false;
        boolean foundDatasource = false;
        boolean addedDatasource = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();
            
            // 检测spring配置段
            if (trimmedLine.equals("spring:")) {
                inSpring = true;
                foundSpring = true;
                result.append(line).append("\n");
                logger.debug("找到spring配置段");
                continue;
            }
            
            // 检测datasource配置段
            if (inSpring && trimmedLine.equals("datasource:")) {
                inDatasource = true;
                foundDatasource = true;
                result.append(line).append("\n");
                
                // 立即添加数据库配置
                result.append("    url: ").append(url).append("\n");
                result.append("    username: ").append(username).append("\n");
                result.append("    password: ").append(password).append("\n");
                result.append("    driver-class-name: ").append(driverClassName).append("\n");
                addedDatasource = true;
                
                logger.info("在datasource段添加了数据库配置");
                
                // 跳过原有的数据库配置项
                for (int j = i + 1; j < lines.length; j++) {
                    String nextLine = lines[j];
                    String nextTrimmed = nextLine.trim();
                    
                    // 如果是datasource的子项（4个空格开头）
                    if (nextLine.startsWith("    ") && !nextLine.startsWith("      ")) {
                        if (nextTrimmed.startsWith("url:") || 
                            nextTrimmed.startsWith("username:") || 
                            nextTrimmed.startsWith("password:") || 
                            nextTrimmed.startsWith("driver-class-name:")) {
                            // 跳过原有配置
                            i = j;
                            continue;
                        } else {
                            // 保留其他配置项
                            result.append(nextLine).append("\n");
                            i = j;
                            continue;
                        }
                    } else {
                        // 不是datasource的子项，回退
                        i = j - 1;
                        break;
                    }
                }
                
                inDatasource = false;
                continue;
            }
            
            // 检测是否离开spring配置段
            if (inSpring && !line.startsWith("  ") && !line.trim().isEmpty() && !trimmedLine.equals("spring:")) {
                inSpring = false;
                inDatasource = false;
            }
            
            // 保留其他行
            result.append(line).append("\n");
        }
        
        // 如果没有找到spring或datasource配置，添加完整配置
        if (!foundSpring) {
            logger.info("未找到spring配置，添加完整配置");
            result.append("spring:\n");
            result.append("  datasource:\n");
            result.append("    url: ").append(url).append("\n");
            result.append("    username: ").append(username).append("\n");
            result.append("    password: ").append(password).append("\n");
            result.append("    driver-class-name: ").append(driverClassName).append("\n");
        } else if (!foundDatasource) {
            logger.info("找到spring但未找到datasource配置，添加datasource配置");
            result.append("  datasource:\n");
            result.append("    url: ").append(url).append("\n");
            result.append("    username: ").append(username).append("\n");
            result.append("    password: ").append(password).append("\n");
            result.append("    driver-class-name: ").append(driverClassName).append("\n");
        }
        
        String finalResult = result.toString();
        logger.info("YAML数据库配置更新完成，最终配置长度: {}", finalResult.length());
        return finalResult;
    }
    
    // Getter方法
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getNacosUrl() {
        return nacosUrl;
    }
    
    public String getNamespace() {
        return namespace;
    }
} 