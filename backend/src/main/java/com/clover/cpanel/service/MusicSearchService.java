package com.clover.cpanel.service;

import com.clover.cpanel.dto.MusicSearchRequestDTO;
import com.clover.cpanel.dto.MusicSearchResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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


    @Autowired
    private SystemConfigService systemConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
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

                // 构建代理URL - 直接返回相对路径
                String encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
                return String.format("/api/music/proxy/image?url=%s", encodedUrl);
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
     * 获取歌词
     */
    public Map<String, Object> getLyrics(String videoUrl, String title, String artist) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("正在获取歌词: url={}, title={}, artist={}", videoUrl, title, artist);

            // 首先尝试从yt-dlp获取字幕
            Map<String, Object> ytdlpLyrics = getLyricsFromYtDlp(videoUrl);
            if (ytdlpLyrics != null && !ytdlpLyrics.isEmpty()) {
                result.putAll(ytdlpLyrics);
                result.put("source", "yt-dlp");
                log.info("从yt-dlp获取到歌词");
                return result;
            }

            // 如果yt-dlp没有歌词，尝试通过歌曲信息搜索歌词
            if (title != null && !title.trim().isEmpty()) {
                Map<String, Object> searchLyrics = searchLyricsByTitle(title, artist);
                if (searchLyrics != null && !searchLyrics.isEmpty()) {
                    result.putAll(searchLyrics);
                    result.put("source", "search");
                    log.info("通过搜索获取到歌词");
                    return result;
                }
            }

            log.info("未找到歌词");
            return null;

        } catch (Exception e) {
            log.error("获取歌词时发生错误", e);
            return null;
        }
    }

    /**
     * 从yt-dlp获取字幕作为歌词
     */
    private Map<String, Object> getLyricsFromYtDlp(String videoUrl) {
        try {
            log.info("尝试从yt-dlp获取歌词: {}", videoUrl);

            // 构建yt-dlp命令获取完整的JSON信息（包括歌词）
            ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "--dump-json",
                "--skip-download",
                videoUrl
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

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
                String jsonOutput = output.toString().trim();
                // 解析JSON获取歌词信息
                return parseSubtitlesFromJson(jsonOutput);
            } else {
                log.warn("yt-dlp获取信息失败，退出码: {}, 输出: {}", exitCode, output.toString());
                return null;
            }

        } catch (Exception e) {
            log.error("从yt-dlp获取歌词时发生错误", e);
            return null;
        }
    }

    /**
     * 解析yt-dlp返回的JSON中的歌词信息
     */
    private Map<String, Object> parseSubtitlesFromJson(String jsonOutput) {
        try {
            log.info("开始解析yt-dlp JSON输出");

            // 简单的JSON解析，查找歌词相关字段
            Map<String, Object> result = new HashMap<>();

            // 查找description字段中的歌词（网易云音乐会在这里放歌词）
            if (jsonOutput.contains("\"description\"")) {
                String description = extractJsonField(jsonOutput, "description");
                if (description != null && isLrcFormat(description)) {
                    log.info("在description字段中找到LRC格式歌词");
                    result.put("lyrics", cleanLrcLyrics(description));
                    result.put("type", "lrc");
                    result.put("synced", true);
                    return result;
                }
            }

            // 查找subtitles.lyrics字段
            if (jsonOutput.contains("\"subtitles\"") && jsonOutput.contains("\"lyrics\"")) {
                log.info("找到subtitles.lyrics字段");
                String lyricsData = extractLyricsFromSubtitles(jsonOutput);
                if (lyricsData != null && !lyricsData.trim().isEmpty()) {
                    log.info("从subtitles中提取到歌词");
                    result.put("lyrics", cleanLrcLyrics(lyricsData));
                    result.put("type", "lrc");
                    result.put("synced", true);
                    return result;
                }
            }

            log.info("未在JSON中找到歌词信息");
            return null;

        } catch (Exception e) {
            log.error("解析歌词JSON时发生错误", e);
            return null;
        }
    }

    /**
     * 从JSON字符串中提取指定字段的值
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*?)\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);
            if (m.find()) {
                String value = m.group(1);
                // 解码Unicode转义字符和其他转义字符
                return decodeJsonString(value);
            }
        } catch (Exception e) {
            log.error("提取JSON字段时发生错误: {}", fieldName, e);
        }
        return null;
    }

    /**
     * 解码JSON字符串中的转义字符
     */
    private String decodeJsonString(String jsonString) {
        if (jsonString == null) {
            return null;
        }

        try {
            // 替换常见的转义字符
            String decoded = jsonString
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

            // 解码Unicode转义字符 \\uXXXX
            Pattern unicodePattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
            Matcher matcher = unicodePattern.matcher(decoded);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String unicodeHex = matcher.group(1);
                int codePoint = Integer.parseInt(unicodeHex, 16);
                char unicodeChar = (char) codePoint;
                matcher.appendReplacement(sb, String.valueOf(unicodeChar));
            }
            matcher.appendTail(sb);

            return sb.toString();

        } catch (Exception e) {
            log.error("解码JSON字符串时发生错误", e);
            return jsonString;
        }
    }

    /**
     * 从subtitles字段中提取歌词数据
     */
    private String extractLyricsFromSubtitles(String json) {
        try {
            // 查找 "subtitles":{"lyrics":[{"data":"..."}]}
            String pattern = "\"subtitles\"\\s*:\\s*\\{[^}]*\"lyrics\"\\s*:\\s*\\[\\s*\\{[^}]*\"data\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*?)\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);
            if (m.find()) {
                String value = m.group(1);
                // 解码Unicode转义字符和其他转义字符
                return decodeJsonString(value);
            }
        } catch (Exception e) {
            log.error("从subtitles提取歌词时发生错误", e);
        }
        return null;
    }

    /**
     * 判断是否为LRC格式歌词
     */
    private boolean isLrcFormat(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        // 检查是否包含时间标签 [mm:ss.xx] 或 [mm:ss.xxx]
        return text.contains("[") && text.matches(".*\\[\\d{2}:\\d{2}\\.\\d{2,3}\\].*");
    }

    /**
     * 清理LRC歌词，移除时间标签，保留纯文本
     */
    private String cleanLrcLyrics(String lrcText) {
        if (lrcText == null || lrcText.trim().isEmpty()) {
            return "";
        }

        try {
            StringBuilder cleanLyrics = new StringBuilder();
            String[] lines = lrcText.split("\n");

            for (String line : lines) {
                // 移除时间标签 [mm:ss.xx]
//                String cleanLine = line.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2}\\]", "").trim()

                // 跳过空行和制作信息
                if (!line.isEmpty() &&
                    !line.startsWith("作词") &&
                    !line.startsWith("作曲") &&
                    !line.startsWith("编曲") &&
                    !line.startsWith("制作") &&
                    !line.startsWith("混音") &&
                    !line.startsWith("监制") &&
                    !line.contains("@") &&
                    !line.contains("现金激励") &&
                    !line.contains("流量扶持") &&
                    !line.contains("商务合作") &&
                    !line.contains("未经著作权人许可")) {
                    cleanLyrics.append(line).append("\n");
                }
            }

            return cleanLyrics.toString().trim();

        } catch (Exception e) {
            log.error("清理LRC歌词时发生错误", e);
            return lrcText;
        }
    }

    /**
     * 通过歌曲标题和艺术家搜索歌词
     */
    private Map<String, Object> searchLyricsByTitle(String title, String artist) {
        try {
            log.info("尝试搜索歌词: title={}, artist={}", title, artist);

            return searchLyricsFromMultipleSources(title, artist);

        } catch (Exception e) {
            log.error("搜索歌词时发生错误", e);
            return createPlaceholderLyrics(title, artist);
        }
    }

    /**
     * 从多个音乐平台搜索歌词
     */
    private Map<String, Object> searchLyricsFromMultipleSources(String title, String artist) {
        try {
            log.info("开始从多个平台搜索歌词: title={}, artist={}", title, artist);

            // 1. 首先尝试网易云音乐搜索歌词
            Map<String, Object> neteaseResult = searchNeteaseCloudLyrics(title, artist);
            if (neteaseResult != null && !neteaseResult.isEmpty()) {
                log.info("从网易云音乐获取到歌词");
                return neteaseResult;
            }

            // 2. 尝试QQ音乐搜索歌词
            Map<String, Object> qqMusicResult = searchQQMusicLyrics(title, artist);
            if (qqMusicResult != null && !qqMusicResult.isEmpty()) {
                log.info("从QQ音乐获取到歌词");
                return qqMusicResult;
            }

            // 3. 如果所有平台都没有找到，返回占位符
            log.info("所有平台都未找到歌词，返回占位符");
            return createPlaceholderLyrics(title, artist);

        } catch (Exception e) {
            log.error("从多个平台搜索歌词时发生错误", e);
            return createPlaceholderLyrics(title, artist);
        }
    }

    /**
     * 创建占位符歌词
     */
    private Map<String, Object> createPlaceholderLyrics(String title, String artist) {
        Map<String, Object> result = new HashMap<>();
        result.put("lyrics", "暂未找到歌词\n\n♪ 正在播放: " + title +
                  (artist != null && !artist.trim().isEmpty() ? " - " + artist : "") + " ♪");
        result.put("type", "placeholder");
        result.put("synced", false);
        return result;
    }

    /**
     * 从QQ音乐搜索歌词
     */
    private Map<String, Object> searchQQMusicLyrics(String title, String artist) {
        try {
            log.info("正在从QQ音乐搜索歌词: title={}, artist={}", title, artist);

            // 构建搜索关键词
            String searchKeyword = title;
            if (artist != null && !artist.trim().isEmpty()) {
                searchKeyword = artist + " " + title;
            }

            // 第一步：搜索歌曲获取歌曲ID
            String songId = searchQQMusicSongId(searchKeyword, title, artist);
            if (songId == null || songId.isEmpty()) {
                log.info("未在QQ音乐找到匹配的歌曲");
                return null;
            }

            // 第二步：根据歌曲ID获取歌词
            return getQQMusicLyricsById(songId);

        } catch (Exception e) {
            log.error("从QQ音乐搜索歌词时发生错误", e);
            return null;
        }
    }

    /**
     * 搜索QQ音乐歌曲ID
     */
    private String searchQQMusicSongId(String searchKeyword, String title, String artist) {
        try {
            log.info("正在搜索QQ音乐歌曲ID: {}", searchKeyword);

            // QQ音乐搜索API
            String searchUrl = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp";
            String encodedKeyword = URLEncoder.encode(searchKeyword, StandardCharsets.UTF_8);
            String fullUrl = searchUrl + "?ct=24&qqmusic_ver=1298&new_json=1&remoteplace=txt.yqq.song&" +
                           "searchid=&t=0&aggr=1&cr=1&catZhida=1&lossless=0&flag_qc=0&p=1&n=10&" +
                           "w=" + encodedKeyword + "&g_tk=&loginUin=0&hostUin=0&format=json&inCharset=utf8&" +
                           "outCharset=utf-8&notice=0&platform=yqq.json&needNewCode=0";

            log.debug("QQ音乐搜索URL: {}", fullUrl);

            // 发送搜索请求
            Document doc = Jsoup.connect(fullUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://y.qq.com/")
                    .header("Origin", "https://y.qq.com")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .get();

            String jsonResponse = doc.body().text();
            log.debug("QQ音乐搜索响应: {}", jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse);

            // 解析JSON响应
            String songId = parseQQMusicSongIdFromJson(jsonResponse, title, artist);
            if (songId != null && !songId.isEmpty()) {
                log.info("找到匹配的QQ音乐歌曲ID: {}", songId);
                return songId;
            }

            log.info("未找到匹配的QQ音乐歌曲");
            return null;

        } catch (Exception e) {
            log.error("搜索QQ音乐歌曲ID时发生错误", e);
            return null;
        }
    }

    /**
     * 从QQ音乐搜索响应JSON中解析歌曲ID
     */
    private String parseQQMusicSongIdFromJson(String jsonResponse, String targetTitle, String targetArtist) {
        try {
            log.debug("开始解析QQ音乐搜索结果");

            // 使用Jackson解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // 检查响应结构 - QQ音乐的结构是 data.song.list
            if (!rootNode.has("data")) {
                log.debug("响应中未找到data字段");
                return null;
            }

            JsonNode dataNode = rootNode.get("data");
            if (!dataNode.has("song")) {
                log.debug("响应中未找到song字段");
                return null;
            }

            JsonNode songNode = dataNode.get("song");
            if (!songNode.has("list")) {
                log.debug("响应中未找到list字段");
                return null;
            }

            JsonNode listNode = songNode.get("list");
            if (!listNode.isArray()) {
                log.debug("list字段不是数组");
                return null;
            }

            // 遍历歌曲列表
            for (JsonNode songItemNode : listNode) {
                if (!songItemNode.has("songmid") || !songItemNode.has("songname")) {
                    continue;
                }

                String songId = songItemNode.get("songmid").asText();
                String songName = songItemNode.get("songname").asText();

                // 提取艺术家信息
                String artistsInfo = extractQQMusicArtistsFromJsonNode(songItemNode);

                log.debug("检查QQ音乐歌曲匹配: id={}, name={}, artists={}", songId, songName, artistsInfo);

                // 检查歌曲名称匹配
                if (isSongMatch(songName, artistsInfo, targetTitle, targetArtist)) {
                    log.info("找到匹配的QQ音乐歌曲: id={}, name={}, artists={}", songId, songName, artistsInfo);
                    return songId;
                }
            }

            log.debug("未找到匹配的QQ音乐歌曲");
            return null;

        } catch (Exception e) {
            log.error("解析QQ音乐搜索结果时发生错误", e);
            return null;
        }
    }

    /**
     * 从QQ音乐歌曲JsonNode中提取艺术家信息
     */
    private String extractQQMusicArtistsFromJsonNode(JsonNode songNode) {
        try {
            if (!songNode.has("singer")) {
                return "";
            }

            JsonNode singersNode = songNode.get("singer");
            if (!singersNode.isArray()) {
                return "";
            }

            StringBuilder artists = new StringBuilder();
            for (JsonNode singerNode : singersNode) {
                if (singerNode.has("name")) {
                    if (artists.length() > 0) {
                        artists.append("/");
                    }
                    artists.append(singerNode.get("name").asText());
                }
            }

            return artists.toString();

        } catch (Exception e) {
            log.debug("提取QQ音乐艺术家信息时发生错误: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从QQ音乐歌曲JSON对象中提取艺术家信息（旧方法，保留兼容性）
     */
    private String extractQQMusicArtistsFromSong(String songJson) {
        try {
            // 查找singer数组
            String singersPattern = "\"singer\"\\s*:\\s*\\[(.*?)\\]";
            Pattern pattern = Pattern.compile(singersPattern, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(songJson);

            if (!matcher.find()) {
                return "";
            }

            String singersArray = matcher.group(1);

            // 提取歌手名称
            StringBuilder artists = new StringBuilder();
            String namePattern = "\"name\"\\s*:\\s*\"([^\"]+)\"";
            Pattern nameP = Pattern.compile(namePattern);
            Matcher nameM = nameP.matcher(singersArray);

            while (nameM.find()) {
                if (artists.length() > 0) {
                    artists.append("/");
                }
                artists.append(nameM.group(1));
            }

            return artists.toString();

        } catch (Exception e) {
            log.debug("提取QQ音乐艺术家信息时发生错误: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从网易云音乐搜索歌词
     */
    private Map<String, Object> searchNeteaseCloudLyrics(String title, String artist) {
        try {
            log.info("正在从网易云音乐搜索歌词: title={}, artist={}", title, artist);

            // 构建搜索关键词
            String searchKeyword = title;
            if (artist != null && !artist.trim().isEmpty()) {
                searchKeyword = artist + " " + title;
            }

            // 第一步：搜索歌曲获取歌曲ID
            String songId = searchNeteaseSongId(searchKeyword, title, artist);
            if (songId == null || songId.isEmpty()) {
                log.info("未在网易云音乐找到匹配的歌曲");
                return null;
            }

            // 第二步：根据歌曲ID获取歌词
            return getNeteaseCloudLyricsById(songId);

        } catch (Exception e) {
            log.error("从网易云音乐搜索歌词时发生错误", e);
            return null;
        }
    }

    /**
     * 搜索网易云音乐歌曲ID
     */
    private String searchNeteaseSongId(String searchKeyword, String title, String artist) {
        try {
            log.info("正在搜索网易云音乐歌曲ID: {}", searchKeyword);

            // 网易云音乐搜索API（使用公开的搜索接口）
            String searchUrl = "https://music.163.com/api/search/get/web";
            String encodedKeyword = URLEncoder.encode(searchKeyword, StandardCharsets.UTF_8);
            String fullUrl = searchUrl + "?s=" + encodedKeyword + "&type=1&offset=0&total=true&limit=10";

            log.debug("网易云音乐搜索URL: {}", fullUrl);

            // 发送搜索请求
            Document doc = Jsoup.connect(fullUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://music.163.com/")
                    .header("Origin", "https://music.163.com")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .get();

            String jsonResponse = doc.body().text();
            log.debug("网易云音乐搜索响应: {}", jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse);

            // 解析JSON响应
            String songId = parseNeteaseSongIdFromJson(jsonResponse, title, artist);
            if (songId != null && !songId.isEmpty()) {
                log.info("找到匹配的网易云音乐歌曲ID: {}", songId);
                return songId;
            }

            log.info("未找到匹配的网易云音乐歌曲");
            return null;

        } catch (Exception e) {
            log.error("搜索网易云音乐歌曲ID时发生错误", e);
            return null;
        }
    }

    /**
     * 从网易云音乐搜索响应JSON中解析歌曲ID
     */
    private String parseNeteaseSongIdFromJson(String jsonResponse, String targetTitle, String targetArtist) {
        try {
            log.debug("开始解析网易云音乐搜索结果");

            // 使用Jackson解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // 检查响应结构
            if (!rootNode.has("result")) {
                log.debug("响应中未找到result字段");
                return null;
            }

            JsonNode resultNode = rootNode.get("result");
            if (!resultNode.has("songs")) {
                log.debug("响应中未找到songs字段");
                return null;
            }

            JsonNode songsNode = resultNode.get("songs");
            if (!songsNode.isArray()) {
                log.debug("songs字段不是数组");
                return null;
            }

            // 遍历歌曲列表
            for (JsonNode songNode : songsNode) {
                if (!songNode.has("id") || !songNode.has("name")) {
                    continue;
                }

                String songIdStr = songNode.get("id").asText();
                String songName = songNode.get("name").asText();

                // 提取艺术家信息
                String artistsInfo = extractNeteaseArtistsFromJsonNode(songNode);

                log.debug("检查网易云歌曲匹配: id={}, name={}, artists={}", songIdStr, songName, artistsInfo);

                // 检查歌曲名称匹配
                if (isSongMatch(songName, artistsInfo, targetTitle, targetArtist)) {
                    log.info("找到匹配的网易云歌曲: id={}, name={}, artists={}", songIdStr, songName, artistsInfo);
                    return songIdStr;
                }
            }

            log.debug("未找到匹配的网易云歌曲");
            return null;

        } catch (Exception e) {
            log.error("解析网易云音乐搜索结果时发生错误", e);
            return null;
        }
    }

    /**
     * 从网易云音乐歌曲JsonNode中提取艺术家信息
     */
    private String extractNeteaseArtistsFromJsonNode(JsonNode songNode) {
        try {
            if (!songNode.has("artists")) {
                return "";
            }

            JsonNode artistsNode = songNode.get("artists");
            if (!artistsNode.isArray()) {
                return "";
            }

            StringBuilder artists = new StringBuilder();
            for (JsonNode artistNode : artistsNode) {
                if (artistNode.has("name")) {
                    if (artists.length() > 0) {
                        artists.append("/");
                    }
                    artists.append(artistNode.get("name").asText());
                }
            }

            return artists.toString();

        } catch (Exception e) {
            log.debug("提取网易云艺术家信息时发生错误: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 提取JSON中的数字字段（不带引号）
     */
    private String extractJsonNumberField(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*(\\d+)";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.debug("提取JSON数字字段时发生错误: {}", fieldName, e);
        }
        return null;
    }

    /**
     * 更精确地分割JSON对象数组
     */
    private String[] splitJsonObjects(String jsonArray) {
        try {
            List<String> objects = new ArrayList<>();
            int braceCount = 0;
            int start = 0;

            for (int i = 0; i < jsonArray.length(); i++) {
                char c = jsonArray.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // 找到一个完整的对象
                        String obj = jsonArray.substring(start, i + 1);
                        objects.add(obj);

                        // 跳过逗号和空白字符，找到下一个对象的开始
                        i++;
                        while (i < jsonArray.length() && (jsonArray.charAt(i) == ',' || Character.isWhitespace(jsonArray.charAt(i)))) {
                            i++;
                        }
                        start = i;
                        i--; // 因为for循环会i++
                    }
                }
            }

            return objects.toArray(new String[0]);

        } catch (Exception e) {
            log.error("分割JSON对象时发生错误", e);
            // 回退到简单分割
            return jsonArray.split("\\},\\s*\\{");
        }
    }

    /**
     * 从网易云音乐歌曲JSON对象中提取艺术家信息
     */
    private String extractNeteaseArtistsFromSong(String songJson) {
        try {
            // 查找artists数组
            String artistsPattern = "\"artists\"\\s*:\\s*\\[(.*?)\\]";
            Pattern pattern = Pattern.compile(artistsPattern, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(songJson);

            if (!matcher.find()) {
                return "";
            }

            String artistsArray = matcher.group(1);

            // 提取艺术家名称
            StringBuilder artists = new StringBuilder();
            String namePattern = "\"name\"\\s*:\\s*\"([^\"]+)\"";
            Pattern nameP = Pattern.compile(namePattern);
            Matcher nameM = nameP.matcher(artistsArray);

            while (nameM.find()) {
                if (artists.length() > 0) {
                    artists.append("/");
                }
                artists.append(nameM.group(1));
            }

            return artists.toString();

        } catch (Exception e) {
            log.debug("提取网易云艺术家信息时发生错误: {}", e.getMessage());
            return "";
        }
    }



    /**
     * 检查歌曲是否匹配
     */
    private boolean isSongMatch(String songName, String artistsInfo, String targetTitle, String targetArtist) {
        try {
            // 标准化字符串（去除空格、转小写）
            String normalizedSongName = normalizeSongTitle(songName);
            String normalizedTargetTitle = normalizeSongTitle(targetTitle);
            String normalizedArtistsInfo = normalizeArtistName(artistsInfo);
            String normalizedTargetArtist = normalizeArtistName(targetArtist);

            // 检查歌曲名称匹配
            boolean titleMatch = normalizedSongName.contains(normalizedTargetTitle) ||
                               normalizedTargetTitle.contains(normalizedSongName) ||
                               calculateSimilarity(normalizedSongName, normalizedTargetTitle) > 0.8;

            // 如果没有提供艺术家信息，只检查标题
            if (targetArtist == null || targetArtist.trim().isEmpty()) {
                return titleMatch;
            }

            // 检查艺术家匹配
            boolean artistMatch = normalizedArtistsInfo.contains(normalizedTargetArtist) ||
                                normalizedTargetArtist.contains(normalizedArtistsInfo) ||
                                calculateSimilarity(normalizedArtistsInfo, normalizedTargetArtist) > 0.7;

            return titleMatch && artistMatch;

        } catch (Exception e) {
            log.debug("检查歌曲匹配时发生错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据歌曲ID获取QQ音乐歌词
     */
    private Map<String, Object> getQQMusicLyricsById(String songId) {
        try {
            log.info("正在获取QQ音乐歌词: songId={}", songId);

            // QQ音乐歌词API
            String lyricsUrl = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg" +
                             "?callback=MusicJsonCallback_lrc&pcachetime=" + System.currentTimeMillis() +
                             "&songmid=" + songId + "&g_tk=&loginUin=0&hostUin=0&format=jsonp&" +
                             "inCharset=utf8&outCharset=utf-8&notice=0&platform=yqq&needNewCode=0";

            log.debug("QQ音乐歌词URL: {}", lyricsUrl);

            // 发送歌词请求
            Document doc = Jsoup.connect(lyricsUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "*/*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://y.qq.com/")
                    .header("Origin", "https://y.qq.com")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .get();

            String jsonpResponse = doc.body().text();
            log.debug("QQ音乐歌词响应: {}", jsonpResponse.length() > 200 ? jsonpResponse.substring(0, 200) + "..." : jsonpResponse);

            // 解析JSONP响应
            return parseQQMusicLyricsFromJsonp(jsonpResponse);

        } catch (Exception e) {
            log.error("获取QQ音乐歌词时发生错误", e);
            return null;
        }
    }

    /**
     * 解析QQ音乐歌词JSONP响应
     */
    private Map<String, Object> parseQQMusicLyricsFromJsonp(String jsonpResponse) {
        try {
            // 移除JSONP包装，提取JSON内容
            String jsonContent = extractJsonFromJsonp(jsonpResponse);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                log.info("QQ音乐JSONP响应中未找到JSON内容");
                return null;
            }

            // 使用Jackson解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            Map<String, Object> result = new HashMap<>();

            // 提取Base64编码的歌词
            String base64Lyrics = null;
            if (rootNode.has("lyric")) {
                base64Lyrics = rootNode.get("lyric").asText();
            }

            if (base64Lyrics != null && !base64Lyrics.trim().isEmpty()) {
                // 解码Base64歌词
                String decodedLyrics = decodeBase64Lyrics(base64Lyrics);

                if (decodedLyrics != null && !decodedLyrics.trim().isEmpty()) {
                    // 清理和处理歌词
                    String cleanedLyrics = cleanQQMusicLyrics(decodedLyrics);

                    if (!cleanedLyrics.trim().isEmpty()) {
                        result.put("lyrics", cleanedLyrics);
                        result.put("type", "lrc");
                        result.put("synced", true);
                        result.put("source", "qqmusic");

                        // 尝试获取翻译歌词
                        String base64Translation = null;
                        if (rootNode.has("trans")) {
                            base64Translation = rootNode.get("trans").asText();
                        }

                        if (base64Translation != null && !base64Translation.trim().isEmpty()) {
                            String decodedTranslation = decodeBase64Lyrics(base64Translation);
                            if (decodedTranslation != null && !decodedTranslation.trim().isEmpty()) {
                                String cleanedTranslation = cleanQQMusicLyrics(decodedTranslation);
                                if (!cleanedTranslation.trim().isEmpty()) {
                                    result.put("translation", cleanedTranslation);
                                }
                            }
                        }

                        log.info("成功解析QQ音乐歌词，长度: {}", cleanedLyrics.length());
                        return result;
                    }
                }
            }

            log.info("QQ音乐响应中未找到有效歌词");
            return null;

        } catch (Exception e) {
            log.error("解析QQ音乐歌词JSONP时发生错误", e);
            return null;
        }
    }

    /**
     * 从JSONP响应中提取JSON内容
     */
    private String extractJsonFromJsonp(String jsonpResponse) {
        try {
            // QQ音乐的JSONP格式: MusicJsonCallback_lrc({...})
            if (jsonpResponse.contains("MusicJsonCallback_lrc(")) {
                int start = jsonpResponse.indexOf("MusicJsonCallback_lrc(") + "MusicJsonCallback_lrc(".length();
                int end = jsonpResponse.lastIndexOf(")");
                if (start < end) {
                    return jsonpResponse.substring(start, end);
                }
            }

            // 通用JSONP格式处理
            int start = jsonpResponse.indexOf("(");
            int end = jsonpResponse.lastIndexOf(")");
            if (start >= 0 && end > start) {
                return jsonpResponse.substring(start + 1, end);
            }

            return jsonpResponse;

        } catch (Exception e) {
            log.error("提取JSONP中的JSON内容时发生错误", e);
            return null;
        }
    }

    /**
     * 解码Base64歌词
     */
    private String decodeBase64Lyrics(String base64Lyrics) {
        try {
            if (base64Lyrics == null || base64Lyrics.trim().isEmpty()) {
                return null;
            }

            // 解码Base64
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Lyrics);
            return new String(decodedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("解码Base64歌词时发生错误", e);
            return null;
        }
    }

    /**
     * 清理QQ音乐歌词
     */
    private String cleanQQMusicLyrics(String lrcText) {
        if (lrcText == null || lrcText.trim().isEmpty()) {
            return "";
        }

        try {
            // 分行处理
            String[] lines = lrcText.split("\n");
            StringBuilder cleanLyrics = new StringBuilder();

            for (String line : lines) {
                line = line.trim();

                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }

                // 跳过制作信息和元数据
                if (line.contains("作词") || line.contains("作曲") || line.contains("编曲") ||
                    line.contains("制作") || line.contains("混音") || line.contains("监制") ||
                    line.contains("出品") || line.contains("发行") || line.contains("录音") ||
                    line.contains("母带") || line.contains("吉他") || line.contains("贝斯") ||
                    line.contains("鼓") || line.contains("键盘") || line.contains("和声") ||
                    line.contains("弦乐") || line.contains("制作人") || line.contains("统筹") ||
                    line.contains("QQ音乐") || line.contains("腾讯") ||
                    line.contains("版权所有") || line.contains("未经许可") ||
                    line.contains("商务合作") || line.contains("客服电话")) {
                    continue;
                }

                // 跳过纯时间标签行（没有歌词内容）
                if (line.matches("^\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]\\s*$")) {
                    continue;
                }

                cleanLyrics.append(line).append("\n");
            }

            return cleanLyrics.toString().trim();

        } catch (Exception e) {
            log.error("清理QQ音乐歌词时发生错误", e);
            return lrcText;
        }
    }

    /**
     * 根据歌曲ID获取网易云音乐歌词
     */
    private Map<String, Object> getNeteaseCloudLyricsById(String songId) {
        try {
            log.info("正在获取网易云音乐歌词: songId={}", songId);

            // 网易云音乐歌词API
            String lyricsUrl = "https://music.163.com/api/song/lyric?id=" + songId + "&lv=1&kv=1&tv=-1";

            log.debug("网易云音乐歌词URL: {}", lyricsUrl);

            // 发送歌词请求
            Document doc = Jsoup.connect(lyricsUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://music.163.com/")
                    .header("Origin", "https://music.163.com")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .get();

            String jsonResponse = doc.body().text();
            log.debug("网易云音乐歌词响应: {}", jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);

            // 解析歌词JSON
            return parseNeteaseLyricsFromJson(jsonResponse);

        } catch (Exception e) {
            log.error("获取网易云音乐歌词时发生错误", e);
            return null;
        }
    }

    /**
     * 解析网易云音乐歌词JSON响应
     */
    private Map<String, Object> parseNeteaseLyricsFromJson(String jsonResponse) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 使用Jackson解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // 提取普通歌词
            String lrcLyrics = null;
            if (rootNode.has("lrc") && rootNode.get("lrc").has("lyric")) {
                lrcLyrics = rootNode.get("lrc").get("lyric").asText();
            }

            // 提取翻译歌词（如果有）
            String translatedLyrics = null;
            if (rootNode.has("tlyric") && rootNode.get("tlyric").has("lyric")) {
                translatedLyrics = rootNode.get("tlyric").get("lyric").asText();
            }

            if (lrcLyrics != null && !lrcLyrics.trim().isEmpty()) {
                // 清理和处理歌词
                String cleanedLyrics = cleanNeteaseLyrics(lrcLyrics);

                if (!cleanedLyrics.trim().isEmpty()) {
                    result.put("lyrics", cleanedLyrics);
                    result.put("type", "lrc");
                    result.put("synced", true);
                    result.put("source", "netease");

                    // 如果有翻译，也添加进去
                    if (translatedLyrics != null && !translatedLyrics.trim().isEmpty()) {
                        String cleanedTranslation = cleanNeteaseLyrics(translatedLyrics);
                        if (!cleanedTranslation.trim().isEmpty()) {
                            result.put("translation", cleanedTranslation);
                        }
                    }

                    log.info("成功解析网易云音乐歌词，长度: {}", cleanedLyrics.length());
                    return result;
                }
            }

            log.info("网易云音乐响应中未找到有效歌词");
            return null;

        } catch (Exception e) {
            log.error("解析网易云音乐歌词JSON时发生错误", e);
            return null;
        }
    }

    /**
     * 清理网易云音乐歌词
     */
    private String cleanNeteaseLyrics(String lrcText) {
        if (lrcText == null || lrcText.trim().isEmpty()) {
            return "";
        }

        try {
            // 解码JSON字符串中的转义字符
            String decodedLyrics = decodeJsonString(lrcText);

            // 分行处理
            String[] lines = decodedLyrics.split("\n");
            StringBuilder cleanLyrics = new StringBuilder();

            for (String line : lines) {
                line = line.trim();

                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }

                // 跳过制作信息和元数据
                if (line.contains("作词") || line.contains("作曲") || line.contains("编曲") ||
                    line.contains("制作") || line.contains("混音") || line.contains("监制") ||
                    line.contains("出品") || line.contains("发行") || line.contains("录音") ||
                    line.contains("母带") || line.contains("吉他") || line.contains("贝斯") ||
                    line.contains("鼓") || line.contains("键盘") || line.contains("和声") ||
                    line.contains("弦乐") || line.contains("制作人") || line.contains("统筹") ||
                    line.contains("@") || line.contains("网易云音乐") ||
                    line.contains("现金激励") || line.contains("流量扶持") ||
                    line.contains("商务合作") || line.contains("未经著作权人许可")) {
                    continue;
                }

                // 跳过纯时间标签行（没有歌词内容）
                if (line.matches("^\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]\\s*$")) {
                    continue;
                }

                cleanLyrics.append(line).append("\n");
            }

            return cleanLyrics.toString().trim();

        } catch (Exception e) {
            log.error("清理网易云音乐歌词时发生错误", e);
            return lrcText;
        }
    }

    /**
     * 标准化歌曲标题（用于匹配）
     */
    private String normalizeSongTitle(String title) {
        if (title == null) return "";

        return title.toLowerCase()
                .replaceAll("[\\s\\-_]+", "")  // 移除空格、横线、下划线
                .replaceAll("[()（）\\[\\]【】]", "")  // 移除括号
                .replaceAll("[.,，。！!？?]", "")  // 移除标点符号
                .trim();
    }

    /**
     * 标准化艺术家名称（用于匹配）
     */
    private String normalizeArtistName(String artist) {
        if (artist == null) return "";

        return artist.toLowerCase()
                .replaceAll("[\\s\\-_/、&]+", "")  // 移除分隔符
                .replaceAll("[()（）\\[\\]【】]", "")  // 移除括号
                .trim();
    }

    /**
     * 计算字符串相似度（简单的编辑距离算法）
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;

        int editDistance = calculateEditDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLength;
    }

    /**
     * 计算编辑距离
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[m][n];
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

    /**
     * 使用yt-dlp流式下载音频到输出流（推荐使用，对服务器友好）
     */
    public boolean downloadAudioStreamWithYtDlp(String videoUrl, String platform, Map<String, Object> selectedFormat, OutputStream outputStream) {
        File tempCookieFile = null;
        try {
            log.info("使用yt-dlp流式下载音频: {}", videoUrl);

            // 根据平台获取对应的cookie配置
            String cookieValue = getCookieByPlatform(platform);

            // 构建yt-dlp下载命令
            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("--no-playlist");       // 不处理播放列表
            command.add("--no-warnings");       // 减少警告输出
            command.add("--prefer-ffmpeg");     // 优先使用ffmpeg进行后处理
            command.add("--embed-metadata");    // 嵌入元数据

            // 添加cookie支持
            if (cookieValue != null && !cookieValue.trim().isEmpty()) {
                try {
                    // 创建临时cookie文件
                    tempCookieFile = File.createTempFile("yt-dlp-cookies-", ".txt");
                    Files.write(tempCookieFile.toPath(), cookieValue.getBytes(StandardCharsets.UTF_8));
                    command.add("--cookies");
                    command.add(tempCookieFile.getAbsolutePath());
                    log.debug("使用cookie文件: {}", tempCookieFile.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("创建cookie文件失败，将不使用cookie: {}", e.getMessage());
                }
            }

            // 根据选择的格式设置下载参数
            if (selectedFormat != null) {
                String formatId = (String) selectedFormat.get("formatId");
                Boolean isAudio = (Boolean) selectedFormat.get("isAudio");
                Boolean isVideo = (Boolean) selectedFormat.get("isVideo");
                Boolean isMerged = (Boolean) selectedFormat.get("isMerged");

                if (isMerged != null && isMerged) {
                    // 合并下载：下载最佳音频和视频并合并
                    command.add("--format");
                    // 使用更好的格式选择策略，优先选择mp4容器的高质量流
                    command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best[ext=mp4]/best");
                    // 确保输出为mp4格式
                    command.add("--merge-output-format");
                    command.add("mp4");
                    // 添加重新编码选项以确保兼容性
                    command.add("--recode-video");
                    command.add("mp4");
                    log.info("使用合并格式流式下载: 最佳音频+视频 -> MP4");
                } else if (formatId != null && !formatId.isEmpty()) {
                    command.add("--format");
                    command.add(formatId);
                    log.info("使用指定格式ID: {}", formatId);
                } else if (Boolean.TRUE.equals(isAudio)) {
                    command.add("--format");
                    command.add("bestaudio/best");
                    log.info("使用最佳音频格式");
                } else if (Boolean.TRUE.equals(isVideo)) {
                    command.add("--format");
                    command.add("best");
                    log.info("使用最佳视频格式");
                } else {
                    command.add("--format");
                    command.add("bestaudio/best");
                    log.info("默认使用最佳音频格式");
                }
            } else {
                command.add("--format");
                command.add("bestaudio/best");
                log.info("未指定格式，使用最佳音频格式");
            }

            // 检查是否需要合并下载
            boolean needsMerging = selectedFormat != null &&
                                 Boolean.TRUE.equals(selectedFormat.get("isMerged"));

            String tempFileName = null;
            if (needsMerging) {
                // 合并下载需要使用临时文件，不能直接输出到stdout
                // 创建临时文件路径
                tempFileName = "temp_download_" + System.currentTimeMillis();
                command.add("--output");
                command.add(tempFileName + ".%(ext)s");
                log.info("合并下载使用临时文件: {}", tempFileName);
            } else {
                // 非合并下载可以直接输出到stdout
                command.add("--output");
                command.add("-");  // 输出到stdout
                log.info("使用流式输出到stdout");
            }

            // 添加视频URL
            command.add(videoUrl);

            log.info("执行yt-dlp流式下载命令: {}", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(false); // 分离错误流和输出流
            Process process = processBuilder.start();

            // 在单独的线程中读取stderr流，区分进度信息和错误信息
            Thread stderrReaderThread = new Thread(() -> {
                try (BufferedReader stderrReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        // 区分不同类型的输出
                        if (line.contains("[download]")) {
                            // 下载进度信息，提取进度百分比
                            if (line.contains("%")) {
                                try {
                                    // 提取进度百分比，格式如：[download]   7.2% of   60.95MiB
                                    String[] parts = line.split("\\s+");
                                    for (String part : parts) {
                                        if (part.endsWith("%")) {
                                            String percentStr = part.replace("%", "");
                                            double percent = Double.parseDouble(percentStr);
                                            log.debug("yt-dlp下载进度: {}%", String.format("%.1f", percent));
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    // 如果解析失败，只记录原始信息
                                    log.debug("yt-dlp下载进度: {}", line);
                                }
                            } else {
                                log.debug("yt-dlp下载信息: {}", line);
                            }
                        } else if (line.toLowerCase().contains("error") ||
                                   line.toLowerCase().contains("failed")) {
                            // 真正的错误
                            log.error("yt-dlp错误: {}", line);
                        } else if (line.toLowerCase().contains("warning")) {
                            // 警告信息
                            log.warn("yt-dlp警告: {}", line);
                        } else {
                            // 其他信息（如格式信息等）
                            log.debug("yt-dlp信息: {}", line);
                        }
                    }
                } catch (IOException e) {
                    log.warn("读取yt-dlp stderr流失败: {}", e.getMessage());
                }
            });
            stderrReaderThread.start();

            // 根据是否需要合并来处理输出
            if (needsMerging) {
                // 合并下载：等待下载完成后再传输文件
                log.info("等待合并下载完成...");
                // 这里不读取stdout，因为输出到了文件
            } else {
                // 非合并下载：直接传输stdout流
                try (InputStream inputStream = process.getInputStream()) {
                    byte[] buffer = new byte[8192]; // 8KB缓冲区
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush(); // 确保数据立即发送
                        totalBytes += bytesRead;

                        if (totalBytes % (1024 * 1024) == 0) { // 每1MB记录一次
                            log.debug("已传输 {} MB", totalBytes / (1024 * 1024));
                        }
                    }

                    log.info("流式下载完成，总共传输: {} bytes", totalBytes);
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();

            // 等待stderr读取线程完成
            stderrReaderThread.join(5000); // 最多等待5秒

            if (exitCode == 0) {
                if (needsMerging) {
                    // 合并下载完成后，需要找到生成的文件并传输
                    return transferMergedFile(outputStream, tempFileName);
                } else {
                    log.info("yt-dlp流式下载成功完成");
                    return true;
                }
            } else {
                log.error("yt-dlp流式下载失败，退出码: {}", exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("使用yt-dlp流式下载音频时发生错误", e);
            return false;
        } finally {
            // 清理临时cookie文件
            if (tempCookieFile != null && tempCookieFile.exists()) {
                try {
                    tempCookieFile.delete();
                    log.debug("清理临时cookie文件: {}", tempCookieFile.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("清理临时cookie文件失败: {}", tempCookieFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * 传输合并下载的临时文件到输出流
     */
    private boolean transferMergedFile(OutputStream outputStream, String tempFileName) {
        try {
            // 查找生成的临时文件
            File currentDir = new File(".");
            File[] files = currentDir.listFiles((dir, name) ->
                name.startsWith(tempFileName) &&
                (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm")));

            if (files == null || files.length == 0) {
                log.error("未找到合并下载的临时文件: {}", tempFileName);
                return false;
            }

            // 通常只会有一个文件，取第一个
            File tempFile = files[0];

            if (!tempFile.exists()) {
                log.error("临时文件不存在: {}", tempFile.getName());
                return false;
            }

            log.info("开始传输合并文件: {}, 大小: {} bytes", tempFile.getName(), tempFile.length());

            // 传输文件内容到输出流
            try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                    totalBytes += bytesRead;

                    if (totalBytes % (1024 * 1024) == 0) {
                        log.debug("已传输 {} MB", totalBytes / (1024 * 1024));
                    }
                }

                log.info("合并文件传输完成，总共传输: {} bytes", totalBytes);
            }

            // 清理临时文件
            try {
                if (tempFile.delete()) {
                    log.info("临时文件清理成功: {}", tempFile.getName());
                } else {
                    log.warn("临时文件清理失败: {}", tempFile.getName());
                }
            } catch (Exception e) {
                log.warn("清理临时文件时发生错误: {}", tempFile.getName(), e);
            }

            return true;

        } catch (Exception e) {
            log.error("传输合并文件时发生错误", e);
            return false;
        }
    }

    /**
     * 使用yt-dlp下载音频到服务器
     */
    public boolean downloadAudioWithYtDlp(String videoUrl, String platform, Map<String, Object> selectedFormat, Path outputPath) {
        File tempCookieFile = null;
        try {
            log.info("使用yt-dlp下载音频: {} -> {}", videoUrl, outputPath);

            // 根据平台获取对应的cookie配置
            String cookieValue = getCookieByPlatform(platform);

            // 构建yt-dlp下载命令
            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("--no-playlist");       // 不处理播放列表
            command.add("--no-warnings");       // 减少警告输出
            command.add("--prefer-ffmpeg");     // 优先使用ffmpeg进行后处理
            command.add("--embed-metadata");    // 嵌入元数据

            // 如果有cookie，创建临时cookie文件
            if (cookieValue != null && !cookieValue.trim().isEmpty()) {
                try {
                    tempCookieFile = createTempCookieFile(cookieValue.trim(), platform);
                    command.add("--cookies");
                    command.add(tempCookieFile.getAbsolutePath());
                    log.info("使用{}平台cookie文件进行下载", platform);
                } catch (Exception e) {
                    log.error("创建{}平台cookie文件失败，使用默认方式下载", platform, e);
                }
            } else {
                log.info("{}平台未配置cookie，使用默认方式下载", platform);
            }

            // 处理格式选择
            if (selectedFormat != null) {
                String formatId = (String) selectedFormat.get("formatId");
                Boolean isMerged = (Boolean) selectedFormat.get("isMerged");

                if (isMerged != null && isMerged) {
                    // 合并下载：下载最佳音频和视频并合并
                    command.add("--format");
                    // 使用更好的格式选择策略，优先选择mp4容器的高质量流
                    command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best[ext=mp4]/best");
                    // 确保输出为mp4格式
                    command.add("--merge-output-format");
                    command.add("mp4");
                    // 添加重新编码选项以确保兼容性
                    command.add("--recode-video");
                    command.add("mp4");
                    log.info("使用合并格式下载: 最佳音频+视频 -> MP4");
                } else if (formatId != null && !formatId.trim().isEmpty()) {
                    command.add("--format");
                    command.add(formatId);
                    log.info("使用指定格式下载: {}", formatId);
                } else {
                    // 如果没有formatId，使用最佳音频格式
                    command.add("--format");
                    command.add("bestaudio/best");
                    log.info("使用最佳音频格式下载");
                }
            } else {
                // 默认使用最佳音频格式
                command.add("--format");
                command.add("bestaudio/best");
                log.info("使用默认最佳音频格式下载");
            }

            // 设置输出文件路径 - 先下载到临时文件，然后重命名为指定文件名
            // 获取文件名（不含扩展名）和目录
            String fileName = outputPath.getFileName().toString();
            String baseFileName = fileName;
            if (fileName.contains(".")) {
                baseFileName = fileName.substring(0, fileName.lastIndexOf("."));
            }

            // 创建临时输出模板：目录/临时文件名.%(ext)s
            String tempFileName = baseFileName + "_temp";
            String tempOutputTemplate = outputPath.getParent().toString() + "/" + tempFileName + ".%(ext)s";
            command.add("--output");
            command.add(tempOutputTemplate);
            log.info("使用临时输出模板: {}", tempOutputTemplate);

            // 添加视频URL
            command.add(videoUrl);

            log.info("执行yt-dlp下载命令: {}", String.join(" ", command));

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
                    log.debug("yt-dlp输出: {}", line);
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // 查找临时下载的文件并重命名为指定文件名
                String originalFileName = outputPath.getFileName().toString();
                String tempBaseFileName;
                if (originalFileName.contains(".")) {
                    tempBaseFileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
                } else {
                    tempBaseFileName = originalFileName;
                }

                Path parentDir = outputPath.getParent();
                String tempFilePrefix = tempBaseFileName + "_temp.";

                try {
                    if (Files.exists(parentDir)) {
                        // 查找临时下载的文件
                        Path tempFile = Files.list(parentDir)
                            .filter(path -> {
                                String name = path.getFileName().toString();
                                return name.startsWith(tempFilePrefix);
                            })
                            .findFirst()
                            .orElse(null);

                        if (tempFile != null) {
                            log.info("找到临时下载文件: {}", tempFile);

                            // 重命名为指定的文件名
                            try {
                                Files.move(tempFile, outputPath);
                                log.info("文件重命名成功: {} -> {}", tempFile.getFileName(), outputPath.getFileName());
                                return true;
                            } catch (Exception e) {
                                log.error("文件重命名失败: {} -> {}", tempFile, outputPath, e);
                                return false;
                            }
                        } else {
                            log.warn("未找到临时下载文件，前缀: {}", tempFilePrefix);
                            return false;
                        }
                    } else {
                        log.warn("下载目录不存在: {}", parentDir);
                        return false;
                    }
                } catch (Exception e) {
                    log.error("处理下载文件时发生错误", e);
                    return false;
                }
            } else {
                log.error("yt-dlp下载失败，退出码: {}, 输出: {}", exitCode, output.toString());
                return false;
            }

        } catch (Exception e) {
            log.error("使用yt-dlp下载音频时发生错误", e);
            return false;
        } finally {
            // 清理临时cookie文件
            if (tempCookieFile != null && tempCookieFile.exists()) {
                try {
                    tempCookieFile.delete();
                    log.debug("清理临时cookie文件: {}", tempCookieFile.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("清理临时cookie文件失败: {}", tempCookieFile.getAbsolutePath(), e);
                }
            }
        }
    }
}
