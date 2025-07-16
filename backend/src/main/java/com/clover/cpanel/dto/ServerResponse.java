package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 服务器配置响应DTO（不包含敏感信息）
 */
@Data
public class ServerResponse {

    /**
     * 服务器ID
     */
    private Integer id;

    /**
     * 服务器名称
     */
    private String serverName;

    /**
     * 服务器主机地址
     */
    private String host;

    /**
     * 服务器端口
     */
    private Integer port;

    /**
     * 用户名
     */
    private String username;

    /**
     * 认证类型：password（密码认证）或 publickey（公钥认证）
     */
    private String authType;

    /**
     * 服务器描述
     */
    private String description;

    /**
     * 服务器图标（Iconify图标名称）
     */
    private String icon;

    /**
     * 所属分类ID
     */
    private Integer categoryId;



    /**
     * 是否为默认服务器
     */
    private Boolean isDefault;

    /**
     * 服务器状态：active（活跃）、inactive（非活跃）
     */
    private String status;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
}
