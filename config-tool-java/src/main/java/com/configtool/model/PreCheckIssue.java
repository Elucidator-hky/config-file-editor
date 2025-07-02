package com.configtool.model;

/**
 * 预检查发现的问题
 */
public class PreCheckIssue {
    private String configItemName;
    private String filePath;
    private int lineNumber;
    private String issueType;
    private String description;
    private String suggestion;

    public PreCheckIssue() {}

    public PreCheckIssue(String configItemName, String filePath, int lineNumber, String issueType, String description) {
        this.configItemName = configItemName;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.issueType = issueType;
        this.description = description;
    }

    // 文件不存在的问题
    public static PreCheckIssue fileNotFound(String configItemName, String filePath) {
        PreCheckIssue issue = new PreCheckIssue(configItemName, filePath, 0, "FILE_NOT_FOUND", 
                String.format("文件不存在: %s", getFileName(filePath)));
        issue.setSuggestion("请检查文件路径是否正确，或创建该文件");
        return issue;
    }

    // 行号无效的问题
    public static PreCheckIssue invalidLineNumber(String configItemName, String filePath, int lineNumber) {
        PreCheckIssue issue = new PreCheckIssue(configItemName, filePath, lineNumber, "INVALID_LINE_NUMBER", 
                String.format("行号无效: %s 第%d行", getFileName(filePath), lineNumber));
        issue.setSuggestion("请检查行号是否超出文件范围");
        return issue;
    }

    // 前缀未找到的问题
    public static PreCheckIssue prefixNotFound(String configItemName, String filePath, int lineNumber, String prefix) {
        PreCheckIssue issue = new PreCheckIssue(configItemName, filePath, lineNumber, "PREFIX_NOT_FOUND", 
                String.format("前缀未找到: %s 第%d行找不到 '%s'", getFileName(filePath), lineNumber, prefix));
        issue.setSuggestion("请检查前缀是否正确，或更新目标点配置");
        return issue;
    }

    // 文件无法读取的问题
    public static PreCheckIssue fileNotReadable(String configItemName, String filePath, String reason) {
        PreCheckIssue issue = new PreCheckIssue(configItemName, filePath, 0, "FILE_NOT_READABLE", 
                String.format("文件无法读取: %s (%s)", getFileName(filePath), reason));
        issue.setSuggestion("请检查文件权限");
        return issue;
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

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return "PreCheckIssue{" +
                "configItemName='" + configItemName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", issueType='" + issueType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
} 