package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.constant.ConfigType;
import com.clover.cpanel.dto.MarginConfigRequest;
import com.clover.cpanel.dto.WallpaperConfigRequest;
import com.clover.cpanel.entity.SystemConfig;
import com.clover.cpanel.service.FileUploadService;
import com.clover.cpanel.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

    // 上传目录配置
    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    // 壁纸配置相关的键名常量
    private static final String WALLPAPER_URL_KEY = "wallpaper_url";
    private static final String WALLPAPER_BLUR_KEY = "wallpaper_blur";
    private static final String WALLPAPER_MASK_KEY = "wallpaper_mask";

    // 边距配置相关常量
    private static final String MARGIN_KEY = "content_margin";

    // 内容边距配置相关常量
    private static final String CONTENT_PADDING_KEY = "content_padding";

    // Logo配置相关常量
    private static final String LOGO_URL_KEY = "logo_url";

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

            // 上传文件到backgrounds子目录
            String wallpaperUrl = fileUploadService.uploadFileToSubDirectory(file, "backgrounds", "wallpaper");
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
     * 上传并设置Logo
     * @param file Logo文件
     * @return 操作结果
     */
    @PostMapping("/logo/upload")
    public ApiResponse<Map<String, Object>> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ApiResponse.error("请选择要上传的文件");
            }

            // 验证文件类型
            if (!fileUploadService.isValidFileType(file)) {
                return ApiResponse.error("不支持的文件类型，请选择 PNG、JPG、SVG 格式的图片");
            }

            // 验证文件大小
            if (!fileUploadService.isValidFileSize(file)) {
                return ApiResponse.error("文件大小超出限制");
            }

            // 上传文件到logos子目录
            String logoUrl = fileUploadService.uploadFileToSubDirectory(file, "logos", "logo");
            log.info("Logo文件上传成功: {}", logoUrl);

            // 保存配置到数据库
            boolean success = systemConfigService.setConfigValue(
                LOGO_URL_KEY,
                logoUrl,
                "自定义Logo图片",
                ConfigType.THEME
            );

            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("logoUrl", logoUrl);

                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("保存Logo配置失败");
            }
        } catch (Exception e) {
            log.error("上传Logo失败", e);
            return ApiResponse.error("上传Logo失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前Logo配置
     * @return Logo配置信息
     */
    @GetMapping("/logo")
    public ApiResponse<Map<String, Object>> getLogoConfig() {
        try {
            String logoUrl = systemConfigService.getConfigValue(LOGO_URL_KEY);

            Map<String, Object> result = new HashMap<>();
            result.put("logoUrl", logoUrl != null ? logoUrl : "");

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取Logo配置失败", e);
            return ApiResponse.error("获取Logo配置失败：" + e.getMessage());
        }
    }

    /**
     * 重置Logo为默认
     * @return 操作结果
     */
    @PostMapping("/logo/reset")
    public ApiResponse<Map<String, Object>> resetLogo() {
        try {
            // 删除Logo配置，恢复默认
            boolean success = systemConfigService.setConfigValue(
                LOGO_URL_KEY,
                "",
                "重置为默认Logo",
                ConfigType.THEME
            );

            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("logoUrl", "");

                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("重置Logo失败");
            }
        } catch (Exception e) {
            log.error("重置Logo失败", e);
            return ApiResponse.error("重置Logo失败：" + e.getMessage());
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

    /**
     * 保存边距设置
     * @param request 边距配置请求
     * @return 操作结果
     */
    @PostMapping("/margin")
    public ApiResponse<Map<String, Object>> saveMarginSettings(@RequestBody MarginConfigRequest request) {
        try {
            log.info("保存边距设置: 边距={}", request.getMargin());

            // 保存配置到数据库
            boolean success = saveMarginConfig(request.getMargin());

            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("margin", request.getMargin());

                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("保存边距配置失败");
            }
        } catch (Exception e) {
            log.error("保存边距设置失败", e);
            return ApiResponse.error("保存边距设置失败：" + e.getMessage());
        }
    }

    /**
     * 获取边距设置
     * @return 边距配置
     */
    @GetMapping("/margin")
    public ApiResponse<Map<String, Object>> getMarginSettings() {
        try {
            String marginStr = systemConfigService.getConfigValue(MARGIN_KEY);
            Double margin = marginStr != null ? Double.parseDouble(marginStr) : 0.0;

            Map<String, Object> result = new HashMap<>();
            result.put("margin", margin);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取边距设置失败", e);
            return ApiResponse.error("获取边距设置失败：" + e.getMessage());
        }
    }

    /**
     * 保存内容边距设置
     * @param request 内容边距配置请求
     * @return 操作结果
     */
    @PostMapping("/content-padding")
    public ApiResponse<Map<String, Object>> saveContentPaddingSettings(@RequestBody Map<String, Object> request) {
        try {
            Object paddingObj = request.get("padding");
            Double padding = 0.0;

            if (paddingObj instanceof Number) {
                padding = ((Number) paddingObj).doubleValue();
            } else if (paddingObj instanceof String) {
                padding = Double.parseDouble((String) paddingObj);
            }

            log.info("保存内容边距设置: 边距={}px", padding);

            // 保存配置到数据库
            boolean success = saveContentPaddingConfig(padding);

            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("padding", padding);

                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("保存内容边距配置失败");
            }
        } catch (Exception e) {
            log.error("保存内容边距设置失败", e);
            return ApiResponse.error("保存内容边距设置失败：" + e.getMessage());
        }
    }

    /**
     * 获取内容边距设置
     * @return 内容边距配置
     */
    @GetMapping("/content-padding")
    public ApiResponse<Map<String, Object>> getContentPaddingSettings() {
        try {
            String paddingStr = systemConfigService.getConfigValue(CONTENT_PADDING_KEY);
            Double padding = paddingStr != null ? Double.parseDouble(paddingStr) : 0.0;

            Map<String, Object> result = new HashMap<>();
            result.put("padding", padding);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取内容边距设置失败", e);
            return ApiResponse.error("获取内容边距设置失败：" + e.getMessage());
        }
    }

    /**
     * 保存内容边距配置到数据库
     * @param padding 内容边距值（px）
     * @return 是否保存成功
     */
    private boolean saveContentPaddingConfig(Double padding) {
        try {
            return systemConfigService.setConfigValue(CONTENT_PADDING_KEY,
                padding.toString(), "内容区域左右边距（像素）", ConfigType.THEME);
        } catch (Exception e) {
            log.error("保存内容边距配置失败", e);
            return false;
        }
    }

    /**
     * 保存边距配置到数据库
     * @param margin 边距值
     * @return 是否保存成功
     */
    private boolean saveMarginConfig(Double margin) {
        try {
            return systemConfigService.setConfigValue(MARGIN_KEY,
                margin.toString(), "内容左右边距", ConfigType.THEME);
        } catch (Exception e) {
            log.error("保存边距配置失败", e);
            return false;
        }
    }

    /**
     * 获取历史壁纸列表
     * @return 壁纸列表
     */
    @GetMapping("/wallpaper/history")
    public ApiResponse<List<Map<String, Object>>> getWallpaperHistory() {
        try {
            List<Map<String, Object>> wallpapers = new ArrayList<>();

            // 获取backgrounds目录
            File backgroundsDir = new File(uploadPath, "backgrounds");
            if (!backgroundsDir.exists() || !backgroundsDir.isDirectory()) {
                return ApiResponse.success(wallpapers);
            }

            // 获取当前使用的壁纸URL
            String currentWallpaperUrl = systemConfigService.getConfigValue(WALLPAPER_URL_KEY);

            // 遍历backgrounds目录中的所有图片文件
            File[] files = backgroundsDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                       lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                       lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
            });

            if (files != null) {
                for (File file : files) {
                    Map<String, Object> wallpaper = new HashMap<>();
                    String fileName = file.getName();
                    String fileUrl = "/uploads/backgrounds/" + fileName;

                    wallpaper.put("id", fileName);
                    wallpaper.put("name", fileName);
                    wallpaper.put("url", fileUrl);
                    wallpaper.put("size", file.length());
                    wallpaper.put("lastModified", file.lastModified());
                    wallpaper.put("isCurrent", fileUrl.equals(currentWallpaperUrl));

                    wallpapers.add(wallpaper);
                }

                // 按最后修改时间倒序排列
                wallpapers.sort((a, b) -> Long.compare(
                    (Long) b.get("lastModified"),
                    (Long) a.get("lastModified")
                ));
            }

            return ApiResponse.success(wallpapers);
        } catch (Exception e) {
            log.error("获取历史壁纸列表失败", e);
            return ApiResponse.error("获取历史壁纸列表失败：" + e.getMessage());
        }
    }

    /**
     * 删除指定壁纸
     * @param wallpaperId 壁纸ID（文件名）
     * @return 操作结果
     */
    @DeleteMapping("/wallpaper/{wallpaperId}")
    public ApiResponse<String> deleteWallpaper(@PathVariable String wallpaperId) {
        try {
            // 构建文件路径
            String fileUrl = "/uploads/backgrounds/" + wallpaperId;

            // 检查是否为当前使用的壁纸
            String currentWallpaperUrl = systemConfigService.getConfigValue(WALLPAPER_URL_KEY);
            if (fileUrl.equals(currentWallpaperUrl)) {
                return ApiResponse.error("无法删除当前正在使用的壁纸");
            }

            // 删除文件
            boolean deleted = fileUploadService.deleteFile(fileUrl);

            if (deleted) {
                log.info("壁纸删除成功: {}", fileUrl);
                return ApiResponse.success("壁纸删除成功");
            } else {
                return ApiResponse.error("壁纸删除失败");
            }
        } catch (Exception e) {
            log.error("删除壁纸失败", e);
            return ApiResponse.error("删除壁纸失败：" + e.getMessage());
        }
    }

    /**
     * 设置指定壁纸为当前壁纸
     * @param wallpaperId 壁纸ID（文件名）
     * @return 操作结果
     */
    @PostMapping("/wallpaper/{wallpaperId}/apply")
    public ApiResponse<Map<String, Object>> applyWallpaper(@PathVariable String wallpaperId) {
        try {
            String fileUrl = "/uploads/backgrounds/" + wallpaperId;

            // 检查文件是否存在
            File file = new File(uploadPath, "backgrounds/" + wallpaperId);
            if (!file.exists()) {
                return ApiResponse.error("壁纸文件不存在");
            }

            // 获取当前的模糊度和遮罩设置
            String blurStr = systemConfigService.getConfigValue(WALLPAPER_BLUR_KEY);
            String maskStr = systemConfigService.getConfigValue(WALLPAPER_MASK_KEY);

            int blur = blurStr != null ? Integer.parseInt(blurStr) : 5;
            int mask = maskStr != null ? Integer.parseInt(maskStr) : 30;

            // 保存壁纸配置
            boolean success = saveWallpaperConfig(fileUrl, blur, mask);

            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("wallpaperUrl", fileUrl);
                result.put("wallpaperBlur", blur);
                result.put("wallpaperMask", mask);

                log.info("壁纸应用成功: {}", fileUrl);
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("壁纸应用失败");
            }
        } catch (Exception e) {
            log.error("应用壁纸失败", e);
            return ApiResponse.error("应用壁纸失败：" + e.getMessage());
        }
    }
}
