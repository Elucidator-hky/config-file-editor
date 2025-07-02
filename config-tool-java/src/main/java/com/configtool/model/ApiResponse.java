package com.configtool.model;

/**
 * API响应封装类
 * 统一的API响应格式
 */
public class ApiResponse<T> {
    private boolean success;
    private String error;
    private T data;

    // 构造函数
    private ApiResponse() {}

    private ApiResponse(boolean success, String error, T data) {
        this.success = success;
        this.error = error;
        this.data = data;
    }

    // 静态工厂方法
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, error, null);
    }

    // Getter方法
    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", error='" + error + '\'' +
                ", data=" + data +
                '}';
    }
} 