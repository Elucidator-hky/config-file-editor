package com.configtool.model;

import java.util.Map;

/**
 * 应用配置更改请求模型
 */
public class ApplyChangesRequest {
    private String templateId;
    private Map<String, String> values; // 配置项名称 -> 新值

    // 构造函数
    public ApplyChangesRequest() {}

    public ApplyChangesRequest(String templateId, Map<String, String> values) {
        this.templateId = templateId;
        this.values = values;
    }

    // Getter和Setter方法
    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "ApplyChangesRequest{" +
                "templateId='" + templateId + '\'' +
                ", values=" + values +
                '}';
    }
} 