package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 服务器配置请求DTO
 */
@Data
public class ServerConfigRequest {

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
     * 密码（当认证类型为password时使用）
     */
    private String password;

    /**
     * 私钥内容（当认证类型为publickey时使用）
     */
    private String privateKey;

    /**
     * 私钥密码（如果私钥有密码保护）
     */
    private String privateKeyPassword;

    /**
     * 是否为默认服务器
     */
    private Boolean isDefault;

    /**
     * 服务器描述
     */
    private String description;
}
