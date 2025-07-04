package com.clover.cpanel.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 音乐搜索结果DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MusicSearchResultDTO {
    
    /**
     * 视频/音乐ID
     */
    private String id;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 作者/UP主
     */
    private String artist;
    
    /**
     * 时长
     */
    private String duration;
    
    /**
     * 平台（bilibili, youtube等）
     */
    private String platform;
    
    /**
     * 缩略图URL
     */
    private String thumbnail;
    
    /**
     * 视频URL
     */
    private String url;
    
    /**
     * 画质/音质
     */
    private String quality;
    
    /**
     * 播放量
     */
    private String playCount;
    
    /**
     * 发布时间
     */
    private String publishTime;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 标签
     */
    private List<String> tags;
}
