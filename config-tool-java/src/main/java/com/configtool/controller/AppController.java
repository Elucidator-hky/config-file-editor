package com.configtool.controller;

import com.configtool.config.EnvironmentConfig;
import com.configtool.model.*;
import com.configtool.service.ConfigService;
import com.configtool.service.FileProcessor;
import com.configtool.service.NacosApiService;
import com.configtool.service.TemplateService;
import com.configtool.service.TemplateAutoGenerator;
import com.configtool.service.DatabaseTestService;
import com.configtool.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用控制器
 * 提供JavaScript Bridge方法，供前端调用
 */
public class AppController {
    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    
    private final TemplateService templateService;
    private final FileProcessor fileProcessor;
    private final ConfigService configService;
    private final TemplateAutoGenerator templateAutoGenerator;
    private final NacosApiService nacosApiService;
    private final DatabaseTestService databaseTestService;
    private final String appType;
    private final boolean isParameterMode;

    public AppController() {
        this("dfm", false); // 默认为dfm，正常模式
    }
    
    public AppController(String appType, boolean isParameterMode) {
        this.appType = appType != null ? appType.toLowerCase() : "dfm";
        this.isParameterMode = isParameterMode;
        this.templateService = new TemplateService();
        this.fileProcessor = new FileProcessor();
        this.configService = new ConfigService(this.appType); // 传递appType参数
        this.templateAutoGenerator = new TemplateAutoGenerator(this.appType);
        this.nacosApiService = new NacosApiService();
        this.databaseTestService = new DatabaseTestService();
    }

    /**
     * 获取模板列表
     */
    public String getTemplatesList() {
        try {
            List<Template> templates = templateService.getAllTemplates();
            ApiResponse<List<Template>> response = ApiResponse.success(templates);
            return JsonUtil.toJson(response);
        } catch (Exception e) {
            logger.error("获取模板列表失败", e);
            ApiResponse<Object> response = ApiResponse.error("获取模板列表失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 获取模板详情
     */
    public String getTemplateDetail(String templateId) {
        try {
            if (templateId == null || templateId.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("模板ID不能为空");
                return JsonUtil.toJson(response);
            }
            
            Template template = templateService.getTemplateById(templateId.trim());
            if (template == null) {
                ApiResponse<Object> response = ApiResponse.error("模板不存在");
                return JsonUtil.toJson(response);
            }
            
            ApiResponse<Template> response = ApiResponse.success(template);
            return JsonUtil.toJson(response);
        } catch (Exception e) {
            logger.error("获取模板详情失败: {}", templateId, e);
            ApiResponse<Object> response = ApiResponse.error("获取模板详情失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 获取模板详情和状态信息
     */
    public String getTemplateDetailWithStatus(String templateId) {
        try {
            if (templateId == null || templateId.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("模板ID不能为空");
                return JsonUtil.toJson(response);
            }
            
            TemplateWithStatus templateWithStatus = configService.checkTemplateStatus(templateId.trim());
            if (templateWithStatus == null) {
                ApiResponse<Object> response = ApiResponse.error("模板不存在");
                return JsonUtil.toJson(response);
            }
            
            ApiResponse<TemplateWithStatus> response = ApiResponse.success(templateWithStatus);
            return JsonUtil.toJson(response);
        } catch (Exception e) {
            logger.error("获取模板详情和状态失败: {}", templateId, e);
            ApiResponse<Object> response = ApiResponse.error("获取模板详情和状态失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 保存模板
     */
    public String saveTemplate(String templateJson) {
        try {
            if (templateJson == null || templateJson.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("模板数据不能为空");
                return JsonUtil.toJson(response);
            }
            
            Template template = JsonUtil.fromJson(templateJson, Template.class);
            Template savedTemplate = templateService.saveTemplate(template);
            
            ApiResponse<Template> response = ApiResponse.success(savedTemplate);
            return JsonUtil.toJson(response);
        } catch (Exception e) {
            logger.error("保存模板失败", e);
            ApiResponse<Object> response = ApiResponse.error("保存模板失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 删除模板
     */
    public String deleteTemplate(String templateId) {
        try {
            if (templateId == null || templateId.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("模板ID不能为空");
                return JsonUtil.toJson(response);
            }
            
            boolean deleted = templateService.deleteTemplate(templateId.trim());
            if (deleted) {
                ApiResponse<Boolean> response = ApiResponse.success(true);
                return JsonUtil.toJson(response);
            } else {
                ApiResponse<Object> response = ApiResponse.error("删除模板失败");
                return JsonUtil.toJson(response);
            }
        } catch (Exception e) {
            logger.error("删除模板失败: {}", templateId, e);
            ApiResponse<Object> response = ApiResponse.error("删除模板失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 查找匹配项
     */
    public String findMatches(String requestJson) {
        try {
            if (requestJson == null || requestJson.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("请求数据不能为空");
                return JsonUtil.toJson(response);
            }
            
            FindMatchesRequest request = JsonUtil.fromJson(requestJson, FindMatchesRequest.class);
            List<MatchResult> matches = fileProcessor.findMatches(
                    request.getFilePath(), 
                    request.getPrefix(), 
                    request.getSuffix()
            );
            
            ApiResponse<List<MatchResult>> response = ApiResponse.success(matches);
            return JsonUtil.toJson(response);
        } catch (Exception e) {
            logger.error("查找匹配项失败", e);
            ApiResponse<Object> response = ApiResponse.error("查找匹配项失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 预检查配置应用
     */
    public String preCheckChanges(String requestJson) {
        try {
            if (requestJson == null || requestJson.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("请求数据不能为空");
                return JsonUtil.toJson(response);
            }
            
            ApplyChangesRequest request = JsonUtil.fromJson(requestJson, ApplyChangesRequest.class);
            PreCheckResult result = configService.preCheckChanges(request);
            
            ApiResponse<PreCheckResult> response = ApiResponse.success(result);
            return JsonUtil.toJson(response);
        } catch (Exception e) {
            logger.error("预检查配置应用失败", e);
            ApiResponse<Object> response = ApiResponse.error("预检查失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 应用配置更改
     */
    public String applyChanges(String requestJson) {
        try {
            if (requestJson == null || requestJson.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("请求数据不能为空");
                return JsonUtil.toJson(response);
            }
            
            ApplyChangesRequest request = JsonUtil.fromJson(requestJson, ApplyChangesRequest.class);
            ApplyResult result = configService.applyChanges(request);
            
            ApiResponse<ApplyResult> response = ApiResponse.success(result);
            return JsonUtil.toJson(response);
        } catch (Exception e) {
            logger.error("应用配置更改失败", e);
            ApiResponse<Object> response = ApiResponse.error("应用配置更改失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 自动生成配置模板（根据appType决定生成DFM或KMVUE模板）
     */
    public String generateTemplate() {
        try {
            String templateType = appType.toUpperCase();
            logger.info("开始生成{}配置模板", templateType);
            
            Template generated = templateAutoGenerator.generateTemplate();
            Template saved = templateService.saveTemplate(generated);
            
            logger.info("{}配置模板生成并保存成功: {}", templateType, saved.getName());
            ApiResponse<Template> response = ApiResponse.success(saved);
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            logger.error("生成{}配置模板失败", appType.toUpperCase(), e);
            ApiResponse<Object> response = ApiResponse.error("生成" + appType.toUpperCase() + "配置模板失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }
    
    /**
     * 自动生成DFM配置模板（保留向下兼容）
     */
    public String generateDfmTemplate() {
        return generateTemplate();
    }
    
    /**
     * 获取当前应用类型
     */
    public String getAppType() {
        return appType;
    }
    
    /**
     * 是否为参数启动模式
     */
    public boolean isParameterMode() {
        return isParameterMode;
    }
    
    /**
     * 刷新配置数据
     * 重新读取环境变量配置等
     */
    public String refreshConfigData() {
        try {
            logger.info("开始刷新配置数据");
            
            // 刷新环境变量配置
            EnvironmentConfig.refreshEnvVars();
            
            logger.info("配置数据刷新成功");
            ApiResponse<String> response = ApiResponse.success("配置数据已刷新");
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            logger.error("刷新配置数据失败", e);
            ApiResponse<Object> response = ApiResponse.error("刷新配置数据失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }
    
    /**
     * 更新skeleton文件
     * 用于联动时同步更新skeleton文件的配置项值
     */
    public String updateSkeleton(String updateDataJson) {
        try {
            logger.info("=== 开始更新skeleton文件 ===");
            logger.info("接收到的更新数据: {}", updateDataJson);
            
            if (updateDataJson == null || updateDataJson.trim().isEmpty()) {
                logger.error("更新数据为空");
                ApiResponse<Object> response = ApiResponse.error("更新数据不能为空");
                return JsonUtil.toJson(response);
            }
            
            // 解析更新数据
            UpdateSkeletonRequest updateData = JsonUtil.fromJson(updateDataJson, UpdateSkeletonRequest.class);
            logger.info("解析更新数据成功，模板ID: {}, 更新项数量: {}", updateData.getTemplateId(), updateData.getUpdates().size());
            
            // 读取当前的skeleton文件
            String skeletonId = updateData.getTemplateId();
            String skeletonFileName = "data/skeletons/" + skeletonId.replace("-template", "") + ".json";
            
            logger.info("准备更新skeleton文件: {}", skeletonFileName);
            
            // 读取skeleton文件
            Template skeleton = templateService.getTemplateFromFile(skeletonFileName);
            if (skeleton == null) {
                logger.error("Skeleton文件不存在: {}", skeletonFileName);
                ApiResponse<Object> response = ApiResponse.error("Skeleton文件不存在: " + skeletonFileName);
                return JsonUtil.toJson(response);
            }
            
            logger.info("成功读取skeleton文件，包含{}个配置项", skeleton.getItems().size());
            
            // 更新skeleton中的配置项值
            int updatedCount = 0;
            for (UpdateSkeletonRequest.UpdateItem updateItem : updateData.getUpdates()) {
                logger.info("尝试更新配置项: {}", updateItem.getItemName());
                boolean found = false;
                
                for (ConfigItem item : skeleton.getItems()) {
                    logger.info("检查skeleton中的配置项: {}", item.getName());
                    if (item.getName().equals(updateItem.getItemName())) {
                        String oldDefault = item.getDefaultValue();
                        String oldCurrent = item.getCurrentValue();
                        
                        item.setDefaultValue(updateItem.getDefaultValue());
                        item.setCurrentValue(updateItem.getCurrentValue());
                        updatedCount++;
                        found = true;
                        
                        logger.info("✓ 更新配置项成功: {} | 默认值: {} -> {} | 当前值: {} -> {}", 
                            item.getName(), oldDefault, updateItem.getDefaultValue(), 
                            oldCurrent, updateItem.getCurrentValue());
                        break;
                    }
                }
                
                if (!found) {
                    logger.warn("✗ 未找到配置项: {}", updateItem.getItemName());
                }
            }
            
            // 保存更新后的skeleton文件
            logger.info("准备保存skeleton文件...");
            templateService.saveTemplateToFile(skeleton, skeletonFileName);
            
            logger.info("=== Skeleton文件更新成功，共更新了{}个配置项 ===", updatedCount);
            ApiResponse<String> response = ApiResponse.success("Skeleton文件更新成功，共更新了" + updatedCount + "个配置项");
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            logger.error("=== 更新skeleton文件失败 ===", e);
            ApiResponse<Object> response = ApiResponse.error("更新skeleton文件失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }
    
    /**
     * 检查Nacos服务状态
     */
    public String checkNacosStatus() {
        try {
            logger.info("检查Nacos服务状态");
            
            Map<String, Object> status = new HashMap<>();
            status.put("enabled", nacosApiService.isEnabled());
            status.put("available", nacosApiService.isNacosAvailable());
            status.put("url", nacosApiService.getNacosUrl());
            status.put("namespace", nacosApiService.getNamespace());
            
            // 测试获取配置
            if (nacosApiService.isEnabled() && nacosApiService.isNacosAvailable()) {
                try {
                    String kmvueConfig = nacosApiService.getConfig("kmvue-commonConfig.yml", "DEFAULT_GROUP");
                    String dfmcloudConfig = nacosApiService.getConfig("dfmcloud-commonConfig.yml", "DEFAULT_GROUP");
                    
                    status.put("kmvueConfigExists", kmvueConfig != null);
                    status.put("dfmcloudConfigExists", dfmcloudConfig != null);
                    status.put("kmvueConfigLength", kmvueConfig != null ? kmvueConfig.length() : 0);
                    status.put("dfmcloudConfigLength", dfmcloudConfig != null ? dfmcloudConfig.length() : 0);
                    
                } catch (Exception e) {
                    logger.warn("获取Nacos配置失败", e);
                    status.put("configError", e.getMessage());
                }
            }
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success(status);
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            logger.error("检查Nacos服务状态失败", e);
            ApiResponse<Object> response = ApiResponse.error("检查Nacos服务状态失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }
    
    /**
     * 测试Nacos配置更新
     */
    public String testNacosUpdate() {
        try {
            logger.info("测试Nacos配置更新");
            
            if (!nacosApiService.isEnabled()) {
                ApiResponse<Object> response = ApiResponse.error("Nacos功能未启用");
                return JsonUtil.toJson(response);
            }
            
            if (!nacosApiService.isNacosAvailable()) {
                ApiResponse<Object> response = ApiResponse.error("Nacos服务不可用");
                return JsonUtil.toJson(response);
            }
            
            // 测试更新kmvue配置
            boolean result = nacosApiService.updateDatabaseConfig(
                "kmvue-commonConfig.yml", 
                "DEFAULT_GROUP", 
                "jdbc:mysql://127.0.0.1:3306/kmview?useUnicode=true&characterEncoding=utf-8&generateSimpleParameterMetadata=true&zeroDateTimeBehavior=convertToNull&serverTimezone=GMT%2B8",
                "root",
                "sasa",
                "com.mysql.cj.jdbc.Driver"
            );
            
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("updateSuccess", result);
            testResult.put("message", result ? "测试更新成功" : "测试更新失败");
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success(testResult);
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            logger.error("测试Nacos配置更新失败", e);
            ApiResponse<Object> response = ApiResponse.error("测试失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }

    /**
     * 测试数据库连接
     */
    public String testDatabaseConnection(String requestJson) {
        try {
            if (requestJson == null || requestJson.trim().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("请求数据不能为空");
                return JsonUtil.toJson(response);
            }
            
            ApplyChangesRequest request = JsonUtil.fromJson(requestJson, ApplyChangesRequest.class);
            
            // 提取数据库相关配置
            Map<String, String> databaseConfig = extractDatabaseConfig(request.getValues());
            
            if (databaseConfig.isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("没有找到数据库配置信息");
                return JsonUtil.toJson(response);
            }
            
            // 测试数据库连接
            DatabaseTestService.DatabaseTestResult result = databaseTestService.testDatabaseConnection(databaseConfig);
            
            ApiResponse<DatabaseTestService.DatabaseTestResult> response = ApiResponse.success(result);
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            logger.error("测试数据库连接失败", e);
            ApiResponse<Object> response = ApiResponse.error("测试数据库连接失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
    }
    
    /**
     * 提取数据库配置
     */
    private Map<String, String> extractDatabaseConfig(Map<String, String> values) {
        Map<String, String> databaseConfig = new HashMap<>();
        
        // 定义数据库相关配置项名称
        String[] databaseItemNames = {
            "数据库类型", "数据库服务器地址", "数据库端口", "数据库名称", "数据库用户名", "数据库密码"
        };
        
        for (String itemName : databaseItemNames) {
            if (values.containsKey(itemName)) {
                databaseConfig.put(itemName, values.get(itemName));
            }
        }
        
        return databaseConfig;
    }
} 