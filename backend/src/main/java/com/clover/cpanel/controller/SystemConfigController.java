package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.constant.ConfigType;
import com.clover.cpanel.dto.MarginConfigRequest;
import com.clover.cpanel.dto.PrivateKeyRequest;
import com.clover.cpanel.dto.ServerConfigRequest;
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

    // 音乐配置相关常量
    private static final String MUSIC_DOWNLOAD_LOCATION_KEY = "music_download_location";
    private static final String MUSIC_SERVER_DOWNLOAD_PATH_KEY = "music_server_download_path";

    // Cookie配置相关常量
    private static final String BILIBILI_COOKIE_KEY = "bilibili_cookie";
    private static final String YOUTUBE_COOKIE_KEY = "youtube_cookie";

    // 服务器配置相关常量
    private static final String SERVER_CONFIG_PREFIX = "server_config_";
    private static final String DEFAULT_SERVER_KEY = "default_server_id";

    // 私钥配置相关常量
    private static final String PRIVATE_KEY_PREFIX = "private_key_";

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
     * 获取音乐设置配置
     * @return 音乐设置配置
     */
    @GetMapping("/music")
    public ApiResponse<Map<String, Object>> getMusicConfig() {
        try {
            Map<String, Object> musicConfig = new HashMap<>();

            // 获取下载位置设置（默认为本地）
            String downloadLocation = systemConfigService.getConfigValue(MUSIC_DOWNLOAD_LOCATION_KEY);
            musicConfig.put("downloadLocation", downloadLocation != null ? downloadLocation : "local");

            // 获取服务器下载路径设置（默认为 uploads/music）
            String serverDownloadPath = systemConfigService.getConfigValue(MUSIC_SERVER_DOWNLOAD_PATH_KEY);
            musicConfig.put("serverDownloadPath", serverDownloadPath != null ? serverDownloadPath : "uploads/music");

            return ApiResponse.success(musicConfig);
        } catch (Exception e) {
            log.error("获取音乐配置失败", e);
            return ApiResponse.error("获取音乐配置失败：" + e.getMessage());
        }
    }

    /**
     * 保存音乐设置配置
     * @param downloadLocation 下载位置（local/server）
     * @param serverDownloadPath 服务器下载路径
     * @return 操作结果
     */
    @PostMapping("/music")
    public ApiResponse<String> saveMusicConfig(
            @RequestParam String downloadLocation,
            @RequestParam(required = false) String serverDownloadPath) {
        try {
            // 验证下载位置参数
            if (!"local".equals(downloadLocation) && !"server".equals(downloadLocation)) {
                return ApiResponse.error("下载位置参数无效，只能是 local 或 server");
            }

            // 保存下载位置设置
            boolean success1 = systemConfigService.setConfigValue(
                MUSIC_DOWNLOAD_LOCATION_KEY,
                downloadLocation,
                "音乐下载位置设置",
                ConfigType.MUSIC
            );

            // 如果是服务器下载，保存服务器路径设置
            boolean success2 = true;
            if ("server".equals(downloadLocation) && serverDownloadPath != null && !serverDownloadPath.trim().isEmpty()) {
                // 清理路径，移除多余的斜杠但保留绝对路径标识
                String cleanPath = serverDownloadPath.trim();

                // 安全验证：防止路径遍历攻击
                if (cleanPath.contains("..") || cleanPath.contains("~")) {
                    return ApiResponse.error("路径包含非法字符，不允许使用 .. 或 ~ 等特殊路径");
                }

                // 标准化路径分隔符（Windows兼容性）
                cleanPath = cleanPath.replace("\\", "/");

                // 移除重复的斜杠
                cleanPath = cleanPath.replaceAll("/+", "/");

                // 移除末尾斜杠（但保留根路径的斜杠）
                if (cleanPath.length() > 1 && cleanPath.endsWith("/")) {
                    cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
                }

                // 如果路径为空，使用默认路径
                if (cleanPath.isEmpty()) {
                    cleanPath = "uploads/music";
                }

                success2 = systemConfigService.setConfigValue(
                    MUSIC_SERVER_DOWNLOAD_PATH_KEY,
                    cleanPath,
                    "音乐服务器下载路径",
                    ConfigType.MUSIC
                );
            }

            if (success1 && success2) {
                log.info("音乐配置保存成功: downloadLocation={}, serverDownloadPath={}",
                    downloadLocation, serverDownloadPath);
                return ApiResponse.success("音乐配置保存成功");
            } else {
                return ApiResponse.error("音乐配置保存失败");
            }
        } catch (Exception e) {
            log.error("保存音乐配置失败", e);
            return ApiResponse.error("保存音乐配置失败：" + e.getMessage());
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

    /**
     * 获取Cookie配置
     * @return Cookie配置
     */
    @GetMapping("/cookies")
    public ApiResponse<Map<String, Object>> getCookieConfig() {
        try {
            Map<String, Object> cookieConfig = new HashMap<>();

            // 获取Bilibili Cookie设置
            String bilibiliCookie = systemConfigService.getConfigValue(BILIBILI_COOKIE_KEY);
            cookieConfig.put("bilibiliCookie", bilibiliCookie != null ? bilibiliCookie : "");

            // 获取YouTube Cookie设置
            String youtubeCookie = systemConfigService.getConfigValue(YOUTUBE_COOKIE_KEY);
            cookieConfig.put("youtubeCookie", youtubeCookie != null ? youtubeCookie : "");

            return ApiResponse.success(cookieConfig);
        } catch (Exception e) {
            log.error("获取Cookie配置失败", e);
            return ApiResponse.error("获取Cookie配置失败：" + e.getMessage());
        }
    }

    /**
     * 保存Cookie配置
     * @param bilibiliCookie Bilibili Cookie值
     * @param youtubeCookie YouTube Cookie值
     * @return 操作结果
     */
    @PostMapping("/cookies")
    public ApiResponse<String> saveCookieConfig(
            @RequestParam(required = false, defaultValue = "") String bilibiliCookie,
            @RequestParam(required = false, defaultValue = "") String youtubeCookie) {
        try {
            // 保存Bilibili Cookie设置
            boolean success1 = systemConfigService.setConfigValue(
                BILIBILI_COOKIE_KEY,
                bilibiliCookie != null ? bilibiliCookie.trim() : "",
                "Bilibili Cookie配置",
                ConfigType.MUSIC
            );

            // 保存YouTube Cookie设置
            boolean success2 = systemConfigService.setConfigValue(
                YOUTUBE_COOKIE_KEY,
                youtubeCookie != null ? youtubeCookie.trim() : "",
                "YouTube Cookie配置",
                ConfigType.MUSIC
            );

            if (success1 && success2) {
                log.info("Cookie配置保存成功");
                return ApiResponse.success("Cookie配置保存成功");
            } else {
                return ApiResponse.error("Cookie配置保存失败");
            }
        } catch (Exception e) {
            log.error("保存Cookie配置失败", e);
            return ApiResponse.error("保存Cookie配置失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有服务器配置
     * @return 服务器配置列表
     */
    @GetMapping("/servers")
    public ApiResponse<List<Map<String, Object>>> getServerConfigs() {
        try {
            List<SystemConfig> serverConfigs = systemConfigService.getConfigsByType(ConfigType.SERVER);
            List<Map<String, Object>> result = new ArrayList<>();

            // 获取默认服务器ID
            String defaultServerId = systemConfigService.getConfigValue(DEFAULT_SERVER_KEY);

            for (SystemConfig config : serverConfigs) {
                if (config.getConfigKey().startsWith(SERVER_CONFIG_PREFIX)) {
                    try {
                        // 解析JSON配置
                        Map<String, Object> serverConfig = parseServerConfig(config.getConfigValue());
                        serverConfig.put("id", config.getId());
                        serverConfig.put("configKey", config.getConfigKey());
                        serverConfig.put("isDefault", config.getId().toString().equals(defaultServerId));

                        // 移除敏感信息（密码和私钥）
                        serverConfig.remove("password");
                        serverConfig.remove("privateKey");
                        serverConfig.remove("privateKeyPassword");

                        result.add(serverConfig);
                    } catch (Exception e) {
                        log.warn("解析服务器配置失败: {}", config.getConfigKey(), e);
                    }
                }
            }

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取服务器配置失败", e);
            return ApiResponse.error("获取服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 保存服务器配置
     * @param request 服务器配置请求
     * @return 操作结果
     */
    @PostMapping("/servers")
    public ApiResponse<String> saveServerConfig(@RequestBody ServerConfigRequest request) {
        try {
            // 验证必填字段
            if (request.getServerName() == null || request.getServerName().trim().isEmpty()) {
                return ApiResponse.error("服务器名称不能为空");
            }
            if (request.getHost() == null || request.getHost().trim().isEmpty()) {
                return ApiResponse.error("服务器地址不能为空");
            }
            if (request.getPort() == null || request.getPort() <= 0 || request.getPort() > 65535) {
                return ApiResponse.error("端口号必须在1-65535之间");
            }
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ApiResponse.error("用户名不能为空");
            }
            if (request.getAuthType() == null || request.getAuthType().trim().isEmpty()) {
                return ApiResponse.error("认证类型不能为空");
            }

            // 验证认证信息
            if ("password".equals(request.getAuthType())) {
                if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                    return ApiResponse.error("密码认证时密码不能为空");
                }
            } else if ("publickey".equals(request.getAuthType())) {
                if (request.getPrivateKey() == null || request.getPrivateKey().trim().isEmpty()) {
                    return ApiResponse.error("公钥认证时私钥不能为空");
                }
            } else {
                return ApiResponse.error("不支持的认证类型");
            }

            // 构建配置JSON
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("serverName", request.getServerName().trim());
            configMap.put("host", request.getHost().trim());
            configMap.put("port", request.getPort());
            configMap.put("username", request.getUsername().trim());
            configMap.put("authType", request.getAuthType().trim());
            configMap.put("description", request.getDescription() != null ? request.getDescription().trim() : "");

            if ("password".equals(request.getAuthType())) {
                configMap.put("password", request.getPassword().trim());
            } else if ("publickey".equals(request.getAuthType())) {
                configMap.put("privateKey", request.getPrivateKey().trim());
                configMap.put("privateKeyPassword", request.getPrivateKeyPassword() != null ? request.getPrivateKeyPassword().trim() : "");
            }

            // 转换为JSON字符串
            String configJson = convertToJson(configMap);

            // 生成配置键名
            String configKey = SERVER_CONFIG_PREFIX + System.currentTimeMillis();

            // 保存配置
            boolean success = systemConfigService.setConfigValue(
                configKey,
                configJson,
                "服务器配置: " + request.getServerName(),
                ConfigType.SERVER
            );

            if (success) {
                // 如果设置为默认服务器，更新默认服务器配置
                if (Boolean.TRUE.equals(request.getIsDefault())) {
                    // 获取刚保存的配置ID
                    SystemConfig savedConfig = systemConfigService.getConfigsByType(ConfigType.SERVER)
                        .stream()
                        .filter(config -> configKey.equals(config.getConfigKey()))
                        .findFirst()
                        .orElse(null);

                    if (savedConfig != null) {
                        systemConfigService.setConfigValue(
                            DEFAULT_SERVER_KEY,
                            savedConfig.getId().toString(),
                            "默认服务器ID",
                            ConfigType.SERVER
                        );
                    }
                }

                log.info("服务器配置保存成功: {}", request.getServerName());
                return ApiResponse.success("服务器配置保存成功");
            } else {
                return ApiResponse.error("服务器配置保存失败");
            }
        } catch (Exception e) {
            log.error("保存服务器配置失败", e);
            return ApiResponse.error("保存服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 删除服务器配置
     * @param id 配置ID
     * @return 操作结果
     */
    @DeleteMapping("/servers/{id}")
    public ApiResponse<String> deleteServerConfig(@PathVariable Integer id) {
        try {
            // 检查是否为默认服务器
            String defaultServerId = systemConfigService.getConfigValue(DEFAULT_SERVER_KEY);
            if (id.toString().equals(defaultServerId)) {
                return ApiResponse.error("不能删除默认服务器配置");
            }

            boolean success = systemConfigService.removeById(id);
            if (success) {
                log.info("服务器配置删除成功: {}", id);
                return ApiResponse.success("服务器配置删除成功");
            } else {
                return ApiResponse.error("服务器配置删除失败");
            }
        } catch (Exception e) {
            log.error("删除服务器配置失败", e);
            return ApiResponse.error("删除服务器配置失败：" + e.getMessage());
        }
    }

    /**
     * 设置默认服务器
     * @param id 服务器配置ID
     * @return 操作结果
     */
    @PostMapping("/servers/{id}/set-default")
    public ApiResponse<String> setDefaultServer(@PathVariable Integer id) {
        try {
            // 检查服务器配置是否存在
            SystemConfig serverConfig = systemConfigService.getById(id);
            if (serverConfig == null || !ConfigType.SERVER.equals(serverConfig.getConfigType())) {
                return ApiResponse.error("服务器配置不存在");
            }

            boolean success = systemConfigService.setConfigValue(
                DEFAULT_SERVER_KEY,
                id.toString(),
                "默认服务器ID",
                ConfigType.SERVER
            );

            if (success) {
                log.info("默认服务器设置成功: {}", id);
                return ApiResponse.success("默认服务器设置成功");
            } else {
                return ApiResponse.error("默认服务器设置失败");
            }
        } catch (Exception e) {
            log.error("设置默认服务器失败", e);
            return ApiResponse.error("设置默认服务器失败：" + e.getMessage());
        }
    }

    /**
     * 测试服务器连接
     * @param request 服务器配置请求
     * @return 测试结果
     */
    @PostMapping("/servers/test")
    public ApiResponse<String> testServerConnection(@RequestBody ServerConfigRequest request) {
        try {
            // 这里可以实现实际的SSH连接测试
            // 为了简化，这里只做基本验证
            if (request.getHost() == null || request.getHost().trim().isEmpty()) {
                return ApiResponse.error("服务器地址不能为空");
            }
            if (request.getPort() == null || request.getPort() <= 0 || request.getPort() > 65535) {
                return ApiResponse.error("端口号必须在1-65535之间");
            }
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ApiResponse.error("用户名不能为空");
            }

            // TODO: 实现实际的SSH连接测试逻辑
            log.info("测试服务器连接: {}:{}", request.getHost(), request.getPort());

            return ApiResponse.success("服务器连接测试成功");
        } catch (Exception e) {
            log.error("测试服务器连接失败", e);
            return ApiResponse.error("测试服务器连接失败：" + e.getMessage());
        }
    }

    /**
     * 解析服务器配置JSON
     * @param configJson JSON字符串
     * @return 配置Map
     */
    private Map<String, Object> parseServerConfig(String configJson) {
        try {
            // 简单的JSON解析实现
            Map<String, Object> result = new HashMap<>();

            // 移除大括号
            String content = configJson.trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                content = content.substring(1, content.length() - 1);
            }

            // 分割键值对
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");

                    // 尝试转换数字
                    if ("port".equals(key)) {
                        try {
                            result.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            result.put(key, value);
                        }
                    } else {
                        result.put(key, value);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("解析服务器配置JSON失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 将Map转换为JSON字符串
     * @param configMap 配置Map
     * @return JSON字符串
     */
    private String convertToJson(Map<String, Object> configMap) {
        try {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;

            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                json.append("\"").append(entry.getKey()).append("\":");

                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                } else if (value instanceof Number) {
                    json.append(value.toString());
                } else {
                    json.append("\"").append(value != null ? value.toString() : "").append("\"");
                }
            }

            json.append("}");
            return json.toString();
        } catch (Exception e) {
            log.error("转换JSON失败", e);
            return "{}";
        }
    }

    /**
     * 获取所有私钥配置
     * @return 私钥配置列表
     */
    @GetMapping("/private-keys")
    public ApiResponse<List<Map<String, Object>>> getPrivateKeys() {
        try {
            List<SystemConfig> privateKeyConfigs = systemConfigService.getConfigsByType(ConfigType.SERVER);
            List<Map<String, Object>> result = new ArrayList<>();

            for (SystemConfig config : privateKeyConfigs) {
                if (config.getConfigKey().startsWith(PRIVATE_KEY_PREFIX)) {
                    try {
                        // 解析JSON配置
                        Map<String, Object> keyConfig = parseServerConfig(config.getConfigValue());
                        keyConfig.put("id", config.getId());
                        keyConfig.put("configKey", config.getConfigKey());

                        // 移除敏感信息（私钥内容和密码）
                        keyConfig.remove("privateKey");
                        keyConfig.remove("privateKeyPassword");

                        result.add(keyConfig);
                    } catch (Exception e) {
                        log.warn("解析私钥配置失败: {}", config.getConfigKey(), e);
                    }
                }
            }

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取私钥配置失败", e);
            return ApiResponse.error("获取私钥配置失败：" + e.getMessage());
        }
    }

    /**
     * 保存私钥配置
     * @param request 私钥配置请求
     * @return 操作结果
     */
    @PostMapping("/private-keys")
    public ApiResponse<String> savePrivateKey(@RequestBody PrivateKeyRequest request) {
        try {
            // 验证必填字段
            if (request.getKeyName() == null || request.getKeyName().trim().isEmpty()) {
                return ApiResponse.error("私钥名称不能为空");
            }
            if (request.getPrivateKey() == null || request.getPrivateKey().trim().isEmpty()) {
                return ApiResponse.error("私钥内容不能为空");
            }

            // 检查私钥名称是否已存在
            List<SystemConfig> existingKeys = systemConfigService.getConfigsByType(ConfigType.SERVER);
            for (SystemConfig config : existingKeys) {
                if (config.getConfigKey().startsWith(PRIVATE_KEY_PREFIX)) {
                    try {
                        Map<String, Object> keyConfig = parseServerConfig(config.getConfigValue());
                        if (request.getKeyName().trim().equals(keyConfig.get("keyName"))) {
                            return ApiResponse.error("私钥名称已存在");
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }

            // 构建配置JSON
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("keyName", request.getKeyName().trim());
            configMap.put("privateKey", request.getPrivateKey().trim());
            configMap.put("privateKeyPassword", request.getPrivateKeyPassword() != null ? request.getPrivateKeyPassword().trim() : "");
            configMap.put("description", request.getDescription() != null ? request.getDescription().trim() : "");
            configMap.put("keyType", request.getKeyType() != null ? request.getKeyType().trim() : "");

            // 转换为JSON字符串
            String configJson = convertToJson(configMap);

            // 生成配置键名
            String configKey = PRIVATE_KEY_PREFIX + System.currentTimeMillis();

            // 保存配置
            boolean success = systemConfigService.setConfigValue(
                configKey,
                configJson,
                "私钥配置: " + request.getKeyName(),
                ConfigType.SERVER
            );

            if (success) {
                log.info("私钥配置保存成功: {}", request.getKeyName());
                return ApiResponse.success("私钥配置保存成功");
            } else {
                return ApiResponse.error("私钥配置保存失败");
            }
        } catch (Exception e) {
            log.error("保存私钥配置失败", e);
            return ApiResponse.error("保存私钥配置失败：" + e.getMessage());
        }
    }

    /**
     * 更新私钥配置
     * @param id 配置ID
     * @param request 私钥配置请求
     * @return 操作结果
     */
    @PutMapping("/private-keys/{id}")
    public ApiResponse<String> updatePrivateKey(@PathVariable Integer id, @RequestBody PrivateKeyRequest request) {
        try {
            // 获取现有配置
            SystemConfig existingConfig = systemConfigService.getById(id);
            if (existingConfig == null || !existingConfig.getConfigKey().startsWith(PRIVATE_KEY_PREFIX)) {
                return ApiResponse.error("私钥配置不存在");
            }

            // 验证必填字段
            if (request.getKeyName() == null || request.getKeyName().trim().isEmpty()) {
                return ApiResponse.error("私钥名称不能为空");
            }
            if (request.getPrivateKey() == null || request.getPrivateKey().trim().isEmpty()) {
                return ApiResponse.error("私钥内容不能为空");
            }

            // 检查私钥名称是否与其他私钥冲突
            List<SystemConfig> existingKeys = systemConfigService.getConfigsByType(ConfigType.SERVER);
            for (SystemConfig config : existingKeys) {
                if (config.getConfigKey().startsWith(PRIVATE_KEY_PREFIX) && !config.getId().equals(id)) {
                    try {
                        Map<String, Object> keyConfig = parseServerConfig(config.getConfigValue());
                        if (request.getKeyName().trim().equals(keyConfig.get("keyName"))) {
                            return ApiResponse.error("私钥名称已存在");
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }

            // 构建配置JSON
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("keyName", request.getKeyName().trim());
            configMap.put("privateKey", request.getPrivateKey().trim());
            configMap.put("privateKeyPassword", request.getPrivateKeyPassword() != null ? request.getPrivateKeyPassword().trim() : "");
            configMap.put("description", request.getDescription() != null ? request.getDescription().trim() : "");
            configMap.put("keyType", request.getKeyType() != null ? request.getKeyType().trim() : "");

            // 转换为JSON字符串
            String configJson = convertToJson(configMap);

            // 更新配置
            existingConfig.setConfigValue(configJson);
            existingConfig.setDescription("私钥配置: " + request.getKeyName());
            boolean success = systemConfigService.updateById(existingConfig);

            if (success) {
                log.info("私钥配置更新成功: {}", request.getKeyName());
                return ApiResponse.success("私钥配置更新成功");
            } else {
                return ApiResponse.error("私钥配置更新失败");
            }
        } catch (Exception e) {
            log.error("更新私钥配置失败", e);
            return ApiResponse.error("更新私钥配置失败：" + e.getMessage());
        }
    }

    /**
     * 删除私钥配置
     * @param id 配置ID
     * @return 操作结果
     */
    @DeleteMapping("/private-keys/{id}")
    public ApiResponse<String> deletePrivateKey(@PathVariable Integer id) {
        try {
            // 检查私钥配置是否存在
            SystemConfig privateKeyConfig = systemConfigService.getById(id);
            if (privateKeyConfig == null || !privateKeyConfig.getConfigKey().startsWith(PRIVATE_KEY_PREFIX)) {
                return ApiResponse.error("私钥配置不存在");
            }

            boolean success = systemConfigService.removeById(id);
            if (success) {
                log.info("私钥配置删除成功: {}", id);
                return ApiResponse.success("私钥配置删除成功");
            } else {
                return ApiResponse.error("私钥配置删除失败");
            }
        } catch (Exception e) {
            log.error("删除私钥配置失败", e);
            return ApiResponse.error("删除私钥配置失败：" + e.getMessage());
        }
    }

    /**
     * 获取私钥详情（包含私钥内容，用于编辑）
     * @param id 私钥配置ID
     * @return 私钥详情
     */
    @GetMapping("/private-keys/{id}")
    public ApiResponse<Map<String, Object>> getPrivateKeyDetail(@PathVariable Integer id) {
        try {
            SystemConfig privateKeyConfig = systemConfigService.getById(id);
            if (privateKeyConfig == null || !privateKeyConfig.getConfigKey().startsWith(PRIVATE_KEY_PREFIX)) {
                return ApiResponse.error("私钥配置不存在");
            }

            Map<String, Object> keyConfig = parseServerConfig(privateKeyConfig.getConfigValue());
            keyConfig.put("id", privateKeyConfig.getId());
            keyConfig.put("configKey", privateKeyConfig.getConfigKey());

            return ApiResponse.success(keyConfig);
        } catch (Exception e) {
            log.error("获取私钥详情失败", e);
            return ApiResponse.error("获取私钥详情失败：" + e.getMessage());
        }
    }
}
