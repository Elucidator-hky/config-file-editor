package com.configtool.controller;

import com.configtool.config.EnvironmentConfig;
import com.configtool.model.*;
import com.configtool.service.ConfigService;
import com.configtool.service.FileProcessor;
import com.configtool.service.TemplateService;
import com.configtool.service.TemplateAutoGenerator;
import com.configtool.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    public AppController() {
        this.templateService = new TemplateService();
        this.fileProcessor = new FileProcessor();
        this.configService = new ConfigService();
        this.templateAutoGenerator = new TemplateAutoGenerator();
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
     * 自动生成DFM配置模板
     */
    public String generateDfmTemplate() {
        try {
            logger.info("开始生成DFM配置模板");
            
            Template generated = templateAutoGenerator.generateDfmTemplate();
            Template saved = templateService.saveTemplate(generated);
            
            logger.info("DFM配置模板生成并保存成功: {}", saved.getName());
            ApiResponse<Template> response = ApiResponse.success(saved);
            return JsonUtil.toJson(response);
            
        } catch (Exception e) {
            logger.error("生成DFM配置模板失败", e);
            ApiResponse<Object> response = ApiResponse.error("生成DFM配置模板失败: " + e.getMessage());
            return JsonUtil.toJson(response);
        }
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
} 