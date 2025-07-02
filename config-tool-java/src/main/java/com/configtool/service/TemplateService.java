package com.configtool.service;

import com.configtool.model.ConfigItem;
import com.configtool.model.FileTarget;
import com.configtool.model.Template;
import com.configtool.util.JsonUtil;
import com.fasterxml.uuid.Generators;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板管理服务
 * 负责模板的增删改查操作
 */
public class TemplateService {
    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);
    private static final String TEMPLATES_DIR = "data/templates/";
    
    private final FileProcessor fileProcessor;

    public TemplateService() {
        this.fileProcessor = new FileProcessor();
        ensureTemplatesDirectory();
    }

    /**
     * 确保模板目录存在
     */
    private void ensureTemplatesDirectory() {
        File dir = new File(TEMPLATES_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("创建模板目录: {}", dir.getAbsolutePath());
            }
        }
    }

    /**
     * 获取所有模板列表
     */
    public List<Template> getAllTemplates() {
        List<Template> templates = new ArrayList<>();
        File dir = new File(TEMPLATES_DIR);
        
        if (!dir.exists()) {
            logger.warn("模板目录不存在: {}", dir.getAbsolutePath());
            return templates;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return templates;
        }
        
        for (File file : files) {
            try {
                String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                Template template = JsonUtil.fromJson(content, Template.class);
                if (template != null) {
                    templates.add(template);
                }
            } catch (IOException e) {
                logger.error("读取模板文件失败: {}", file.getName(), e);
            }
        }
        
        logger.info("加载了 {} 个模板", templates.size());
        return templates;
    }

    /**
     * 根据ID获取模板详情（包含当前值）
     */
    public Template getTemplateById(String templateId) {
        File file = new File(TEMPLATES_DIR + templateId + ".json");
        
        if (!file.exists()) {
            logger.warn("模板文件不存在: {}", file.getAbsolutePath());
            return null;
        }
        
        try {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            Template template = JsonUtil.fromJson(content, Template.class);
            
            if (template != null) {
                // 填充当前值
                fillCurrentValues(template);
            }
            
            return template;
        } catch (IOException e) {
            logger.error("读取模板文件失败: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * 填充模板配置项的当前值和状态
     */
    private void fillCurrentValues(Template template) {
        if (template.getItems() == null) {
            return;
        }
        
        for (ConfigItem item : template.getItems()) {
            if (item.getTargets() != null && !item.getTargets().isEmpty()) {
                analyzeConfigItemStatus(item);
            } else {
                // 没有目标点的配置项
                item.setCurrentValue("");
                item.setStatus("ERROR");
            }
        }
    }
    
    /**
     * 分析配置项状态：检查所有目标点
     */
    private void analyzeConfigItemStatus(ConfigItem item) {
        List<String> allValues = new ArrayList<>();
        List<String> successValues = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        
        // 检查每个目标点
        for (FileTarget target : item.getTargets()) {
            String value = fileProcessor.extractCurrentValue(target);
            allValues.add(value);
            
            if (isErrorMessage(value)) {
                errorMessages.add(value);
            } else {
                successValues.add(value);
            }
        }
        
        // 确定配置项状态和显示值
        if (errorMessages.isEmpty()) {
            // 所有目标点都成功
            if (allValuesConsistent(successValues)) {
                item.setStatus("SUCCESS");
                item.setCurrentValue(successValues.get(0)); // 所有值一致，取第一个
            } else {
                item.setStatus("WARNING"); // 成功但不一致
                item.setCurrentValue(successValues.get(0) + " (存在不一致)");
            }
        } else if (successValues.isEmpty()) {
            // 所有目标点都失败
            item.setStatus("ERROR");
            item.setCurrentValue(errorMessages.get(0)); // 显示第一个错误信息
        } else {
            // 部分成功部分失败
            item.setStatus("WARNING");
            item.setCurrentValue(successValues.get(0) + " (部分目标点有问题)");
        }
        
        logger.debug("配置项 {} 状态分析完成: 总目标点={}, 成功={}, 失败={}, 状态={}", 
                    item.getName(), allValues.size(), successValues.size(), 
                    errorMessages.size(), item.getStatus());
    }
    
    /**
     * 判断是否为错误信息
     */
    private boolean isErrorMessage(String value) {
        return value != null && (
            value.equals("文件不存在") || 
            value.equals("前缀未找到") || 
            value.equals("行号无效") || 
            value.equals("读取失败")
        );
    }
    
    /**
     * 检查所有值是否一致
     */
    private boolean allValuesConsistent(List<String> values) {
        if (values.isEmpty()) return true;
        
        String firstValue = values.get(0);
        return values.stream().allMatch(value -> value.equals(firstValue));
    }

    /**
     * 保存模板
     */
    public Template saveTemplate(Template template) {
        try {
            // 如果没有ID，生成一个新ID
            if (template.getId() == null || template.getId().isEmpty()) {
                template.setId(Generators.timeBasedGenerator().generate().toString());
            }
            
            // 确保items不为null
            if (template.getItems() == null) {
                template.setItems(new ArrayList<>());
            }
            
            String json = JsonUtil.toJson(template);
            File file = new File(TEMPLATES_DIR + template.getId() + ".json");
            
            FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
            
            logger.info("保存模板成功: {} -> {}", template.getName(), file.getAbsolutePath());
            return template;
            
        } catch (IOException e) {
            logger.error("保存模板失败: {}", template.getName(), e);
            throw new RuntimeException("保存模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除模板
     */
    public boolean deleteTemplate(String templateId) {
        File file = new File(TEMPLATES_DIR + templateId + ".json");
        
        if (!file.exists()) {
            logger.warn("要删除的模板文件不存在: {}", file.getAbsolutePath());
            return false;
        }
        
        boolean deleted = file.delete();
        if (deleted) {
            logger.info("删除模板成功: {}", file.getAbsolutePath());
        } else {
            logger.error("删除模板失败: {}", file.getAbsolutePath());
        }
        
        return deleted;
    }

    /**
     * 检查模板是否存在
     */
    public boolean templateExists(String templateId) {
        File file = new File(TEMPLATES_DIR + templateId + ".json");
        return file.exists();
    }
} 