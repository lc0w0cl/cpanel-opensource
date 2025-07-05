package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 歌单解析请求DTO
 */
@Data
public class PlaylistParseRequestDTO {
    
    /**
     * 歌单URL
     */
    private String url;
    
    /**
     * 平台类型：qq, netease, auto（自动识别）
     */
    private String platform = "auto";
}
