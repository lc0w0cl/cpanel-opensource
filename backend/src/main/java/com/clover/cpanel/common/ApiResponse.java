package com.clover.cpanel.common;

import lombok.Data;

/**
 * 统一API响应结果封装类
 * @param <T> 响应数据类型
 */
@Data
public class ApiResponse<T> {
    
    /**
     * 响应状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 请求是否成功
     */
    private Boolean success;
    
    /**
     * 私有构造函数
     */
    private ApiResponse() {}
    
    /**
     * 私有构造函数
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     * @param success 是否成功
     */
    private ApiResponse(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }
    
    /**
     * 成功响应（无数据）
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null, true);
    }
    
    /**
     * 成功响应（带数据）
     * @param data 响应数据
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data, true);
    }
    
    /**
     * 成功响应（自定义消息和数据）
     * @param message 响应消息
     * @param data 响应数据
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, true);
    }
    
    /**
     * 失败响应（默认消息）
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(500, "操作失败", null, false);
    }
    
    /**
     * 失败响应（自定义消息）
     * @param message 错误消息
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, false);
    }
    
    /**
     * 失败响应（自定义状态码和消息）
     * @param code 状态码
     * @param message 错误消息
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null, false);
    }

    /**
     * 失败响应（自定义消息和数据）
     * @param message 错误消息
     * @param data 响应数据
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(500, message, data, false);
    }
}
