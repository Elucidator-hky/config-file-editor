package com.configtool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置项模型
 * 表示模板中的一个配置项，包含名称、描述、默认值和目标点列表
 */
public class ConfigItem {
    private String id;
    private String name;
    private String description;
    private String defaultValue;
    private List<FileTarget> targets;
    private String currentValue; // 用于显示当前文件中的值
    private String status; // 配置项状态：SUCCESS（全部成功）、WARNING（部分成功）、ERROR（有错误）

    // 构造函数
    public ConfigItem() {
        this.targets = new ArrayList<>();
    }

    public ConfigItem(String id, String name, String description, String defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.targets = new ArrayList<>();
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<FileTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<FileTarget> targets) {
        this.targets = targets != null ? targets : new ArrayList<>();
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // 便利方法
    public void addTarget(FileTarget target) {
        if (this.targets == null) {
            this.targets = new ArrayList<>();
        }
        this.targets.add(target);
    }

    public void removeTarget(FileTarget target) {
        if (this.targets != null) {
            this.targets.remove(target);
        }
    }

    @Override
    public String toString() {
        return "ConfigItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", targets=" + (targets != null ? targets.size() : 0) + " items" +
                ", currentValue='" + currentValue + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
} 