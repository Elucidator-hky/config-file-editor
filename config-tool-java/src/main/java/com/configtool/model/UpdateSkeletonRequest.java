package com.configtool.model;

import java.util.List;

/**
 * 更新skeleton文件请求模型
 */
public class UpdateSkeletonRequest {
    private String templateId;
    private List<UpdateItem> updates;

    // 构造函数
    public UpdateSkeletonRequest() {}

    public UpdateSkeletonRequest(String templateId, List<UpdateItem> updates) {
        this.templateId = templateId;
        this.updates = updates;
    }

    // Getter和Setter方法
    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public List<UpdateItem> getUpdates() {
        return updates;
    }

    public void setUpdates(List<UpdateItem> updates) {
        this.updates = updates;
    }

    /**
     * 更新项内部类
     */
    public static class UpdateItem {
        private String itemName;
        private String defaultValue;
        private String currentValue;

        // 构造函数
        public UpdateItem() {}

        public UpdateItem(String itemName, String defaultValue, String currentValue) {
            this.itemName = itemName;
            this.defaultValue = defaultValue;
            this.currentValue = currentValue;
        }

        // Getter和Setter方法
        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }

        @Override
        public String toString() {
            return "UpdateItem{" +
                    "itemName='" + itemName + '\'' +
                    ", defaultValue='" + defaultValue + '\'' +
                    ", currentValue='" + currentValue + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "UpdateSkeletonRequest{" +
                "templateId='" + templateId + '\'' +
                ", updates=" + updates +
                '}';
    }
} 