package com.clover.cpanel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体类
 * 对应数据库表：system_config
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("panel_system_config")
public class SystemConfig {

    /**
     * 配置ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 配置键名
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 配置描述
     */
    @TableField("description")
    private String description;

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
