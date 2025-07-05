package com.clover.cpanel.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 歌单信息DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaylistInfoDTO {
    
    /**
     * 歌单标题
     */
    private String title;
    
    /**
     * 创建者
     */
    private String creator;
    
    /**
     * 歌单封面URL
     */
    private String cover;
    
    /**
     * 歌单原始URL
     */
    private String url;
    
    /**
     * 来源平台
     */
    private String source;
    
    /**
     * 歌曲列表
     */
    private List<PlaylistSongDTO> songs;
    
    /**
     * 歌曲数量
     */
    private Integer songCount;
    
    /**
     * 歌单描述
     */
    private String description;
}
