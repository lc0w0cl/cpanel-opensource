package com.clover.cpanel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务器配置实体类
 * 对应数据库表：panel_servers
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("panel_servers")
public class Server {

    /**
     * 服务器ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 服务器名称
     */
    @TableField("server_name")
    private String serverName;

    /**
     * 服务器主机地址
     */
    @TableField("host")
    private String host;

    /**
     * 服务器端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 认证类型：password（密码认证）或 publickey（公钥认证）
     */
    @TableField("auth_type")
    private String authType;

    /**
     * 密码（当认证类型为password时使用）
     */
    @TableField("password")
    private String password;

    /**
     * 私钥内容（当认证类型为publickey时使用）
     */
    @TableField("private_key")
    private String privateKey;

    /**
     * 私钥密码（如果私钥有密码保护）
     */
    @TableField("private_key_password")
    private String privateKeyPassword;

    /**
     * 服务器描述
     */
    @TableField("description")
    private String description;

    /**
     * 服务器图标（Iconify图标名称）
     */
    @TableField("icon")
    private String icon;

    /**
     * 是否为默认服务器
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 服务器状态：active（活跃）、inactive（非活跃）
     */
    @TableField("status")
    private String status;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间，格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private String createdAt;

    /**
     * 更新时间，格式：yyyy-MM-dd HH:mm:ss
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private String updatedAt;
}
