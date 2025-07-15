package com.clover.cpanel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分类实体类（支持导航分类和服务器分组）
 * 对应数据库表：panel_categories
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("panel_categories")
public class PanelCategory {

    /**
     * 分类ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 分类名称
     */
    @TableField("name")
    private String name;

    /**
     * 分类类型：navigation(导航分类)、server(服务器分组)
     */
    @TableField("type")
    private String type;

    /**
     * 分类排序序号
     */
    @TableField("`order`")
    private Integer order;

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
