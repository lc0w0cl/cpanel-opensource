package com.clover.cpanel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2FA认证配置实体类
 * 对应数据库表：panel_two_factor_auth
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("panel_two_factor_auth")
public class TwoFactorAuth {

    /**
     * 配置ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户标识（对应JWT中的subject，目前固定为"admin"）
     */
    @TableField("user_id")
    private String userId;

    /**
     * 2FA密钥（Base32编码）
     */
    @TableField("secret_key")
    private String secretKey;

    /**
     * 是否启用2FA
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 备用恢复码（JSON数组格式）
     */
    @TableField("backup_codes")
    private String backupCodes;

    /**
     * 最后使用的验证码（防止重复使用）
     */
    @TableField("last_used_code")
    private String lastUsedCode;

    /**
     * 最后使用验证码的时间戳
     */
    @TableField("last_used_time")
    private Long lastUsedTime;

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
