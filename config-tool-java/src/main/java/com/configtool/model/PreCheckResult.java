package com.configtool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 预检查结果模型
 * 用于在应用配置前检查是否存在问题
 */
public class PreCheckResult {
    private boolean allValid;
    private int totalTargets;
    private int validCount;
    private int invalidCount;
    private List<PreCheckIssue> issues;
    private String summary;

    public PreCheckResult() {
        this.issues = new ArrayList<>();
    }

    public PreCheckResult(int totalTargets) {
        this.totalTargets = totalTargets;
        this.issues = new ArrayList<>();
    }

    // Getter和Setter方法
    public boolean isAllValid() {
        return allValid;
    }

    public void setAllValid(boolean allValid) {
        this.allValid = allValid;
    }

    public int getTotalTargets() {
        return totalTargets;
    }

    public void setTotalTargets(int totalTargets) {
        this.totalTargets = totalTargets;
    }

    public int getValidCount() {
        return validCount;
    }

    public void setValidCount(int validCount) {
        this.validCount = validCount;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public void setInvalidCount(int invalidCount) {
        this.invalidCount = invalidCount;
    }

    public List<PreCheckIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<PreCheckIssue> issues) {
        this.issues = issues;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    // 便利方法
    public void addIssue(PreCheckIssue issue) {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);
        this.invalidCount++;
        updateSummary();
    }

    public void addValidTarget() {
        this.validCount++;
        updateSummary();
    }

    private void updateSummary() {
        this.allValid = (this.invalidCount == 0 && this.validCount > 0);
        
        if (this.allValid) {
            this.summary = String.format("检查通过！共 %d 个目标文件都可正常访问", this.validCount);
        } else if (this.validCount > 0) {
            this.summary = String.format("部分问题：%d 个正常，%d 个有问题", this.validCount, this.invalidCount);
        } else {
            this.summary = String.format("检查失败：共 %d 个目标文件都有问题", this.invalidCount);
        }
    }

    @Override
    public String toString() {
        return "PreCheckResult{" +
                "allValid=" + allValid +
                ", totalTargets=" + totalTargets +
                ", validCount=" + validCount +
                ", invalidCount=" + invalidCount +
                ", summary='" + summary + '\'' +
                '}';
    }
} 