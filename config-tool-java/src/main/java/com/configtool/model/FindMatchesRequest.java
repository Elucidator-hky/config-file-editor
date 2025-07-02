package com.configtool.model;

/**
 * 查找匹配项请求模型
 */
public class FindMatchesRequest {
    private String filePath;
    private String prefix;
    private String suffix;

    // 构造函数
    public FindMatchesRequest() {}

    public FindMatchesRequest(String filePath, String prefix, String suffix) {
        this.filePath = filePath;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    // Getter和Setter方法
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
        return "FindMatchesRequest{" +
                "filePath='" + filePath + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                '}';
    }
} 