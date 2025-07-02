package com.configtool.model;

/**
 * 文件目标点模型
 * 表示配置项在文件中的具体位置和匹配规则
 */
public class FileTarget {
    private String id;
    private String filePath;
    private int lineNumber;
    private String prefix;
    private String suffix;

    // 构造函数
    public FileTarget() {}

    public FileTarget(String id, String filePath, int lineNumber, String prefix, String suffix) {
        this.id = id;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "FileTarget{" +
                "id='" + id + '\'' +
                ", filePath='" + filePath + '\'' +
                ", lineNumber=" + lineNumber +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                '}';
    }
} 