package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.dto.MusicSearchRequestDTO;
import com.clover.cpanel.dto.MusicSearchResultDTO;
import com.clover.cpanel.service.MusicSearchService;
import com.clover.cpanel.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicSearchService musicSearchService;
    private final SystemConfigService systemConfigService;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 搜索音乐
     */
    @PostMapping("/search")
    public ApiResponse<List<MusicSearchResultDTO>> searchMusic(@RequestBody MusicSearchRequestDTO request) {
        try {
            log.info("收到音乐搜索请求: {}", request);

            // 参数验证
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ApiResponse.error("搜索关键词不能为空");
            }

            // 执行搜索
            List<MusicSearchResultDTO> results = musicSearchService.searchMusic(request);

            log.info("搜索完成，返回 {} 个结果", results.size());
            return ApiResponse.success(results);

        } catch (Exception e) {
            log.error("搜索音乐时发生错误", e);
            return ApiResponse.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取视频详情（用于直链下载）
     */
    @GetMapping("/video/{platform}/{videoId}")
    public ApiResponse<MusicSearchResultDTO> getVideoDetail(
            @PathVariable String platform,
            @PathVariable String videoId) {
        try {
            log.info("获取视频详情: platform={}, videoId={}", platform, videoId);

            // TODO: 实现获取视频详情的逻辑
            return ApiResponse.error("功能开发中");

        } catch (Exception e) {
            log.error("获取视频详情时发生错误", e);
            return ApiResponse.error("获取视频详情失败: " + e.getMessage());
        }
    }

    /**
     * 测试搜索功能（用于调试）
     */
    @GetMapping("/test-search")
    public ApiResponse<List<MusicSearchResultDTO>> testSearch(
            @RequestParam(defaultValue = "稻香") String query) {
        try {
            log.info("测试搜索: {}", query);

            MusicSearchRequestDTO request = new MusicSearchRequestDTO();
            request.setQuery(query);
            request.setSearchType("keyword");
            request.setPlatform("bilibili");
            request.setPage(1);
            request.setPageSize(5);

            List<MusicSearchResultDTO> results = musicSearchService.searchMusic(request);

            log.info("测试搜索完成，返回 {} 个结果", results.size());
            return ApiResponse.success(results);

        } catch (Exception e) {
            log.error("测试搜索时发生错误", e);
            return ApiResponse.error("测试搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取音频流URL（用于在线播放）
     */
    @GetMapping("/stream/{platform}/{videoId}")
    public ApiResponse<String> getAudioStream(
            @PathVariable String platform,
            @PathVariable String videoId) {
        try {
            log.info("获取音频流: platform={}, videoId={}", platform, videoId);

            String audioUrl = musicSearchService.getAudioStreamUrl(platform, videoId);

            if (audioUrl != null && !audioUrl.isEmpty()) {
                return ApiResponse.success(audioUrl);
            } else {
                return ApiResponse.error("无法获取音频流");
            }

        } catch (Exception e) {
            log.error("获取音频流时发生错误", e);
            return ApiResponse.error("获取音频流失败: " + e.getMessage());
        }
    }

    /**
     * 通过URL获取音频流
     */
    @PostMapping("/stream-url")
    public ApiResponse<String> getAudioStreamByUrl(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            log.info("通过URL获取音频流: {}", url);

            if (url == null || url.trim().isEmpty()) {
                return ApiResponse.error("URL不能为空");
            }

            String audioUrl = musicSearchService.getAudioStreamUrlByUrl(url);

            if (audioUrl != null && !audioUrl.isEmpty()) {
                return ApiResponse.success(audioUrl);
            } else {
                return ApiResponse.error("无法获取音频流");
            }

        } catch (Exception e) {
            log.error("通过URL获取音频流时发生错误", e);
            return ApiResponse.error("获取音频流失败: " + e.getMessage());
        }
    }

    /**
     * 音频流式代理播放接口 - 解决403错误问题，支持Range请求
     */
    @GetMapping("/proxy/audio-stream")
    public ResponseEntity<byte[]> proxyAudioStream(
            @RequestParam String url,
            @RequestHeader(value = "Range", required = false) String range) {
        try {
            log.debug("代理音频流请求: {}, Range: {}", url, range);

            // 验证URL是否有效
            if (url == null || url.trim().isEmpty()) {
                log.warn("音频URL为空");
                return ResponseEntity.badRequest().build();
            }

            // 创建连接
            URL audioUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) audioUrl.openConnection();

            // 设置请求头，模拟浏览器请求
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            // 根据URL来源设置不同的Referer
            if (url.contains("bilibili.com") || url.contains("bilivideo.cn") || url.contains("bili")) {
                connection.setRequestProperty("Referer", "https://www.bilibili.com/");
            } else if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
                connection.setRequestProperty("Referer", "https://www.youtube.com/");
            }

            connection.setRequestProperty("Accept", "audio/webm,audio/ogg,audio/wav,audio/*;q=0.9,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Accept-Encoding", "identity");

            // 如果客户端发送了Range请求，转发给源服务器
            if (range != null && !range.isEmpty()) {
                connection.setRequestProperty("Range", range);
            }

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != 206) { // 206 Partial Content
                log.warn("音频流请求失败，状态码: {}, URL: {}", responseCode, url);
                return ResponseEntity.status(responseCode).build();
            }

            // 获取内容类型
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                // 根据URL推断音频类型
                if (url.contains(".mp3")) {
                    contentType = "audio/mpeg";
                } else if (url.contains(".m4a")) {
                    contentType = "audio/mp4";
                } else if (url.contains(".webm")) {
                    contentType = "audio/webm";
                } else {
                    contentType = "audio/mpeg"; // 默认类型
                }
            }

            // 获取内容长度和范围信息
            long contentLength = connection.getContentLengthLong();
            String contentRange = connection.getHeaderField("Content-Range");
            String acceptRanges = connection.getHeaderField("Accept-Ranges");

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));

            if (acceptRanges != null) {
                headers.set("Accept-Ranges", acceptRanges);
            } else {
                headers.set("Accept-Ranges", "bytes");
            }

            if (contentRange != null) {
                headers.set("Content-Range", contentRange);
            }

            if (contentLength > 0) {
                headers.setContentLength(contentLength);
            }

            // 读取音频数据
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] audioData = inputStream.readAllBytes();

                log.debug("成功代理音频流，大小: {} bytes, 类型: {}, 状态码: {}",
                    audioData.length, contentType, responseCode);

                // 根据原始响应状态码返回
                HttpStatus status = (responseCode == 206) ? // 206 Partial Content
                    HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;

                return new ResponseEntity<>(audioData, headers, status);
            }

        } catch (IOException e) {
            log.error("代理音频流时发生IO错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("代理音频流时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 音频代理播放接口 - 解决403错误问题
     */
    @GetMapping("/proxy/audio")
    public ResponseEntity<byte[]> proxyAudio(@RequestParam String url) {
        try {
            log.debug("代理音频请求: {}", url);

            // 验证URL是否有效
            if (url == null || url.trim().isEmpty()) {
                log.warn("音频URL为空");
                return ResponseEntity.badRequest().build();
            }

            // 创建连接
            URL audioUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) audioUrl.openConnection();

            // 设置请求头，模拟浏览器请求
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            // 根据URL来源设置不同的Referer
            if (url.contains("bilibili.com") || url.contains("bilivideo.cn")) {
                connection.setRequestProperty("Referer", "https://www.bilibili.com/");
            } else if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
                connection.setRequestProperty("Referer", "https://www.youtube.com/");
            }

            connection.setRequestProperty("Accept", "audio/webm,audio/ogg,audio/wav,audio/*;q=0.9,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("Range", "bytes=0-");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != 206) { // 206 Partial Content
                log.warn("音频请求失败，状态码: {}, URL: {}", responseCode, url);
                return ResponseEntity.status(responseCode).build();
            }

            // 获取内容类型
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                // 根据URL推断音频类型
                if (url.contains(".mp3")) {
                    contentType = "audio/mpeg";
                } else if (url.contains(".m4a")) {
                    contentType = "audio/mp4";
                } else if (url.contains(".webm")) {
                    contentType = "audio/webm";
                } else {
                    contentType = "audio/mpeg"; // 默认类型
                }
            }

            // 获取内容长度
            long contentLength = connection.getContentLengthLong();

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.set("Accept-Ranges", "bytes");

            if (contentLength > 0) {
                headers.setContentLength(contentLength);
            }

            // 读取音频数据
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] audioData = inputStream.readAllBytes();

                log.debug("成功代理音频，大小: {} bytes, 类型: {}", audioData.length, contentType);

                return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
            }

        } catch (IOException e) {
            log.error("代理音频时发生IO错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("代理音频时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    /**
     * 图片代理接口 - 解决防盗链问题
     */
    @GetMapping("/proxy/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            log.debug("代理图片请求: {}", url);

            // 验证URL是否为哔哩哔哩的图片
            if (!isValidImageUrl(url)) {
                log.warn("无效的图片URL: {}", url);
                return ResponseEntity.badRequest().build();
            }

            // 创建连接
            URL imageUrl = new URL(url.startsWith("//") ? "https:" + url : url);
            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();

            // 设置请求头，模拟来自哔哩哔哩的请求
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Referer", "https://www.bilibili.com/");
            connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("图片请求失败，状态码: {}, URL: {}", responseCode, url);
                return ResponseEntity.status(responseCode).build();
            }

            // 获取内容类型
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg"; // 默认类型
            }

            // 读取图片数据
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] imageData = inputStream.readAllBytes();

                // 设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setCacheControl("public, max-age=3600"); // 缓存1小时
                headers.set("Access-Control-Allow-Origin", "*");

                log.debug("成功代理图片，大小: {} bytes, 类型: {}", imageData.length, contentType);

                return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
            }

        } catch (IOException e) {
            log.error("代理图片时发生IO错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("代理图片时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 验证图片URL是否有效
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 移除协议前缀进行检查
        String cleanUrl = url.startsWith("//") ? url.substring(2) : url;
        if (cleanUrl.startsWith("https://")) {
            cleanUrl = cleanUrl.substring(8);
        } else if (cleanUrl.startsWith("http://")) {
            cleanUrl = cleanUrl.substring(7);
        }

        // 检查是否为哔哩哔哩的图片域名
        return cleanUrl.startsWith("i0.hdslb.com/") ||
               cleanUrl.startsWith("i1.hdslb.com/") ||
               cleanUrl.startsWith("i2.hdslb.com/") ||
               cleanUrl.startsWith("s1.hdslb.com/") ||
               cleanUrl.startsWith("s2.hdslb.com/");
    }

    /**
     * 下载音乐到服务器
     */
    @PostMapping("/download-to-server")
    public ApiResponse<Map<String, String>> downloadMusicToServer(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            String title = request.get("title");
            String artist = request.get("artist");
            String platform = request.get("platform");

            log.info("开始服务器下载音乐: title={}, artist={}, url={}", title, artist, url);

            // 参数验证
            if (url == null || url.trim().isEmpty()) {
                return ApiResponse.error("视频URL不能为空");
            }
            if (title == null || title.trim().isEmpty()) {
                return ApiResponse.error("音乐标题不能为空");
            }

            // 获取音乐下载设置
            String serverDownloadPath = systemConfigService.getConfigValue("music_server_download_path");
            if (serverDownloadPath == null || serverDownloadPath.trim().isEmpty()) {
                serverDownloadPath = "uploads/music";
            }

            // 1. 获取音频流URL
            String audioUrl = musicSearchService.getAudioStreamUrlByUrl(url);
            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                return ApiResponse.error("无法获取音频流URL");
            }

            // 2. 生成文件名
            String fileName = generateServerFileName(title, artist, audioUrl);

            // 3. 创建下载目录
            Path downloadDir = Paths.get(serverDownloadPath);
            if (!Files.exists(downloadDir)) {
                Files.createDirectories(downloadDir);
                log.info("创建下载目录: {}", downloadDir.toAbsolutePath());
            }

            // 4. 下载文件到服务器
            Path filePath = downloadDir.resolve(fileName);
            boolean success = downloadAudioToServer(audioUrl, filePath);

            if (success) {
                // 构建访问URL
                String accessUrl = buildFileAccessUrl(serverDownloadPath, fileName);

                Map<String, String> result = new HashMap<>();
                result.put("fileName", fileName);
                result.put("filePath", filePath.toString());
                result.put("accessUrl", accessUrl);

                log.info("服务器下载成功: {}", fileName);
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("下载到服务器失败");
            }

        } catch (Exception e) {
            log.error("服务器下载音乐时发生错误", e);
            return ApiResponse.error("服务器下载失败: " + e.getMessage());
        }
    }

    /**
     * 下载音频文件到服务器
     */
    private boolean downloadAudioToServer(String audioUrl, Path filePath) {
        try {
            log.info("开始下载音频文件: {} -> {}", audioUrl, filePath);

            // 创建连接
            URL url = new URL(audioUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求头
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            // 根据URL设置Referer
            if (audioUrl.contains("bilivideo.cn") || audioUrl.contains("bilibili.com")) {
                connection.setRequestProperty("Referer", "https://www.bilibili.com/");
            } else if (audioUrl.contains("youtube.com") || audioUrl.contains("googlevideo.com")) {
                connection.setRequestProperty("Referer", "https://www.youtube.com/");
            }

            connection.setRequestProperty("Accept", "audio/webm,audio/ogg,audio/wav,audio/*;q=0.9,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("音频下载失败，状态码: {}, URL: {}", responseCode, audioUrl);
                return false;
            }

            // 下载文件
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("音频文件下载完成: {}, 大小: {} bytes", filePath, Files.size(filePath));
                return true;
            }

        } catch (Exception e) {
            log.error("下载音频文件到服务器失败: {}", audioUrl, e);
            return false;
        }
    }

    /**
     * 生成服务器文件名
     */
    private String generateServerFileName(String title, String artist, String audioUrl) {
        // 清理文件名中的非法字符
        String cleanTitle = title.replaceAll("[<>:\"/\\\\|?*]", "").trim();
        String cleanArtist = artist != null ? artist.replaceAll("[<>:\"/\\\\|?*]", "").trim() : "";

        // 根据URL推断文件扩展名
        String extension = ".mp3"; // 默认扩展名
        if (audioUrl.contains(".m4a")) {
            extension = ".m4a";
        } else if (audioUrl.contains(".webm")) {
            extension = ".webm";
        } else if (audioUrl.contains(".ogg")) {
            extension = ".ogg";
        }

        // 生成文件名：艺术家 - 歌曲名.扩展名
        if (cleanArtist != null && !cleanArtist.isEmpty() && !cleanArtist.equals(cleanTitle)) {
            return cleanArtist + " - " + cleanTitle + extension;
        } else {
            return cleanTitle + extension;
        }
    }

    /**
     * 构建文件访问URL
     */
    private String buildFileAccessUrl(String serverDownloadPath, String fileName) {
        // 构建相对于contextPath的URL
        String relativePath = serverDownloadPath + "/" + fileName;

        // 确保路径以/开头
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }

        // 添加contextPath
        if (contextPath != null && !contextPath.isEmpty()) {
            return contextPath + relativePath;
        } else {
            return relativePath;
        }
    }
}
