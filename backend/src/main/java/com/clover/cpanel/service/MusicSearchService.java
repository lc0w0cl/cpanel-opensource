package com.clover.cpanel.service;

import com.clover.cpanel.dto.MusicSearchRequestDTO;
import com.clover.cpanel.dto.MusicSearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 音乐搜索服务
 */
@Slf4j
@Service
public class MusicSearchService {

    private static final String BILIBILI_SEARCH_URL = "https://search.bilibili.com/all";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Value("${music.proxy.base-url:http://localhost:8080}")
    private String proxyBaseUrl;
    @Autowired
    private SystemConfigService systemConfigService;
    
    /**
     * 搜索音乐
     */
    public List<MusicSearchResultDTO> searchMusic(MusicSearchRequestDTO request) {
        List<MusicSearchResultDTO> results = new ArrayList<>();
        
        try {
            if ("bilibili".equals(request.getPlatform()) || "both".equals(request.getPlatform())) {
                List<MusicSearchResultDTO> bilibiliResults = searchBilibili(request);
                results.addAll(bilibiliResults);
            }
            
            // YouTube搜索或URL解析
            if ("youtube".equals(request.getPlatform()) || "both".equals(request.getPlatform())) {
                if ("url".equals(request.getSearchType())) {
                    // URL模式：直接解析YouTube视频URL
                    MusicSearchResultDTO youtubeResult = parseYouTubeVideoUrl(request.getQuery());
                    if (youtubeResult != null) {
                        results.add(youtubeResult);
                    }
                } else {
                    // 关键词模式：YouTube搜索功能待实现
                    log.info("YouTube关键词搜索功能待实现");
                }
            }
            
        } catch (Exception e) {
            log.error("搜索音乐时发生错误", e);
        }
        
        return results;
    }
    
    /**
     * 搜索哔哩哔哩
     */
    private List<MusicSearchResultDTO> searchBilibili(MusicSearchRequestDTO request) throws IOException {
        List<MusicSearchResultDTO> results = new ArrayList<>();

        // 检查搜索类型
        if ("url".equals(request.getSearchType())) {
            // URL模式：直接解析视频URL
            MusicSearchResultDTO videoResult = parseBilibiliVideoUrl(request.getQuery());
            if (videoResult != null) {
                results.add(videoResult);
            }
            return results;
        }

        // 关键词模式：搜索B站
        // 构建搜索URL - 添加更多参数以获得更好的搜索结果
        String encodedQuery = URLEncoder.encode(request.getQuery(), StandardCharsets.UTF_8);
        String searchUrl = BILIBILI_SEARCH_URL + "?keyword=" + encodedQuery
                + "&page=" + request.getPage()
                + "&order=totalrank"  // 按综合排序
                + "&duration=0"       // 不限时长
                + "&tids_1=3";        // 音乐分区

        log.info("正在搜索哔哩哔哩: {}", searchUrl);

        try {
            // 模拟浏览器请求 - 增强请求头
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Cache-Control", "max-age=0")
                    .referrer("https://www.bilibili.com/")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            // 尝试多种选择器来解析搜索结果
            Elements videoItems = doc.select(".bili-video-card");
            if (videoItems.isEmpty()) {
                // 尝试其他可能的选择器
                videoItems = doc.select(".video-item");
                if (videoItems.isEmpty()) {
                    videoItems = doc.select(".video");
                }
            }

            log.info("找到 {} 个视频项", videoItems.size());

            // 如果没有找到视频项，尝试输出页面内容进行调试
            if (videoItems.isEmpty()) {
                log.warn("未找到视频项，页面标题: {}", doc.title());
                log.debug("页面内容片段: {}", doc.body().html().substring(0, Math.min(1000, doc.body().html().length())));
            }

            for (Element item : videoItems) {
                try {
                    MusicSearchResultDTO result = parseBilibiliVideoItem(item);
                    if (result != null) {
                        results.add(result);
                        // 限制结果数量
                        if (results.size() >= request.getPageSize()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析哔哩哔哩视频项时出错", e);
                }
            }

            log.info("从哔哩哔哩获取到 {} 个有效结果", results.size());

        } catch (IOException e) {
            log.error("请求哔哩哔哩搜索页面失败: {}", e.getMessage());
            throw e;
        }

        return results;
    }
    
    /**
     * 解析哔哩哔哩视频项
     */
    private MusicSearchResultDTO parseBilibiliVideoItem(Element item) {
        try {
            // 根据新的HTML结构提取标题和链接
            Element titleElement = item.selectFirst(".bili-video-card__info--tit");
            Element linkElement = item.selectFirst("a[href*='/video/']");

            // 如果没找到新结构，尝试旧结构
            if (titleElement == null || linkElement == null) {
                titleElement = item.selectFirst(".title a");
                linkElement = titleElement;
            }

            if (titleElement == null || linkElement == null) return null;

            // 提取标题 - 去除HTML标签（如<em>标签）
            String title = titleElement.text().trim();
            if (title.isEmpty()) {
                title = titleElement.attr("title");
            }

            // 提取链接
            String href = linkElement.attr("href");
            String url = href.startsWith("//") ? "https:" + href :
                        href.startsWith("/") ? "https://www.bilibili.com" + href : href;

            // 提取视频ID
            String videoId = extractBilibiliVideoId(url);
            if (videoId.isEmpty()) return null;

            // 提取UP主 - 根据新结构
            Element uploaderElement = item.selectFirst(".bili-video-card__info--author");
            if (uploaderElement == null) {
                uploaderElement = item.selectFirst(".upname a");
            }
            if (uploaderElement == null) {
                uploaderElement = item.selectFirst(".up-name");
            }
            String artist = uploaderElement != null ? uploaderElement.text().trim() : "未知UP主";

            // 提取时长 - 根据新结构
            Element durationElement = item.selectFirst(".bili-video-card__stats__duration");
            if (durationElement == null) {
                durationElement = item.selectFirst(".duration");
            }
            String duration = durationElement != null ? durationElement.text().trim() : "未知时长";

            // 提取缩略图 - 根据新结构，从picture元素中的img标签获取
            Element imgElement = item.selectFirst(".bili-video-card__cover img");
            if (imgElement == null) {
                imgElement = item.selectFirst("img");
            }
            String thumbnail = "";
            if (imgElement != null) {
                // 优先获取src属性
                thumbnail = imgElement.attr("src");
                if (thumbnail.isEmpty()) {
                    thumbnail = imgElement.attr("data-src");
                }
                if (thumbnail.isEmpty()) {
                    thumbnail = imgElement.attr("data-original");
                }
                if (!thumbnail.isEmpty() && !thumbnail.startsWith("http")) {
                    thumbnail = "https:" + thumbnail;
                }

                // 将缩略图URL转换为代理URL
                if (!thumbnail.isEmpty()) {
                    thumbnail = convertToProxyUrl(thumbnail);
                }
            }

            // 提取播放量 - 根据新结构，从stats区域获取第一个数字
            Elements statsItems = item.select(".bili-video-card__stats--item span:last-child");
            String playCount = "0";
            if (!statsItems.isEmpty()) {
                playCount = statsItems.first().text().trim();
            } else {
                // 尝试旧结构
                Element playElement = item.selectFirst(".play");
                if (playElement != null) {
                    playCount = playElement.text().trim();
                }
            }

            // 提取发布时间 - 根据新结构
            Element timeElement = item.selectFirst(".bili-video-card__info--date");
            if (timeElement == null) {
                timeElement = item.selectFirst(".time");
            }
            String publishTime = timeElement != null ? timeElement.text().trim().replace("·", "").trim() : "";

            // 清理标题中的HTML标签
            title = title.replaceAll("<[^>]+>", "").trim();

            // 添加调试日志
            log.debug("解析视频项: title={}, artist={}, duration={}, playCount={}, publishTime={}",
                     title, artist, duration, playCount, publishTime);

            return MusicSearchResultDTO.builder()
                    .id(videoId)
                    .title(title)
                    .artist(artist)
                    .duration(duration)
                    .platform("bilibili")
                    .thumbnail(thumbnail)
                    .url(url)
                    .quality("HD")
                    .playCount(playCount)
                    .publishTime(publishTime)
                    .build();

        } catch (Exception e) {
            log.warn("解析哔哩哔哩视频项失败: {}", e.getMessage());
            // 添加更详细的错误信息
            log.debug("解析失败的HTML片段: {}", item.outerHtml().substring(0, Math.min(500, item.outerHtml().length())));
            return null;
        }
    }

    /**
     * 解析哔哩哔哩视频URL，获取视频详情
     */
    private MusicSearchResultDTO parseBilibiliVideoUrl(String videoUrl) {
        try {
            log.info("正在解析B站视频URL: {}", videoUrl);

            // 验证URL格式
            if (!videoUrl.contains("bilibili.com/video/")) {
                log.warn("不是有效的B站视频URL: {}", videoUrl);
                return null;
            }

            // 提取视频ID
            String videoId = extractBilibiliVideoId(videoUrl);
            if (videoId.isEmpty()) {
                log.warn("无法从URL中提取视频ID: {}", videoUrl);
                return null;
            }

            // 确保URL格式正确
            String normalizedUrl = videoUrl;
            if (!normalizedUrl.startsWith("http")) {
                normalizedUrl = "https://" + normalizedUrl;
            }

            // 获取视频页面
            Document doc = Jsoup.connect(normalizedUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(10000)
                    .get();

            // 提取视频信息
            String title = "";
            String artist = "";
            String duration = "";
            String thumbnail = "";
            String playCount = "";
            String publishTime = "";

            // 获取标题
            Element titleElement = doc.selectFirst("h1[title], .video-title, title");
            if (titleElement != null) {
                title = titleElement.text().trim();
                // 移除可能的后缀
                title = title.replaceAll("_哔哩哔哩_bilibili$", "").trim();
            }

            // 获取UP主信息
            Element uploaderElement = doc.selectFirst(".up-name, .username, .up-detail-top .up-name a");
            if (uploaderElement != null) {
                artist = uploaderElement.text().trim();
            }

            // 获取时长 - 从多个可能的位置尝试
            Element durationElement = doc.selectFirst(".duration, .video-duration, [class*='duration']");
            if (durationElement != null) {
                duration = durationElement.text().trim();
            }

            // 获取缩略图
            Element thumbnailElement = doc.selectFirst("meta[property='og:image'], .player-cover img, video");
            if (thumbnailElement != null) {
                if (thumbnailElement.tagName().equals("meta")) {
                    thumbnail = thumbnailElement.attr("content");
                } else {
                    thumbnail = thumbnailElement.attr("src");
                    if (thumbnail.isEmpty()) {
                        thumbnail = thumbnailElement.attr("poster");
                    }
                }
                // 转换为代理URL
                thumbnail = convertToProxyUrl(thumbnail);
            }

            // 获取播放量
            Element playCountElement = doc.selectFirst(".view, .play-count, [class*='play']");
            if (playCountElement != null) {
                playCount = playCountElement.text().trim();
            }

            // 获取发布时间
            Element publishElement = doc.selectFirst(".pubdate, .publish-time, [class*='time']");
            if (publishElement != null) {
                publishTime = publishElement.text().trim();
            }

            // 如果某些信息为空，设置默认值
            if (title.isEmpty()) {
                title = "B站视频";
            }
            if (artist.isEmpty()) {
                artist = "未知UP主";
            }
            if (duration.isEmpty()) {
                duration = "未知";
            }

            log.info("成功解析B站视频: 标题={}, UP主={}, 时长={}", title, artist, duration);

            return MusicSearchResultDTO.builder()
                    .id(videoId)
                    .title(title)
                    .artist(artist)
                    .duration(duration)
                    .platform("bilibili")
                    .thumbnail(thumbnail)
                    .url(normalizedUrl)
                    .quality("音频")
                    .playCount(playCount)
                    .publishTime(publishTime)
                    .description("直链解析")
                    .build();

        } catch (Exception e) {
            log.error("解析B站视频URL失败: {}", videoUrl, e);
            return null;
        }
    }

    /**
     * 从URL中提取哔哩哔哩视频ID
     */
    private String extractBilibiliVideoId(String url) {
        Pattern pattern = Pattern.compile("/video/([^/?]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 将原始图片URL转换为代理URL
     */
    private String convertToProxyUrl(String originalUrl) {
        try {
            if (originalUrl == null || originalUrl.trim().isEmpty()) {
                return "";
            }

            // 检查是否为哔哩哔哩的图片
            String cleanUrl = originalUrl.startsWith("//") ? originalUrl.substring(2) : originalUrl;
            if (cleanUrl.startsWith("https://")) {
                cleanUrl = cleanUrl.substring(8);
            } else if (cleanUrl.startsWith("http://")) {
                cleanUrl = cleanUrl.substring(7);
            }

            // 只对哔哩哔哩的图片进行代理
            if (cleanUrl.startsWith("i0.hdslb.com/") ||
                cleanUrl.startsWith("i1.hdslb.com/") ||
                cleanUrl.startsWith("i2.hdslb.com/") ||
                cleanUrl.startsWith("s1.hdslb.com/") ||
                cleanUrl.startsWith("s2.hdslb.com/")) {

                // 构建代理URL
                String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
                return String.format("%s/api/music/proxy/image?url=%s", proxyBaseUrl, encodedUrl);
            }

            return originalUrl;
        } catch (Exception e) {
            log.warn("转换代理URL失败: {}", e.getMessage());
            return originalUrl;
        }
    }

    /**
     * 获取音频流URL（通过平台和视频ID）
     */
    public String getAudioStreamUrl(String platform, String videoId) {
        try {
            String videoUrl;
            if ("bilibili".equals(platform)) {
                videoUrl = "https://www.bilibili.com/video/" + videoId;
            } else if ("youtube".equals(platform)) {
                videoUrl = "https://www.youtube.com/watch?v=" + videoId;
            } else {
                log.warn("不支持的平台: {}", platform);
                return null;
            }

            return getAudioStreamUrlByUrl(videoUrl);
        } catch (Exception e) {
            log.error("获取音频流URL失败: platform={}, videoId={}", platform, videoId, e);
            return null;
        }
    }

    /**
     * 获取音频流URL（通过视频URL）
     */
    public String getAudioStreamUrlByUrl(String videoUrl) {
        try {
            log.info("正在获取音频流URL: {}", videoUrl);

            // 构建yt-dlp命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "--get-url",           // 只获取URL，不下载
                "--format", "bestaudio/best",  // 获取最佳音频格式
                "--no-playlist",       // 不处理播放列表
                videoUrl
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                String audioUrl = output.toString().trim();
                if (!audioUrl.isEmpty() && audioUrl.startsWith("http")) {
                    log.info("成功获取音频流URL: {}", audioUrl.substring(0, Math.min(100, audioUrl.length())));
                    return audioUrl;
                } else {
                    log.warn("yt-dlp返回的URL格式不正确: {}", audioUrl);
                    return null;
                }
            } else {
                log.error("yt-dlp执行失败，退出码: {}, 输出: {}", exitCode, output.toString());
                return null;
            }

        } catch (Exception e) {
            log.error("执行yt-dlp时发生错误", e);
            return null;
        }
    }

    /**
     * 获取可用格式列表（通过视频URL和平台）
     */
    public List<Map<String, Object>> getAvailableFormats(String videoUrl, String platform) {
        File tempCookieFile = null;
        try {
            log.info("正在获取可用格式列表: {}, 平台: {}", videoUrl, platform);

            // 根据平台获取对应的cookie配置
            String cookieValue = getCookieByPlatform(platform);

            // 构建yt-dlp命令，使用-F参数获取格式列表
            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("-F");                  // 列出所有可用格式
            command.add("--no-playlist");       // 不处理播放列表

            // 如果有cookie，创建临时cookie文件
            if (cookieValue != null && !cookieValue.trim().isEmpty()) {
                try {
                    // 创建临时cookie文件
                    tempCookieFile = createTempCookieFile(cookieValue.trim(), platform);
                    command.add("--cookies");
                    command.add(tempCookieFile.getAbsolutePath());
                    log.info("使用{}平台cookie文件: {}", platform, tempCookieFile.getAbsolutePath());
                } catch (Exception e) {
                    log.error("创建{}平台cookie文件失败，使用默认方式", platform, e);
                }
            } else {
                log.info("{}平台未配置cookie，使用默认方式", platform);
            }

            command.add(videoUrl);

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return parseFormatOutput(output.toString());
            } else {
                log.error("yt-dlp获取格式列表失败，退出码: {}, 输出: {}", exitCode, output.toString());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            log.error("获取可用格式列表时发生错误", e);
            return new ArrayList<>();
        } finally {
            // 清理临时cookie文件
            if (tempCookieFile != null && tempCookieFile.exists()) {
                try {
                    if (tempCookieFile.delete()) {
                        log.debug("临时cookie文件已删除: {}", tempCookieFile.getAbsolutePath());
                    } else {
                        log.warn("无法删除临时cookie文件: {}", tempCookieFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    log.error("删除临时cookie文件时发生错误", e);
                }
            }
        }
    }

    /**
     * 根据平台获取对应的cookie配置
     */
    private String getCookieByPlatform(String platform) {
        try {
            String cookieKey;
            switch (platform.toLowerCase()) {
                case "bilibili":
                    cookieKey = "bilibili_cookie";
                    break;
                case "youtube":
                    cookieKey = "youtube_cookie";
                    break;
                default:
                    log.warn("未知平台: {}, 不使用cookie", platform);
                    return null;
            }

            String cookieValue = systemConfigService.getConfigValue(cookieKey);
            log.debug("获取{}平台cookie配置: {}", platform, cookieValue != null ? "已配置" : "未配置");
            return cookieValue;
        } catch (Exception e) {
            log.error("获取{}平台cookie配置失败", platform, e);
            return null;
        }
    }

    /**
     * 创建临时cookie文件
     */
    private File createTempCookieFile(String cookieValue, String platform) throws Exception {
        // 创建临时文件
        File tempFile = File.createTempFile("yt-dlp-cookies-" + platform + "-", ".txt");

        try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
            // 直接写入cookie内容
            writer.write(cookieValue);
        }

        log.debug("创建临时cookie文件: {}", tempFile.getAbsolutePath());
        return tempFile;
    }



    /**
     * 解析yt-dlp格式输出
     */
    private List<Map<String, Object>> parseFormatOutput(String output) {
        List<Map<String, Object>> formats = new ArrayList<>();

        try {
            String[] lines = output.split("\n");
            boolean formatSectionStarted = false;

            for (String line : lines) {
                line = line.trim();

                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("[")) {
                    continue;
                }

                // 检查是否到达格式列表部分
                if (line.contains("ID") && line.contains("EXT") && line.contains("RESOLUTION")) {
                    formatSectionStarted = true;
                    continue;
                }

                // 如果还没到格式列表部分，继续跳过
                if (!formatSectionStarted) {
                    continue;
                }

                // 解析格式行
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    Map<String, Object> format = new HashMap<>();

                    String formatId = cleanText(parts[0]);
                    String ext = cleanText(parts[1]);
                    String resolution = cleanText(parts[2]);

                    // 跳过无效的格式ID
                    if (formatId.equals("ID") || formatId.contains("-")) {
                        continue;
                    }

                    format.put("formatId", formatId);
                    format.put("ext", ext);
                    format.put("resolution", resolution);

                    // 解析其他信息
                    if (parts.length > 3) {
                        StringBuilder note = new StringBuilder();
                        for (int i = 3; i < parts.length; i++) {
                            if (note.length() > 0) note.append(" ");
                            note.append(parts[i]);
                        }
                        format.put("note", cleanText(note.toString()));

                        // 判断是否为音频格式
                        String noteStr = note.toString().toLowerCase();
                        boolean isAudio = noteStr.contains("audio only") ||
                                         ext.matches("m4a|mp3|ogg|webm|flac|aac|opus");
                        format.put("isAudio", isAudio);

                        // 判断是否为视频格式
                        boolean isVideo = !isAudio && !resolution.equals("audio");
                        format.put("isVideo", isVideo);

                        // 提取比特率信息
                        if (noteStr.contains("k")) {
                            String[] noteParts = noteStr.split("\\s+");
                            for (String part : noteParts) {
                                if (part.endsWith("k") && part.length() > 1) {
                                    try {
                                        String bitrateStr = part.substring(0, part.length() - 1);
                                        format.put("bitrate", bitrateStr + "k");
                                        break;
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }

                    formats.add(format);
                }
            }

            log.info("解析到 {} 个可用格式", formats.size());
            return formats;

        } catch (Exception e) {
            log.error("解析格式输出时发生错误", e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析YouTube视频URL，获取视频详情
     */
    private MusicSearchResultDTO parseYouTubeVideoUrl(String videoUrl) {
        try {
            log.info("正在解析YouTube视频URL: {}", videoUrl);

            // 验证URL格式
            if (!videoUrl.contains("youtube.com/watch") && !videoUrl.contains("youtu.be/")) {
                log.warn("不是有效的YouTube视频URL: {}", videoUrl);
                return null;
            }

            // 提取视频ID
            String videoId = extractYouTubeVideoId(videoUrl);
            if (videoId.isEmpty()) {
                log.warn("无法从URL中提取YouTube视频ID: {}", videoUrl);
                return null;
            }

            // 确保URL格式正确
            String normalizedUrl = videoUrl;
            if (!normalizedUrl.startsWith("http")) {
                normalizedUrl = "https://" + normalizedUrl;
            }

            // 使用yt-dlp获取视频信息
            ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "--dump-json",          // 输出JSON格式的视频信息
                "--no-playlist",        // 不处理播放列表
                normalizedUrl
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // 解析JSON输出
                String jsonOutput = output.toString().trim();
                if (jsonOutput.startsWith("{")) {
                    // 简单的JSON解析（这里可以使用Jackson或Gson，但为了简单起见使用字符串匹配）
                    String title = extractJsonValue(jsonOutput, "title");
                    String uploader = extractJsonValue(jsonOutput, "uploader");
                    String duration = extractJsonValue(jsonOutput, "duration_string");
                    String thumbnail = extractJsonValue(jsonOutput, "thumbnail");
                    String viewCount = extractJsonValue(jsonOutput, "view_count");
                    String uploadDate = extractJsonValue(jsonOutput, "upload_date");

                    // 格式化播放量
                    if (viewCount != null && !viewCount.isEmpty()) {
                        try {
                            long views = Long.parseLong(viewCount);
                            if (views >= 10000) {
                                viewCount = String.format("%.1f万", views / 10000.0);
                            } else {
                                viewCount = String.valueOf(views);
                            }
                        } catch (NumberFormatException e) {
                            // 保持原值
                        }
                    }

                    // 格式化上传日期
                    if (uploadDate != null && uploadDate.length() == 8) {
                        try {
                            String year = uploadDate.substring(0, 4);
                            String month = uploadDate.substring(4, 6);
                            String day = uploadDate.substring(6, 8);
                            uploadDate = year + "-" + month + "-" + day;
                        } catch (Exception e) {
                            // 保持原值
                        }
                    }

                    // 设置默认值
                    if (title == null || title.isEmpty()) {
                        title = "YouTube视频";
                    }
                    if (uploader == null || uploader.isEmpty()) {
                        uploader = "未知作者";
                    }
                    if (duration == null || duration.isEmpty()) {
                        duration = "未知";
                    }

                    log.info("成功解析YouTube视频: 标题={}, 作者={}, 时长={}", title, uploader, duration);

                    return MusicSearchResultDTO.builder()
                            .id(videoId)
                            .title(title)
                            .artist(uploader)
                            .duration(duration)
                            .platform("youtube")
                            .thumbnail(thumbnail)
                            .url(normalizedUrl)
                            .quality("音频")
                            .playCount(viewCount)
                            .publishTime(uploadDate)
                            .description("直链解析")
                            .build();
                } else {
                    log.warn("yt-dlp返回的不是有效的JSON格式: {}", jsonOutput);
                    return null;
                }
            } else {
                log.error("yt-dlp执行失败，退出码: {}, 输出: {}", exitCode, output.toString());
                return null;
            }

        } catch (Exception e) {
            log.error("解析YouTube视频URL失败: {}", videoUrl, e);
            return null;
        }
    }

    /**
     * 从YouTube URL中提取视频ID
     */
    private String extractYouTubeVideoId(String url) {
        // 处理 youtube.com/watch?v=VIDEO_ID 格式
        Pattern pattern1 = Pattern.compile("[?&]v=([^&]+)");
        Matcher matcher1 = pattern1.matcher(url);
        if (matcher1.find()) {
            return matcher1.group(1);
        }

        // 处理 youtu.be/VIDEO_ID 格式
        Pattern pattern2 = Pattern.compile("youtu\\.be/([^?]+)");
        Matcher matcher2 = pattern2.matcher(url);
        if (matcher2.find()) {
            return matcher2.group(1);
        }

        return "";
    }

    /**
     * 从JSON字符串中提取指定字段的值
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }

            // 尝试数字值
            String numberPattern = "\"" + key + "\"\\s*:\\s*([0-9]+)";
            Pattern np = Pattern.compile(numberPattern);
            Matcher nm = np.matcher(json);
            if (nm.find()) {
                return nm.group(1);
            }
        } catch (Exception e) {
            log.debug("提取JSON值失败: key={}, error={}", key, e.getMessage());
        }
        return null;
    }

    /**
     * 清理文本中的乱码字符
     */
    private String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // 移除常见的乱码字符和控制字符
            return text
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // 移除控制字符，保留换行和制表符
                .replaceAll("\\uFFFD+", "") // 移除Unicode替换字符
                .replaceAll("�+", "") // 移除替换字符
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // 移除其他控制字符
                .trim();
        } catch (Exception e) {
            log.warn("清理文本时发生错误: {}", e.getMessage());
            return text;
        }
    }
}
