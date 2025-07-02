package com.configtool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置应用结果模型
 * 包含详细的成功和失败信息
 */
public class ApplyResult {
    private boolean allSuccess;
    private int totalTargets;
    private int successCount;
    private int failureCount;
    private List<TargetResult> results;
    private String summary;

    public ApplyResult() {
        this.results = new ArrayList<>();
    }

    public ApplyResult(int totalTargets) {
        this.totalTargets = totalTargets;
        this.results = new ArrayList<>();
    }

    // Getter和Setter方法
    public boolean isAllSuccess() {
        return allSuccess;
    }

    public void setAllSuccess(boolean allSuccess) {
        this.allSuccess = allSuccess;
    }

    public int getTotalTargets() {
        return totalTargets;
    }

    public void setTotalTargets(int totalTargets) {
        this.totalTargets = totalTargets;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<TargetResult> getResults() {
        return results;
    }

    public void setResults(List<TargetResult> results) {
        this.results = results;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    // 便利方法
    public void addResult(TargetResult result) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(result);
        
        if (result.isSuccess()) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
        
        updateSummary();
    }

    private void updateSummary() {
        this.allSuccess = (this.failureCount == 0 && this.successCount > 0);
        
        if (this.allSuccess) {
            this.summary = String.format("全部成功！共更新了 %d 个位置", this.successCount);
        } else if (this.successCount > 0) {
            this.summary = String.format("部分成功：%d 个成功，%d 个失败", this.successCount, this.failureCount);
        } else {
            this.summary = String.format("全部失败：共 %d 个位置更新失败", this.failureCount);
        }
    }

    /**
     * 获取失败的结果列表
     */
    public List<TargetResult> getFailureResults() {
        List<TargetResult> failures = new ArrayList<>();
        if (this.results != null) {
            for (TargetResult result : this.results) {
                if (!result.isSuccess()) {
                    failures.add(result);
                }
            }
        }
        return failures;
    }

    @Override
    public String toString() {
        return "ApplyResult{" +
                "allSuccess=" + allSuccess +
                ", totalTargets=" + totalTargets +
                ", successCount=" + successCount +
                ", failureCount=" + failureCount +
                ", summary='" + summary + '\'' +
                '}';
    }
} 