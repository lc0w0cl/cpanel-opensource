package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 导航项创建请求DTO
 */
@Data
public class NavigationItemCreateRequest {

    /**
     * 导航项名称
     */
    private String name;

    /**
     * 导航项外部URL
     */
    private String url;

    /**
     * 所属分类ID
     */
    private Integer categoryId;

    /**
     * 导航项描述
     */
    private String description;

    /**
     * 导航项内部URL
     */
    private String internalUrl;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
