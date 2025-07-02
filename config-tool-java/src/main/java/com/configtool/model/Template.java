package com.configtool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板模型
 * 表示配置模板，包含基本信息和配置项列表
 */
public class Template {
    private String id;
    private String name;
    private String description;
    private List<ConfigItem> items;

    // 构造函数
    public Template() {
        this.items = new ArrayList<>();
    }

    public Template(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.items = new ArrayList<>();
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

    public List<ConfigItem> getItems() {
        return items;
    }

    public void setItems(List<ConfigItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    // 便利方法
    public void addItem(ConfigItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public void removeItem(ConfigItem item) {
        if (this.items != null) {
            this.items.remove(item);
        }
    }

    public ConfigItem findItemById(String itemId) {
        if (this.items == null || itemId == null) {
            return null;
        }
        return this.items.stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "Template{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", items=" + (items != null ? items.size() : 0) + " items" +
                '}';
    }
} 