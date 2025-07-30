package com.clover.cpanel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TODO任务实体类
 * 对应数据库表：panel_todos
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("panel_todos")
public class Todo {

    /**
     * 任务ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务内容
     */
    @TableField("text")
    private String text;

    /**
     * 是否已完成
     */
    @TableField("completed")
    private Boolean completed;

    /**
     * 所属分组ID，外键关联panel_categories表（type=todo）
     */
    @TableField("category_id")
    private Integer categoryId;

    /**
     * 排序序号
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 开始日期，格式：yyyy-MM-dd
     */
    @TableField("start_date")
    private String startDate;

    /**
     * 结束日期，格式：yyyy-MM-dd
     */
    @TableField("end_date")
    private String endDate;

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
