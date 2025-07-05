package com.clover.cpanel.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 歌单歌曲DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaylistSongDTO {
    
    /**
     * 歌曲标题
     */
    private String title;
    
    /**
     * 艺术家
     */
    private String artist;
    
    /**
     * 专辑名称
     */
    private String album;
    
    /**
     * 歌曲URL
     */
    private String url;
    
    /**
     * 封面图片URL
     */
    private String cover;
    
    /**
     * 来源平台
     */
    private String source;
    
    /**
     * 来源平台的歌曲ID
     */
    private String sourceId;
    
    /**
     * 时长
     */
    private String duration;
}
