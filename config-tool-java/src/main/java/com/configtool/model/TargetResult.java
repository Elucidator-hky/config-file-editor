package com.configtool.model;

/**
 * 单个目标的操作结果
 */
public class TargetResult {
    private String configItemName;
    private String filePath;
    private int lineNumber;
    private String newValue;
    private boolean success;
    private String errorMessage;
    private String description;

    public TargetResult() {}

    public TargetResult(String configItemName, String filePath, int lineNumber, String newValue) {
        this.configItemName = configItemName;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.newValue = newValue;
    }

    // 成功的结果
    public static TargetResult success(String configItemName, String filePath, int lineNumber, String newValue) {
        TargetResult result = new TargetResult(configItemName, filePath, lineNumber, newValue);
        result.setSuccess(true);
        result.setDescription(String.format("成功更新 %s -> %s 第%d行", configItemName, getFileName(filePath), lineNumber));
        return result;
    }

    // 失败的结果
    public static TargetResult failure(String configItemName, String filePath, int lineNumber, String newValue, String errorMessage) {
        TargetResult result = new TargetResult(configItemName, filePath, lineNumber, newValue);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setDescription(String.format("更新失败 %s -> %s: %s", configItemName, getFileName(filePath), errorMessage));
        return result;
    }

    // 获取文件名（不包含路径）
    private static String getFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "未知文件";
        }
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
    }

    // Getter和Setter方法
    public String getConfigItemName() {
        return configItemName;
    }

    public void setConfigItemName(String configItemName) {
        this.configItemName = configItemName;
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

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "TargetResult{" +
                "configItemName='" + configItemName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", lineNumber=" + lineNumber +
                ", success=" + success +
                ", description='" + description + '\'' +
                '}';
    }
} 