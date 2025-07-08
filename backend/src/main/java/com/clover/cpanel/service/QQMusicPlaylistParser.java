package com.clover.cpanel.service;

import com.clover.cpanel.dto.PlaylistInfoDTO;
import com.clover.cpanel.dto.PlaylistSongDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QQ音乐歌单解析器
 */
@Slf4j
@Service
public class QQMusicPlaylistParser {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36"
    };
    
    /**
     * 验证是否为有效的QQ音乐歌单链接
     */
    public boolean validateUrl(String url) {
        return url != null && url.contains("y.qq.com") && 
               (url.contains("/playlist/") || url.contains("songlist"));
    }
    
    /**
     * 从URL中提取歌单ID
     */
    public String extractPlaylistId(String url) {
        try {
            // 新版链接: https://y.qq.com/n/ryqq/playlist/8668419170
            if (url.contains("/playlist/")) {
                String[] parts = url.split("/playlist/");
                if (parts.length > 1) {
                    return parts[1].split("\\?")[0];
                }
            }
            
            // 旧版链接: https://y.qq.com/n/yqq/playlist/7266910918.html
            if (url.contains("yqq/playlist/")) {
                Pattern pattern = Pattern.compile("playlist/(\\d+)");
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            
            // 分享链接中的id参数
            Pattern idPattern = Pattern.compile("[?&]id=([^&]+)");
            Matcher idMatcher = idPattern.matcher(url);
            if (idMatcher.find()) {
                return idMatcher.group(1);
            }
            
        } catch (Exception e) {
            log.error("提取歌单ID时发生错误", e);
        }
        
        return null;
    }
    
    /**
     * 获取随机User-Agent
     */
    private String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }
    
    /**
     * 解析QQ音乐歌单
     */
    public PlaylistInfoDTO parsePlaylist(String url) {
        if (!validateUrl(url)) {
            throw new RuntimeException("无效的QQ音乐歌单链接");
        }
        
        String playlistId = extractPlaylistId(url);
        if (playlistId == null) {
            throw new RuntimeException("无法提取歌单ID");
        }
        
        try {
            return fetchPlaylistFromApi(playlistId, url);
        } catch (Exception e) {
            log.error("解析QQ音乐歌单失败", e);
            throw new RuntimeException("解析QQ音乐歌单失败: " + e.getMessage());
        }
    }
    
    /**
     * 通过API获取歌单信息
     */
    private PlaylistInfoDTO fetchPlaylistFromApi(String playlistId, String originalUrl) throws IOException {
        String apiUrl = String.format(
            "https://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg?type=1&json=1&utf8=1&disstid=%s&format=json",
            URLEncoder.encode(playlistId, StandardCharsets.UTF_8)
        );
        
        log.info("正在请求QQ音乐API: {}", apiUrl);
        
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求头
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", getRandomUserAgent());
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        connection.setRequestProperty("Referer", "https://y.qq.com/n/ryqq/playlist");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("API请求失败，状态码: " + responseCode);
        }
        
        // 读取响应
        String response = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.debug("API响应: {}", response.substring(0, Math.min(200, response.length())));
        
        return parseApiResponse(response, originalUrl);
    }

    /**
     * 解析API响应
     */
    private PlaylistInfoDTO parseApiResponse(String response, String originalUrl) throws IOException {
        JsonNode rootNode = objectMapper.readTree(response);

        if (!rootNode.has("cdlist") || rootNode.get("cdlist").size() == 0) {
            throw new RuntimeException("API响应中没有找到歌单信息");
        }

        JsonNode cdlist = rootNode.get("cdlist").get(0);

        // 提取歌单基本信息
        String title = cdlist.has("dissname") ? cdlist.get("dissname").asText() : "未知歌单";
        String creator = cdlist.has("nickname") ? cdlist.get("nickname").asText() : "未知用户";
        String cover = cdlist.has("logo") ? cdlist.get("logo").asText() : "";
        String description = cdlist.has("desc") ? cdlist.get("desc").asText() : "";

        // 解析歌曲列表
        List<PlaylistSongDTO> songs = new ArrayList<>();
        if (cdlist.has("songlist")) {
            JsonNode songlist = cdlist.get("songlist");
            for (JsonNode songNode : songlist) {
                PlaylistSongDTO song = parseSong(songNode, title);
                if (song != null) {
                    songs.add(song);
                }
            }
        }

        return PlaylistInfoDTO.builder()
                .title(title)
                .creator(creator)
                .cover(cover)
                .url(originalUrl)
                .source("qq")
                .songs(songs)
                .songCount(songs.size())
                .description(description)
                .build();
    }

    /**
     * 解析单首歌曲信息
     */
    private PlaylistSongDTO parseSong(JsonNode songNode, String playlistName) {
        try {
            String songName = songNode.has("songname") ? songNode.get("songname").asText() : "未知歌曲";
            String songId = songNode.has("songid") ? songNode.get("songid").asText() : "0";
            String mid = songNode.has("songmid") ? songNode.get("songmid").asText() : "";
            String albumName = songNode.has("albumname") ? songNode.get("albumname").asText() : "未知专辑";
            String albumMid = songNode.has("albummid") ? songNode.get("albummid").asText() : "";

            // 检测VIP状态 - QQ音乐的多种VIP标识
            boolean vip = false;
            if (songNode.has("pay")) {
                JsonNode payNode = songNode.get("pay");
                // 检查付费下载
                if (payNode.has("paydownload") && payNode.get("paydownload").asInt() > 0) {
                    vip = true;
                }
                // 检查付费播放
                if (payNode.has("payplay") && payNode.get("payplay").asInt() > 0) {
                    vip = true;
                }
                // 检查付费状态
                if (payNode.has("payinfo") && payNode.get("payinfo").asInt() > 0) {
                    vip = true;
                }
            }

            // 检查其他VIP标识字段
            if (songNode.has("switch") && songNode.get("switch").asInt() > 0) {
                vip = true;
            }

            // 检查试听标识
            if (songNode.has("preview") && songNode.get("preview").has("trybegin") &&
                songNode.get("preview").get("trybegin").asInt() > 0) {
                vip = true;
            }
            // 获取歌手信息
            List<String> singers = new ArrayList<>();
            if (songNode.has("singer")) {
                JsonNode singerArray = songNode.get("singer");
                for (JsonNode singerNode : singerArray) {
                    if (singerNode.has("name")) {
                        singers.add(singerNode.get("name").asText());
                    }
                }
            }
            String artist = singers.isEmpty() ? "未知歌手" : String.join(", ", singers);

            // 构建歌曲URL
            String songUrl = String.format("https://y.qq.com/n/ryqq/songDetail/%s", mid);

            // 构建专辑封面URL
            String coverUrl = String.format("https://y.qq.com/music/photo_new/T002R300x300M000%s.jpg", albumMid);

            return PlaylistSongDTO.builder()
                    .title(songName)
                    .artist(artist)
                    .album(albumName)
                    .url(songUrl)
                    .cover(coverUrl)
                    .source("qq")
                    .sourceId(songId)
                    .playlistName(playlistName)
                    .vip(vip)
                    .build();

        } catch (Exception e) {
            log.error("解析歌曲信息时发生错误", e);
            return null;
        }
    }
}
