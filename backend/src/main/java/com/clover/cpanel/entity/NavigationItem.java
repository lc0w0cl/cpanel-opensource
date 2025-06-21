package com.clover.cpanel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 导航项实体类
 * 对应数据库表：panel_navigation_items
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("panel_navigation_items")
public class NavigationItem {

    /**
     * 导航项ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 导航项名称
     */
    @TableField("name")
    private String name;

    /**
     * 导航项外部URL
     */
    @TableField("url")
    private String url;

    /**
     * 导航项图标路径
     */
    @TableField("logo")
    private String logo;

    /**
     * 所属分类ID，外键关联panel_categories表
     */
    @TableField("category_id")
    private Integer categoryId;

    /**
     * 导航项描述
     */
    @TableField("description")
    private String description;

    /**
     * 导航项内部URL
     */
    @TableField("internal_url")
    private String internalUrl;

    /**
     * 排序序号
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
