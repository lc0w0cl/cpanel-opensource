package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.constant.ConfigType;
import com.clover.cpanel.dto.WallpaperConfigRequest;
import com.clover.cpanel.entity.SystemConfig;
import com.clover.cpanel.service.FileUploadService;
import com.clover.cpanel.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system-config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private FileUploadService fileUploadService;

    // 壁纸配置相关的键名常量
    private static final String WALLPAPER_URL_KEY = "wallpaper_url";
    private static final String WALLPAPER_BLUR_KEY = "wallpaper_blur";
    private static final String WALLPAPER_MASK_KEY = "wallpaper_mask";

    /**
     * 获取壁纸配置
     * @return 壁纸配置信息
     */
    @GetMapping("/wallpaper")
    public ApiResponse<Map<String, Object>> getWallpaperConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            String wallpaperUrl = systemConfigService.getConfigValue(WALLPAPER_URL_KEY);
            String wallpaperBlur = systemConfigService.getConfigValue(WALLPAPER_BLUR_KEY);
            String wallpaperMask = systemConfigService.getConfigValue(WALLPAPER_MASK_KEY);
            
            config.put("wallpaperUrl", wallpaperUrl != null ? wallpaperUrl : "");
            config.put("wallpaperBlur", wallpaperBlur != null ? Integer.parseInt(wallpaperBlur) : 5);
            config.put("wallpaperMask", wallpaperMask != null ? Integer.parseInt(wallpaperMask) : 30);
            
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("获取壁纸配置失败", e);
            return ApiResponse.error("获取壁纸配置失败：" + e.getMessage());
        }
    }

    /**
     * 上传并设置壁纸
     * @param file 壁纸文件
     * @param blur 模糊度
     * @param mask 遮罩透明度
     * @return 操作结果
     */
    @PostMapping("/wallpaper/upload")
    public ApiResponse<Map<String, Object>> uploadWallpaper(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "blur", defaultValue = "5") Integer blur,
            @RequestParam(value = "mask", defaultValue = "30") Integer mask) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ApiResponse.error("请选择要上传的文件");
            }

            // 验证文件类型
            if (!fileUploadService.isValidFileType(file)) {
                return ApiResponse.error("不支持的文件类型，请选择 JPG、PNG 格式的图片");
            }

            // 验证文件大小
            if (!fileUploadService.isValidFileSize(file)) {
                return ApiResponse.error("文件大小超出限制");
            }

            // 上传文件
            String wallpaperUrl = fileUploadService.uploadFile(file, "wallpaper");
            log.info("壁纸文件上传成功: {}", wallpaperUrl);

            // 保存配置到数据库
            boolean success = saveWallpaperConfig(wallpaperUrl, blur, mask);
            
            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("wallpaperUrl", wallpaperUrl);
                result.put("wallpaperBlur", blur);
                result.put("wallpaperMask", mask);
                
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("保存壁纸配置失败");
            }
        } catch (Exception e) {
            log.error("上传壁纸失败", e);
            return ApiResponse.error("上传壁纸失败：" + e.getMessage());
        }
    }

    /**
     * 更新壁纸设置（不上传新文件）
     * @param request 壁纸配置请求
     * @return 操作结果
     */
    @PostMapping("/wallpaper/settings")
    public ApiResponse<Map<String, Object>> updateWallpaperSettings(@RequestBody WallpaperConfigRequest request) {
        try {
            // 获取当前壁纸URL（如果没有提供新的URL）
            String wallpaperUrl = request.getWallpaperUrl();
            if (wallpaperUrl == null) {
                wallpaperUrl = systemConfigService.getConfigValue(WALLPAPER_URL_KEY);
            }

            // 保存配置到数据库
            boolean success = saveWallpaperConfig(wallpaperUrl, request.getWallpaperBlur(), request.getWallpaperMask());
            
            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("wallpaperUrl", wallpaperUrl != null ? wallpaperUrl : "");
                result.put("wallpaperBlur", request.getWallpaperBlur());
                result.put("wallpaperMask", request.getWallpaperMask());
                
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("保存壁纸配置失败");
            }
        } catch (Exception e) {
            log.error("更新壁纸设置失败", e);
            return ApiResponse.error("更新壁纸设置失败：" + e.getMessage());
        }
    }

    /**
     * 重置壁纸为默认设置
     * @return 操作结果
     */
    @PostMapping("/wallpaper/reset")
    public ApiResponse<Map<String, Object>> resetWallpaper() {
        try {
            // 重置为默认设置
            boolean success = saveWallpaperConfig("", 5, 30);
            
            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("wallpaperUrl", "");
                result.put("wallpaperBlur", 5);
                result.put("wallpaperMask", 30);
                
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("重置壁纸配置失败");
            }
        } catch (Exception e) {
            log.error("重置壁纸失败", e);
            return ApiResponse.error("重置壁纸失败：" + e.getMessage());
        }
    }

    /**
     * 保存壁纸配置到数据库
     * @param wallpaperUrl 壁纸URL
     * @param blur 模糊度
     * @param mask 遮罩透明度
     * @return 是否保存成功
     */
    private boolean saveWallpaperConfig(String wallpaperUrl, Integer blur, Integer mask) {
        try {
            boolean success1 = systemConfigService.setConfigValue(WALLPAPER_URL_KEY,
                wallpaperUrl != null ? wallpaperUrl : "", "自定义壁纸URL", ConfigType.THEME);
            boolean success2 = systemConfigService.setConfigValue(WALLPAPER_BLUR_KEY,
                blur.toString(), "壁纸模糊度", ConfigType.THEME);
            boolean success3 = systemConfigService.setConfigValue(WALLPAPER_MASK_KEY,
                mask.toString(), "壁纸遮罩透明度", ConfigType.THEME);

            return success1 && success2 && success3;
        } catch (Exception e) {
            log.error("保存壁纸配置失败", e);
            return false;
        }
    }

    /**
     * 获取指定配置项的值
     * @param key 配置键名
     * @return 配置值
     */
    @GetMapping("/config/{key}")
    public ApiResponse<String> getConfigValue(@PathVariable String key) {
        try {
            String value = systemConfigService.getConfigValue(key);
            return ApiResponse.success(value != null ? value : "");
        } catch (Exception e) {
            log.error("获取配置值失败", e);
            return ApiResponse.error("获取配置值失败：" + e.getMessage());
        }
    }

    /**
     * 设置指定配置项的值
     * @param key 配置键名
     * @param value 配置值
     * @param description 配置描述
     * @param type 配置类型
     * @return 操作结果
     */
    @PostMapping("/config/{key}")
    public ApiResponse<String> setConfigValue(
            @PathVariable String key,
            @RequestParam String value,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "system") String type) {
        try {
            boolean success = systemConfigService.setConfigValue(key, value, description, type);
            if (success) {
                return ApiResponse.success("配置保存成功");
            } else {
                return ApiResponse.error("配置保存失败");
            }
        } catch (Exception e) {
            log.error("设置配置值失败", e);
            return ApiResponse.error("设置配置值失败：" + e.getMessage());
        }
    }

    /**
     * 根据配置类型获取配置列表
     * @param type 配置类型
     * @return 配置列表
     */
    @GetMapping("/configs/type/{type}")
    public ApiResponse<List<SystemConfig>> getConfigsByType(@PathVariable String type) {
        try {
            List<SystemConfig> configs = systemConfigService.getConfigsByType(type);
            return ApiResponse.success(configs);
        } catch (Exception e) {
            log.error("获取配置列表失败", e);
            return ApiResponse.error("获取配置列表失败：" + e.getMessage());
        }
    }
}
