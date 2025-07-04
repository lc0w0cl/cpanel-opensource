package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 音乐搜索请求DTO
 */
@Data
public class MusicSearchRequestDTO {
    
    /**
     * 搜索关键词或URL
     */
    private String query;
    
    /**
     * 搜索类型：keyword（关键词）或 url（直链）
     */
    private String searchType = "keyword";
    
    /**
     * 平台：bilibili, youtube, both
     */
    private String platform = "both";
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
