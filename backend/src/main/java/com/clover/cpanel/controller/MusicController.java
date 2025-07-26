package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.dto.MusicSearchRequestDTO;
import com.clover.cpanel.dto.MusicSearchResultDTO;
import com.clover.cpanel.dto.PlaylistParseRequestDTO;
import com.clover.cpanel.dto.PlaylistInfoDTO;
import com.clover.cpanel.service.MusicSearchService;
import com.clover.cpanel.service.SystemConfigService;
import com.clover.cpanel.service.PlaylistParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.ContentDisposition;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicSearchService musicSearchService;
    private final SystemConfigService systemConfigService;
    private final PlaylistParserService playlistParserService;

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
     * 获取可用格式列表
     */
    @PostMapping("/formats")
    public ApiResponse<List<Map<String, Object>>> getAvailableFormats(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            String platform = request.get("platform");
            log.info("获取可用格式列表: {}, 平台: {}", url, platform);

            if (url == null || url.trim().isEmpty()) {
                return ApiResponse.error("URL不能为空");
            }

            if (platform == null || platform.trim().isEmpty()) {
                return ApiResponse.error("平台标识不能为空");
            }

            List<Map<String, Object>> formats = musicSearchService.getAvailableFormats(url, platform);

            if (formats != null && !formats.isEmpty()) {
                return ApiResponse.success(formats);
            } else {
                return ApiResponse.error("无法获取格式列表");
            }

        } catch (Exception e) {
            log.error("获取可用格式列表时发生错误", e);
            return ApiResponse.error("获取格式列表失败: " + e.getMessage());
        }
    }

    /**
     * 解析歌单
     */
    @PostMapping("/parse-playlist")
    public ApiResponse<PlaylistInfoDTO> parsePlaylist(@RequestBody PlaylistParseRequestDTO request) {
        try {
            log.info("收到歌单解析请求: {}", request);

            // 参数验证
            if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
                return ApiResponse.error("歌单URL不能为空");
            }

            // 验证URL是否支持
            if (!playlistParserService.isSupportedUrl(request.getUrl())) {
                return ApiResponse.error("不支持的歌单链接，目前支持QQ音乐和网易云音乐");
            }

            // 执行解析
            PlaylistInfoDTO playlist = playlistParserService.parsePlaylist(request);

            log.info("歌单解析完成: 标题={}, 歌曲数量={}", playlist.getTitle(), playlist.getSongCount());
            return ApiResponse.success(playlist);

        } catch (Exception e) {
            log.error("解析歌单时发生错误", e);
            return ApiResponse.error("解析歌单失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的歌单平台
     */
    @GetMapping("/supported-platforms")
    public ApiResponse<String[]> getSupportedPlatforms() {
        try {
            String[] platforms = playlistParserService.getSupportedPlatforms();
            return ApiResponse.success(platforms);
        } catch (Exception e) {
            log.error("获取支持平台时发生错误", e);
            return ApiResponse.error("获取支持平台失败: " + e.getMessage());
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
    public ApiResponse<Map<String, String>> downloadMusicToServer(@RequestBody Map<String, Object> request) {
        try {
            String url = (String) request.get("url");
            String title = (String) request.get("title");
            String artist = (String) request.get("artist");
            String platform = (String) request.get("platform");
            Map<String, Object> selectedFormat = (Map<String, Object>) request.get("selectedFormat");
            String playlistName = (String) request.get("playlistName");

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

            // 1. 生成文件名
            String fileName = generateServerFileName(title, artist, selectedFormat);

            // 2. 创建下载目录
            Path baseDownloadDir;
            if (serverDownloadPath.startsWith("/")) {
                // 绝对路径
                baseDownloadDir = Paths.get(serverDownloadPath);
                log.info("使用绝对路径: {}", serverDownloadPath);
            } else {
                // 相对路径，相对于当前工作目录
                baseDownloadDir = Paths.get(serverDownloadPath);
                log.info("使用相对路径: {}", serverDownloadPath);
            }

            // 如果有歌单名称，创建歌单子目录
            Path downloadDir = baseDownloadDir;
            if (playlistName != null && !playlistName.trim().isEmpty()) {
                // 清理歌单名称，移除不合法的文件名字符
                String cleanPlaylistName = playlistName.trim()
                    .replaceAll("[\\\\/:*?\"<>|]", "_")  // 替换Windows不允许的字符
                    .replaceAll("\\s+", "_");           // 替换空格为下划线

                downloadDir = baseDownloadDir.resolve(cleanPlaylistName);
                log.info("检测到歌单名称: {}，将下载到子目录: {}", playlistName, downloadDir.toAbsolutePath());
            }

            if (!Files.exists(downloadDir)) {
                try {
                    Files.createDirectories(downloadDir);
                    log.info("创建下载目录成功: {}", downloadDir.toAbsolutePath());
                } catch (Exception e) {
                    log.error("创建下载目录失败: {}", downloadDir.toAbsolutePath(), e);
                    return ApiResponse.error("无法创建下载目录: " + downloadDir.toAbsolutePath() +
                        "，请检查路径是否有效以及是否有足够的权限");
                }
            }

            // 3. 使用yt-dlp下载文件到服务器
            Path filePath = downloadDir.resolve(fileName);
            boolean success = musicSearchService.downloadAudioWithYtDlp(url, platform, selectedFormat, filePath);

            if (success) {
                // 文件已经按照指定的文件名保存，直接使用传入的文件名
                // 构建访问URL，考虑歌单子目录
                String relativePath = fileName;
                if (playlistName != null && !playlistName.trim().isEmpty()) {
                    String cleanPlaylistName = playlistName.trim()
                        .replaceAll("[\\\\/:*?\"<>|]", "_")
                        .replaceAll("\\s+", "_");
                    relativePath = cleanPlaylistName + "/" + fileName;
                }
                String accessUrl = buildFileAccessUrl(serverDownloadPath, relativePath);

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
            if (audioUrl.contains("bilivideo.cn") || audioUrl.contains("bilibili.com") || audioUrl.contains("bili")) {
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
    private String generateServerFileName(String title, String artist, Map<String, Object> selectedFormat) {
        // 清理文件名中的非法字符，保留中文字符
        String cleanTitle = title.replaceAll("[<>:\"/\\\\|?*]", "").trim();
        String cleanArtist = artist != null ? artist.replaceAll("[<>:\"/\\\\|?*]", "").trim() : "";

        // 确保文件名不为空
        if (cleanTitle.isEmpty()) {
            cleanTitle = "未知标题";
        }
        if (cleanArtist.isEmpty()) {
            cleanArtist = "未知艺术家";
        }

        // 根据选择的格式确定文件扩展名
        String extension = ".mp3"; // 默认扩展名

        boolean containsFlac = Optional.ofNullable(selectedFormat.get("note"))
                .map(Object::toString)
                .filter(s -> s.contains("flac"))
                .isPresent();

        if (selectedFormat != null) {
            Boolean isAudio = (Boolean) selectedFormat.get("isAudio");
            Boolean isVideo = (Boolean) selectedFormat.get("isVideo");
            Boolean isMerged = (Boolean) selectedFormat.get("isMerged");
            String ext = (String) selectedFormat.get("ext");

            if (isMerged != null && isMerged) {
                // 合并下载固定使用mp4格式
                extension = ".mp4";
            } else if (Boolean.TRUE.equals(isAudio)) {
                // 音频格式处理
                if ("flac".equals(ext) || containsFlac) {
                    extension = ".flac";
                } else if ("m4a".equals(ext)) {
                    extension = ".m4a";
                } else if ("webm".equals(ext)) {
                    extension = ".webm";
                } else if ("ogg".equals(ext)) {
                    extension = ".ogg";
                } else if ("aac".equals(ext)) {
                    extension = ".aac";
                } else if ("opus".equals(ext)) {
                    extension = ".opus";
                } else {
                    extension = ".mp3"; // 音频默认mp3
                }
            } else if (Boolean.TRUE.equals(isVideo)) {
                // 视频格式处理
                if ("mp4".equals(ext)) {
                    extension = ".mp4";
                } else if ("webm".equals(ext)) {
                    extension = ".webm";
                } else if ("mkv".equals(ext)) {
                    extension = ".mkv";
                } else if ("avi".equals(ext)) {
                    extension = ".avi";
                } else {
                    extension = ".mp4"; // 视频默认mp4
                }
            } else if (ext != null) {
                // 如果格式类型不明确，根据扩展名判断
                extension = "." + ext;
            }
        }

        // 生成文件名：艺术家 - 歌曲名.扩展名
        if (cleanArtist != null && !cleanArtist.isEmpty() && !cleanArtist.equals(cleanTitle)) {
            return cleanArtist + " - " + cleanTitle + extension;
        } else {
            return cleanTitle + extension;
        }
    }



    /**
     * 流式下载音乐（推荐使用，对服务器友好）
     */
    @PostMapping("/download-stream")
    public ResponseEntity<StreamingResponseBody> downloadMusicStream(@RequestBody Map<String, Object> request) {
        try {
            String url = (String) request.get("url");
            String title = (String) request.get("title");
            String artist = (String) request.get("artist");
            String platform = (String) request.get("platform");
            Map<String, Object> selectedFormat = (Map<String, Object>) request.get("selectedFormat");

            log.info("开始流式下载音乐: title={}, artist={}, url={}", title, artist, url);
            log.debug("接收到的参数 - title: [{}], artist: [{}]", title, artist);

            // 参数验证
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 生成文件名
            String fileName = generateServerFileName(title, artist, selectedFormat);
            log.info("生成的文件名: {}", fileName);

            // 创建流式响应体
            StreamingResponseBody stream = outputStream -> {
                try {
                    // 使用yt-dlp直接输出到响应流
                    boolean success = musicSearchService.downloadAudioStreamWithYtDlp(url, platform, selectedFormat, outputStream);
                    if (!success) {
                        log.error("流式下载失败: {}", url);
                        throw new RuntimeException("下载失败");
                    }
                } catch (Exception e) {
                    log.error("流式下载过程中发生错误", e);
                    throw new RuntimeException("下载过程中发生错误: " + e.getMessage());
                }
            };

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();

            // 根据格式设置正确的 Content-Type
            String contentType = getContentTypeFromFormat(selectedFormat);
            headers.setContentType(MediaType.parseMediaType(contentType));

            // 设置下载文件名 - 使用更兼容的方式
            try {
                // 方法1：使用 Spring 的 ContentDisposition
                headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                );
                log.debug("使用 Spring ContentDisposition 设置文件名: {}", fileName);
            } catch (Exception e) {
                // 方法2：手动设置 Content-Disposition 头
                log.warn("Spring ContentDisposition 设置失败，使用手动方式: {}", e.getMessage());
                String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20"); // 空格用 %20 而不是 +
                headers.set("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);
                log.debug("手动设置 Content-Disposition: attachment; filename*=UTF-8''{}", encodedFileName);
            }

            log.info("开始流式传输文件: {}", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(stream);

        } catch (Exception e) {
            log.error("流式下载音乐时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载音乐到临时文件并返回给前端（用于本地下载）
     * @deprecated 推荐使用 /download-stream 接口，对服务器更友好
     */
    @PostMapping("/download-for-local")
    @Deprecated
    public ResponseEntity<byte[]> downloadMusicForLocal(@RequestBody Map<String, Object> request) {
        try {
            String url = (String) request.get("url");
            String title = (String) request.get("title");
            String artist = (String) request.get("artist");
            String platform = (String) request.get("platform");
            Map<String, Object> selectedFormat = (Map<String, Object>) request.get("selectedFormat");

            log.info("开始为本地下载准备音乐: title={}, artist={}, url={}", title, artist, url);

            // 参数验证
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 生成临时文件名
            String fileName = generateServerFileName(title, artist, selectedFormat);

            // 创建临时目录
            Path tempDir = Files.createTempDirectory("music-download-");
            Path tempFilePath = tempDir.resolve(fileName);

            try {
                // 使用yt-dlp下载到临时文件
                boolean success = musicSearchService.downloadAudioWithYtDlp(url, platform, selectedFormat, tempFilePath);

                if (success && Files.exists(tempFilePath)) {
                    // 读取文件内容
                    byte[] fileContent = Files.readAllBytes(tempFilePath);

                    // 设置响应头
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

                    // 正确设置文件名，避免编码问题
                    try {
                        headers.setContentDisposition(
                            ContentDisposition.builder("attachment")
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                        );
                        log.debug("使用 Spring ContentDisposition 设置文件名: {}", fileName);
                    } catch (Exception e) {
                        log.warn("Spring ContentDisposition 设置失败，使用手动方式: {}", e.getMessage());
                        String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                            .replace("+", "%20");
                        headers.set("Content-Disposition",
                            "attachment; filename*=UTF-8''" + encodedFileName);
                        log.debug("手动设置 Content-Disposition: attachment; filename*=UTF-8''{}", encodedFileName);
                    }

                    headers.setContentLength(fileContent.length);

                    log.info("本地下载文件准备完成: {}, 大小: {} bytes", fileName, fileContent.length);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(fileContent);
                } else {
                    log.error("下载失败或文件不存在: {}", tempFilePath);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } finally {
                // 清理临时文件和目录
                try {
                    if (Files.exists(tempFilePath)) {
                        Files.delete(tempFilePath);
                    }
                    if (Files.exists(tempDir)) {
                        Files.delete(tempDir);
                    }
                } catch (Exception e) {
                    log.warn("清理临时文件失败: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("为本地下载准备音乐时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据格式信息确定Content-Type
     */
    private String getContentTypeFromFormat(Map<String, Object> selectedFormat) {
        if (selectedFormat == null) {
            return "audio/mpeg"; // 默认mp3
        }

        String ext = (String) selectedFormat.get("ext");
        Boolean isAudio = (Boolean) selectedFormat.get("isAudio");
        Boolean isVideo = (Boolean) selectedFormat.get("isVideo");
        String note = (String) selectedFormat.get("note");

        // 检查是否包含flac
        boolean containsFlac = note != null && note.toLowerCase().contains("flac");

        if (Boolean.TRUE.equals(isAudio)) {
            // 音频格式
            if ("flac".equals(ext) || containsFlac) {
                return "audio/flac";
            } else if ("m4a".equals(ext)) {
                return "audio/mp4";
            } else if ("webm".equals(ext)) {
                return "audio/webm";
            } else if ("ogg".equals(ext)) {
                return "audio/ogg";
            } else if ("aac".equals(ext)) {
                return "audio/aac";
            } else if ("opus".equals(ext)) {
                return "audio/opus";
            } else {
                return "audio/mpeg"; // 默认mp3
            }
        } else if (Boolean.TRUE.equals(isVideo)) {
            // 视频格式
            if ("mp4".equals(ext)) {
                return "video/mp4";
            } else if ("webm".equals(ext)) {
                return "video/webm";
            } else if ("mkv".equals(ext)) {
                return "video/x-matroska";
            } else if ("avi".equals(ext)) {
                return "video/x-msvideo";
            } else {
                return "video/mp4"; // 默认mp4
            }
        } else if (ext != null) {
            // 根据扩展名推断
            switch (ext.toLowerCase()) {
                case "mp3": return "audio/mpeg";
                case "flac": return "audio/flac";
                case "m4a": return "audio/mp4";
                case "webm": return "audio/webm";
                case "ogg": return "audio/ogg";
                case "aac": return "audio/aac";
                case "opus": return "audio/opus";
                case "mp4": return "video/mp4";
                case "mkv": return "video/x-matroska";
                case "avi": return "video/x-msvideo";
                default: return "application/octet-stream";
            }
        }

        return "application/octet-stream"; // 默认二进制流
    }

    /**
     * 测试文件名编码（用于调试）
     */
    @GetMapping("/test-filename-encoding")
    public ResponseEntity<String> testFilenameEncoding(@RequestParam String filename) {
        try {
            log.info("测试文件名编码: {}", filename);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            // 测试不同的编码方式
            StringBuilder result = new StringBuilder();
            result.append("原始文件名: ").append(filename).append("\n");

            try {
                // 方法1：Spring ContentDisposition
                ContentDisposition cd1 = ContentDisposition.builder("attachment")
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();
                result.append("Spring ContentDisposition: ").append(cd1.toString()).append("\n");
            } catch (Exception e) {
                result.append("Spring ContentDisposition 失败: ").append(e.getMessage()).append("\n");
            }

            try {
                // 方法2：手动编码
                String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
                result.append("手动编码: attachment; filename*=UTF-8''").append(encodedFileName).append("\n");
            } catch (Exception e) {
                result.append("手动编码失败: ").append(e.getMessage()).append("\n");
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(result.toString());

        } catch (Exception e) {
            log.error("测试文件名编码失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("测试失败: " + e.getMessage());
        }
    }

    /**
     * 构建文件访问URL
     */
    private String buildFileAccessUrl(String serverDownloadPath, String fileName) {
        String relativePath;

        if (serverDownloadPath.startsWith("/")) {
            // 绝对路径：需要映射到Web可访问的路径
            // 对于绝对路径，我们使用文件名作为访问路径，实际文件位置由服务器配置处理
            log.warn("使用绝对路径 {} 保存文件，Web访问可能需要额外配置", serverDownloadPath);
            relativePath = "/downloads/" + fileName; // 使用通用的下载路径
        } else {
            // 相对路径：构建相对于contextPath的URL
            relativePath = serverDownloadPath + "/" + fileName;

            // 确保路径以/开头
            if (!relativePath.startsWith("/")) {
                relativePath = "/" + relativePath;
            }
        }

        // 添加contextPath
        if (contextPath != null && !contextPath.isEmpty()) {
            return contextPath + relativePath;
        } else {
            return relativePath;
        }
    }

    /**
     * 获取歌词
     */
    @PostMapping("/lyrics")
    public ApiResponse<Map<String, Object>> getLyrics(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            String title = request.get("title");
            String artist = request.get("artist");

            log.info("获取歌词: url={}, title={}, artist={}", url, title, artist);

            if (url == null || url.trim().isEmpty()) {
                return ApiResponse.error("URL不能为空");
            }

            Map<String, Object> lyrics = musicSearchService.getLyrics(url, title, artist);

            if (lyrics != null && !lyrics.isEmpty()) {
                return ApiResponse.success(lyrics);
            } else {
                return ApiResponse.error("未找到歌词");
            }

        } catch (Exception e) {
            log.error("获取歌词时发生错误", e);
            return ApiResponse.error("获取歌词失败: " + e.getMessage());
        }
    }

    /**
     * 测试VIP歌曲检测
     */
    @PostMapping("/test/vip-detection")
    public ApiResponse<Map<String, Object>> testVipDetection(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            log.info("测试VIP歌曲检测: {}", url);

            PlaylistParseRequestDTO parseRequest = new PlaylistParseRequestDTO();
            parseRequest.setUrl(url);
            parseRequest.setPlatform("auto");

            PlaylistInfoDTO playlist = playlistParserService.parsePlaylist(parseRequest);

            // 统计VIP歌曲
            long vipCount = playlist.getSongs().stream()
                .mapToLong(song -> song.isVip() ? 1 : 0)
                .sum();

            Map<String, Object> result = new HashMap<>();
            result.put("totalSongs", playlist.getSongs().size());
            result.put("vipSongs", vipCount);
            result.put("freeSongs", playlist.getSongs().size() - vipCount);
            result.put("vipPercentage", playlist.getSongs().size() > 0 ?
                (double) vipCount / playlist.getSongs().size() * 100 : 0);

            // 返回前10首歌曲的VIP状态作为示例
            result.put("sampleSongs", playlist.getSongs().stream()
                .limit(10)
                .map(song -> {
                    Map<String, Object> songInfo = new HashMap<>();
                    songInfo.put("title", song.getTitle());
                    songInfo.put("artist", song.getArtist());
                    songInfo.put("vip", song.isVip());
                    return songInfo;
                })
                .toList());

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("测试VIP歌曲检测失败", e);
            return ApiResponse.error("测试失败: " + e.getMessage());
        }
    }
}
