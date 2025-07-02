package com.configtool.service;

import com.configtool.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置应用服务
 * 负责将配置更改应用到文件
 */
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    
    private final TemplateService templateService;
    private final FileProcessor fileProcessor;

    public ConfigService() {
        this.templateService = new TemplateService();
        this.fileProcessor = new FileProcessor();
    }

    /**
     * 检查模板中所有配置项的当前状态
     */
    public TemplateWithStatus checkTemplateStatus(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            throw new RuntimeException("模板ID不能为空");
        }
        
        // 获取模板
        Template template = templateService.getTemplateById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateId);
        }
        
        logger.info("开始检查模板状态: {}", template.getName());
        
        Map<String, TemplateWithStatus.ConfigItemStatus> itemStatuses = new HashMap<>();
        
        // 检查每个配置项
        if (template.getItems() != null) {
            for (ConfigItem item : template.getItems()) {
                TemplateWithStatus.ConfigItemStatus status = checkConfigItemStatus(item);
                itemStatuses.put(item.getName(), status);
            }
        }
        
        logger.info("模板状态检查完成: {}, 配置项数: {}", template.getName(), itemStatuses.size());
        
        return new TemplateWithStatus(template, itemStatuses);
    }

    /**
     * 检查单个配置项的状态
     */
    private TemplateWithStatus.ConfigItemStatus checkConfigItemStatus(ConfigItem configItem) {
        if (configItem.getTargets() == null || configItem.getTargets().isEmpty()) {
            return TemplateWithStatus.ConfigItemStatus.error("没有配置目标文件");
        }
        
        // 检查所有目标点
        java.util.List<String> successValues = new java.util.ArrayList<>();
        java.util.List<String> errorMessages = new java.util.ArrayList<>();
        java.util.List<TemplateWithStatus.TargetStatus> targetStatuses = new java.util.ArrayList<>();
        
        for (FileTarget target : configItem.getTargets()) {
            String targetStatus = "ERROR";
            String targetMessage = "";
            String targetCurrentValue = "";
            
            try {
                String filePath = fileProcessor.cleanFilePath(target.getFilePath());
                File file = new File(filePath);
                
                // 检查文件是否存在
                if (!file.exists()) {
                    targetMessage = "文件不存在: " + filePath;
                    errorMessages.add(targetMessage);
                } else if (!file.canRead()) {
                    // 检查文件是否可读
                    targetMessage = "文件无法读取: " + filePath;
                    errorMessages.add(targetMessage);
                } else {
                    // 读取文件并检查行号
                    try {
                        java.nio.charset.Charset encoding = fileProcessor.detectFileEncoding(file);
                        java.util.List<String> lines = org.apache.commons.io.FileUtils.readLines(file, encoding);
                        
                        // 检查行号是否有效
                        if (target.getLineNumber() < 1 || target.getLineNumber() > lines.size()) {
                            targetMessage = String.format("行号无效: %d (文件共 %d 行)", target.getLineNumber(), lines.size());
                            errorMessages.add(targetMessage);
                        } else {
                            // 获取指定行的内容
                            String line = lines.get(target.getLineNumber() - 1);
                            String prefix = target.getPrefix();
                            String suffix = target.getSuffix();
                            
                            // 检查前缀是否存在
                            if (prefix != null && !prefix.isEmpty() && !line.contains(prefix)) {
                                targetMessage = String.format("第 %d 行未找到前缀: %s", target.getLineNumber(), prefix);
                                errorMessages.add(targetMessage);
                            } else {
                                // 提取当前值 - 成功
                                targetCurrentValue = extractCurrentValue(line, prefix, suffix);
                                successValues.add(targetCurrentValue);
                                targetStatus = "SUCCESS";
                                targetMessage = "读取成功";
                            }
                        }
                        
                    } catch (Exception e) {
                        targetMessage = "读取文件失败: " + e.getMessage();
                        errorMessages.add(targetMessage);
                    }
                }
                
            } catch (Exception e) {
                targetMessage = "处理文件路径失败: " + e.getMessage();
                errorMessages.add(targetMessage);
            }
            
            // 创建目标点状态
            TemplateWithStatus.TargetStatus ts = new TemplateWithStatus.TargetStatus(
                target.getId(),
                target.getFilePath(),
                target.getLineNumber(),
                target.getPrefix(),
                target.getSuffix(),
                targetStatus,
                targetMessage,
                targetCurrentValue
            );
            targetStatuses.add(ts);
        }
        
        // 判断整体状态 - 简化为只有SUCCESS和ERROR两种状态
        TemplateWithStatus.ConfigItemStatus status;
        if (errorMessages.isEmpty()) {
            // 所有目标点都成功
            String firstValue = successValues.get(0);
            // 不管值是否一致，只要所有目标点都成功就是SUCCESS
            status = TemplateWithStatus.ConfigItemStatus.ok(firstValue);
        } else {
            // 有任何一个目标点失败就是ERROR
            String currentValue = successValues.isEmpty() ? "" : successValues.get(0);
            String errorDetail = String.join("; ", errorMessages);
            status = TemplateWithStatus.ConfigItemStatus.error("部分目标点失败: " + errorDetail);
        }
        
        // 设置目标点详细状态
        status.setTargetStatuses(targetStatuses);
        return status;
    }

    /**
     * 从行内容中提取当前值
     */
    private String extractCurrentValue(String line, String prefix, String suffix) {
        if (prefix == null || prefix.isEmpty()) {
            return line.trim();
        }
        
        int prefixIndex = line.indexOf(prefix);
        if (prefixIndex == -1) {
            return "";
        }
        
        String afterPrefix = line.substring(prefixIndex + prefix.length());
        
        if (suffix == null || suffix.isEmpty()) {
            return afterPrefix.trim();
        }
        
        int suffixIndex = afterPrefix.indexOf(suffix);
        if (suffixIndex == -1) {
            return afterPrefix.trim();
        }
        
        return afterPrefix.substring(0, suffixIndex).trim();
    }

    /**
     * 预检查配置应用是否可行
     */
    public PreCheckResult preCheckChanges(ApplyChangesRequest request) {
        String templateId = request.getTemplateId();
        Map<String, String> values = request.getValues();
        
        if (templateId == null || templateId.isEmpty()) {
            throw new RuntimeException("模板ID不能为空");
        }
        
        if (values == null || values.isEmpty()) {
            throw new RuntimeException("没有需要更改的配置项");
        }
        
        // 获取模板
        Template template = templateService.getTemplateById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateId);
        }
        
        logger.info("开始预检查配置应用，模板: {}, 配置项数: {}", template.getName(), values.size());
        
        PreCheckResult result = new PreCheckResult();
        
        // 遍历需要更改的配置项
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String itemName = entry.getKey();
            String newValue = entry.getValue();
            
            // 在模板中查找对应的配置项
            ConfigItem configItem = findConfigItemByName(template, itemName);
            if (configItem == null) {
                logger.warn("配置项不存在: {}", itemName);
                continue;
            }
            
            // 检查所有目标点
            if (configItem.getTargets() != null) {
                for (FileTarget target : configItem.getTargets()) {
                    PreCheckIssue issue = checkFileTarget(itemName, target);
                    if (issue != null) {
                        result.addIssue(issue);
                    } else {
                        result.addValidTarget();
                    }
                }
            }
        }
        
        result.setTotalTargets(result.getValidCount() + result.getInvalidCount());
        logger.info("预检查完成：总计 {} 个目标，{} 个有效，{} 个有问题", 
                result.getTotalTargets(), result.getValidCount(), result.getInvalidCount());
        
        return result;
    }

    /**
     * 应用配置更改（支持部分更新）
     */
    public ApplyResult applyChanges(ApplyChangesRequest request) {
        String templateId = request.getTemplateId();
        Map<String, String> values = request.getValues();
        
        if (templateId == null || templateId.isEmpty()) {
            throw new RuntimeException("模板ID不能为空");
        }
        
        if (values == null || values.isEmpty()) {
            throw new RuntimeException("没有需要更改的配置项");
        }
        
        // 获取模板
        Template template = templateService.getTemplateById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateId);
        }
        
        logger.info("开始应用配置更改，模板: {}, 配置项数: {}", template.getName(), values.size());
        
        ApplyResult result = new ApplyResult();
        
        // 遍历需要更改的配置项
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String itemName = entry.getKey();
            String newValue = entry.getValue();
            
            // 在模板中查找对应的配置项
            ConfigItem configItem = findConfigItemByName(template, itemName);
            if (configItem == null) {
                logger.warn("配置项不存在: {}", itemName);
                continue;
            }
            
            // 应用到所有目标点
            if (configItem.getTargets() != null) {
                for (FileTarget target : configItem.getTargets()) {
                    try {
                        fileProcessor.applyChange(target, newValue);
                        TargetResult targetResult = TargetResult.success(itemName, target.getFilePath(), 
                                target.getLineNumber(), newValue);
                        result.addResult(targetResult);
                        logger.debug("成功更新配置项 {} 到文件 {} 第 {} 行", 
                                itemName, target.getFilePath(), target.getLineNumber());
                    } catch (Exception e) {
                        TargetResult targetResult = TargetResult.failure(itemName, target.getFilePath(), 
                                target.getLineNumber(), newValue, e.getMessage());
                        result.addResult(targetResult);
                        logger.error("更新配置项失败: {} -> {}", itemName, target.getFilePath(), e);
                    }
                }
            }
        }
        
        result.setTotalTargets(result.getSuccessCount() + result.getFailureCount());
        logger.info("配置更改应用完成：总计 {} 个目标，{} 个成功，{} 个失败", 
                result.getTotalTargets(), result.getSuccessCount(), result.getFailureCount());
        
        return result;
    }

    /**
     * 检查文件目标点是否有效
     */
    private PreCheckIssue checkFileTarget(String configItemName, FileTarget target) {
        try {
            String filePath = fileProcessor.cleanFilePath(target.getFilePath());
            File file = new File(filePath);
            
            // 检查文件是否存在
            if (!file.exists()) {
                return PreCheckIssue.fileNotFound(configItemName, filePath);
            }
            
            // 检查文件是否可读
            if (!file.canRead()) {
                return PreCheckIssue.fileNotReadable(configItemName, filePath, "没有读取权限");
            }
            
            // 检查行号和前缀
            try {
                java.nio.charset.Charset encoding = fileProcessor.detectFileEncoding(file);
                java.util.List<String> lines = org.apache.commons.io.FileUtils.readLines(file, encoding);
                
                // 检查行号是否有效
                if (target.getLineNumber() < 1 || target.getLineNumber() > lines.size()) {
                    return PreCheckIssue.invalidLineNumber(configItemName, filePath, target.getLineNumber());
                    }
                
                // 检查前缀是否存在
                String line = lines.get(target.getLineNumber() - 1);
                String prefix = target.getPrefix();
                if (prefix != null && !prefix.isEmpty() && !line.contains(prefix)) {
                    return PreCheckIssue.prefixNotFound(configItemName, filePath, target.getLineNumber(), prefix);
                }
                
                // 检查通过
                return null;
                
            } catch (Exception e) {
                return PreCheckIssue.fileNotReadable(configItemName, filePath, e.getMessage());
            }
            
        } catch (Exception e) {
            return PreCheckIssue.fileNotReadable(configItemName, target.getFilePath(), e.getMessage());
        }
    }

    /**
     * 根据名称查找配置项
     */
    private ConfigItem findConfigItemByName(Template template, String itemName) {
        if (template.getItems() == null) {
            return null;
        }
        
        return template.getItems().stream()
                .filter(item -> itemName.equals(item.getName()))
                .findFirst()
                .orElse(null);
    }
} 