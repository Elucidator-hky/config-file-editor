package com.configtool.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON工具类
 * 提供JSON序列化和反序列化功能
 */
public class JsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            logger.error("对象转JSON失败: {}", obj, e);
            throw new RuntimeException("JSON序列化失败: " + e.getMessage());
        }
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            logger.error("JSON转对象失败: {}", json, e);
            throw new RuntimeException("JSON反序列化失败: " + e.getMessage());
        }
    }

    /**
     * 获取Gson实例
     */
    public static Gson getGson() {
        return gson;
    }
} 