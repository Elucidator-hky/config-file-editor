package com.configtool.model;

import java.util.List;
import java.util.Map;

/**
 * 包含状态信息的模板
 */
public class TemplateWithStatus {
    /** 基础模板信息 */
    private Template template;
    
    /** 配置项状态映射 (配置项名称 -> 状态) */
    private Map<String, ConfigItemStatus> itemStatuses;

    public TemplateWithStatus() {
    }

    public TemplateWithStatus(Template template, Map<String, ConfigItemStatus> itemStatuses) {
        this.template = template;
        this.itemStatuses = itemStatuses;
    }

    // Getters and setters
    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public Map<String, ConfigItemStatus> getItemStatuses() {
        return itemStatuses;
    }

    public void setItemStatuses(Map<String, ConfigItemStatus> itemStatuses) {
        this.itemStatuses = itemStatuses;
    }

    /**
     * 配置项状态信息
     */
    public static class ConfigItemStatus {
        /** 状态类型：OK(正常), WARNING(警告), ERROR(错误) */
        private String status;
        
        /** 状态描述 */
        private String message;
        
        /** 当前值（如果能读取到） */
        private String currentValue;
        
        /** 文件是否存在 */
        private boolean fileExists;
        
        /** 行号是否有效 */
        private boolean lineValid;
        
        /** 目标点详细状态列表 */
        private List<TargetStatus> targetStatuses;

        public ConfigItemStatus() {
        }

        public ConfigItemStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public static ConfigItemStatus ok(String currentValue) {
            ConfigItemStatus status = new ConfigItemStatus("OK", "配置正常");
            status.currentValue = currentValue;
            status.fileExists = true;
            status.lineValid = true;
            return status;
        }

        public static ConfigItemStatus warning(String message, String currentValue) {
            ConfigItemStatus status = new ConfigItemStatus("WARNING", message);
            status.currentValue = currentValue;
            status.fileExists = true;
            status.lineValid = true;
            return status;
        }

        public static ConfigItemStatus error(String message) {
            ConfigItemStatus status = new ConfigItemStatus("ERROR", message);
            status.fileExists = false;
            status.lineValid = false;
            return status;
        }

        // Getters and setters
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }

        public boolean isFileExists() {
            return fileExists;
        }

        public void setFileExists(boolean fileExists) {
            this.fileExists = fileExists;
        }

        public boolean isLineValid() {
            return lineValid;
        }

        public void setLineValid(boolean lineValid) {
            this.lineValid = lineValid;
        }

        public List<TargetStatus> getTargetStatuses() {
            return targetStatuses;
        }

        public void setTargetStatuses(List<TargetStatus> targetStatuses) {
            this.targetStatuses = targetStatuses;
        }
    }

    /**
     * 目标点状态信息
     */
    public static class TargetStatus {
        /** 目标点ID */
        private String targetId;
        
        /** 文件路径 */
        private String filePath;
        
        /** 行号 */
        private int lineNumber;
        
        /** 前缀 */
        private String prefix;
        
        /** 后缀 */
        private String suffix;
        
        /** 状态：SUCCESS/ERROR */
        private String status;
        
        /** 状态消息 */
        private String message;
        
        /** 当前值 */
        private String currentValue;

        public TargetStatus() {
        }

        public TargetStatus(String targetId, String filePath, int lineNumber, String prefix, String suffix, 
                           String status, String message, String currentValue) {
            this.targetId = targetId;
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.prefix = prefix;
            this.suffix = suffix;
            this.status = status;
            this.message = message;
            this.currentValue = currentValue;
        }

        // Getters and setters
        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }
    }
}