package com.clover.cpanel.dto;

import lombok.Data;

/**
 * 壁纸配置请求DTO
 */
@Data
public class WallpaperConfigRequest {

    /**
     * 壁纸URL（可选，如果不提供则保持当前设置）
     */
    private String wallpaperUrl;

    /**
     * 壁纸模糊度（0-20）
     */
    private Integer wallpaperBlur = 5;

    /**
     * 壁纸遮罩透明度（0-80）
     */
    private Integer wallpaperMask = 30;
}
