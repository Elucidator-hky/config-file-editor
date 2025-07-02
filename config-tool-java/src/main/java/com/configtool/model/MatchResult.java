package com.configtool.model;

/**
 * 文件匹配结果模型
 * 表示在文件中找到的匹配项
 */
public class MatchResult {
    private int lineNumber;
    private String lineContent;

    // 构造函数
    public MatchResult() {}

    public MatchResult(int lineNumber, String lineContent) {
        this.lineNumber = lineNumber;
        this.lineContent = lineContent;
    }

    // Getter和Setter方法
    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLineContent() {
        return lineContent;
    }

    public void setLineContent(String lineContent) {
        this.lineContent = lineContent;
    }

    @Override
    public String toString() {
        return "MatchResult{" +
                "lineNumber=" + lineNumber +
                ", lineContent='" + lineContent + '\'' +
                '}';
    }
} 