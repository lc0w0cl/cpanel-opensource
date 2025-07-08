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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网易云音乐歌单解析器
 */
@Slf4j
@Service
public class NetEasePlaylistParser {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15"
    };
    
    /**
     * 验证是否为有效的网易云音乐歌单链接
     */
    public boolean validateUrl(String url) {
        return url != null && url.contains("music.163.com") && 
               (url.contains("/playlist") || url.contains("/discover/playlist"));
    }
    
    /**
     * 从URL中提取歌单ID
     */
    public String extractPlaylistId(String url) {
        try {
            // 标准链接: https://music.163.com/#/playlist?id=123456
            Pattern idPattern = Pattern.compile("[?&]id=([^&]+)");
            Matcher idMatcher = idPattern.matcher(url);
            if (idMatcher.find()) {
                return idMatcher.group(1);
            }
            
            // 新版链接: https://music.163.com/playlist/123456/
            Pattern pathPattern = Pattern.compile("/playlist/(\\d+)");
            Matcher pathMatcher = pathPattern.matcher(url);
            if (pathMatcher.find()) {
                return pathMatcher.group(1);
            }
            
        } catch (Exception e) {
            log.error("提取网易云歌单ID时发生错误", e);
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
     * 解析网易云音乐歌单
     */
    public PlaylistInfoDTO parsePlaylist(String url) {
        if (!validateUrl(url)) {
            throw new RuntimeException("无效的网易云音乐歌单链接");
        }
        
        String playlistId = extractPlaylistId(url);
        if (playlistId == null) {
            throw new RuntimeException("无法提取歌单ID");
        }
        
        try {
            return fetchPlaylistFromApi(playlistId, url);
        } catch (Exception e) {
            log.error("解析网易云音乐歌单失败", e);
            throw new RuntimeException("解析网易云音乐歌单失败: " + e.getMessage());
        }
    }
    
    /**
     * 通过API获取歌单信息
     */
    private PlaylistInfoDTO fetchPlaylistFromApi(String playlistId, String originalUrl) throws IOException {
        // 网易云音乐API (注意：实际使用时可能需要处理反爬虫机制)
        String apiUrl = String.format("https://music.163.com/api/playlist/detail?id=%s", playlistId);
        
        log.info("正在请求网易云音乐API: {}", apiUrl);
        
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求头
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", getRandomUserAgent());
        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        connection.setRequestProperty("Referer", "https://music.163.com/");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        // 添加包含os=pc的Cookie
        connection.setRequestProperty("Cookie", "os=pc; appver=2.9.7; __csrf=; MUSIC_U=");
        
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
        
        if (!rootNode.has("result")) {
            throw new RuntimeException("API响应中没有找到歌单信息");
        }
        
        JsonNode result = rootNode.get("result");
        
        // 提取歌单基本信息
        String title = result.has("name") ? result.get("name").asText() : "未知歌单";
        String creator = "未知用户";
        if (result.has("creator") && result.get("creator").has("nickname")) {
            creator = result.get("creator").get("nickname").asText();
        }
        String cover = result.has("coverImgUrl") ? result.get("coverImgUrl").asText() : "";
        String description = result.has("description") ? result.get("description").asText() : "";
        
        // 解析歌曲列表
        List<PlaylistSongDTO> songs = new ArrayList<>();
        if (result.has("tracks")) {
            JsonNode tracks = result.get("tracks");
            for (JsonNode trackNode : tracks) {
                PlaylistSongDTO song = parseSong(trackNode, title);
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
                .source("netease")
                .songs(songs)
                .songCount(songs.size())
                .description(description)
                .build();
    }
    
    /**
     * 解析单首歌曲信息
     */
    private PlaylistSongDTO parseSong(JsonNode trackNode, String playlistName) {
        try {
            String songName = trackNode.has("name") ? trackNode.get("name").asText() : "未知歌曲";
            String songId = trackNode.has("id") ? trackNode.get("id").asText() : "0";

            // 检测VIP状态 - 网易云音乐的多种VIP标识
            boolean vip = false;
            if (trackNode.has("fee")) {
                int fee = trackNode.get("fee").asInt();
                // fee=1: VIP歌曲, fee=4: 付费专辑, fee=8: 非会员可免费播放低音质，会员可播放高音质及下载
                if (fee == 1 || fee == 4 || fee == 8) {
                    vip = true;
                }
            }

            // 检查其他VIP相关字段
            if (trackNode.has("privilege")) {
                JsonNode privilege = trackNode.get("privilege");
                // 检查播放权限
                if (privilege.has("st") && privilege.get("st").asInt() < 0) {
                    vip = true;
                }
                // 检查下载权限
                if (privilege.has("dl") && privilege.get("dl").asInt() < 0) {
                    vip = true;
                }
                // 检查付费类型
                if (privilege.has("fee") && privilege.get("fee").asInt() > 0) {
                    vip = true;
                }
            }

            // 检查版权信息
            if (trackNode.has("copyright") && trackNode.get("copyright").asInt() == 0) {
                // 版权受限，可能需要VIP
                vip = true;
            }
            
            // 获取专辑信息
            String albumName = "未知专辑";
            String coverUrl = "";
            if (trackNode.has("album")) {
                JsonNode album = trackNode.get("album");
                albumName = album.has("name") ? album.get("name").asText() : "未知专辑";
                if (album.has("picUrl")) {
                    coverUrl = album.get("picUrl").asText();
                }
            }
            
            // 获取歌手信息
            List<String> artists = new ArrayList<>();
            if (trackNode.has("artists")) {
                JsonNode artistArray = trackNode.get("artists");
                for (JsonNode artistNode : artistArray) {
                    if (artistNode.has("name")) {
                        artists.add(artistNode.get("name").asText());
                    }
                }
            }
            String artist = artists.isEmpty() ? "未知歌手" : String.join(", ", artists);
            
            // 构建歌曲URL
            String songUrl = String.format("https://music.163.com/#/song?id=%s", songId);
            
            // 获取时长
            String duration = "";
            if (trackNode.has("duration")) {
                long durationMs = trackNode.get("duration").asLong();
                long minutes = durationMs / 60000;
                long seconds = (durationMs % 60000) / 1000;
                duration = String.format("%02d:%02d", minutes, seconds);
            }
            
            return PlaylistSongDTO.builder()
                    .title(songName)
                    .artist(artist)
                    .album(albumName)
                    .url(songUrl)
                    .cover(coverUrl)
                    .source("netease")
                    .sourceId(songId)
                    .duration(duration)
                    .playlistName(playlistName)
                    .vip(vip)
                    .build();
                    
        } catch (Exception e) {
            log.error("解析网易云歌曲信息时发生错误", e);
            return null;
        }
    }
}
