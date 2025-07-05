package com.clover.cpanel.service;

import com.clover.cpanel.dto.PlaylistInfoDTO;
import com.clover.cpanel.dto.PlaylistParseRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 歌单解析服务
 */
@Slf4j
@Service
public class PlaylistParserService {
    
    @Autowired
    private QQMusicPlaylistParser qqMusicParser;
    
    @Autowired
    private NetEasePlaylistParser netEaseParser;
    
    /**
     * 解析歌单
     */
    public PlaylistInfoDTO parsePlaylist(PlaylistParseRequestDTO request) {
        String url = request.getUrl();
        String platform = request.getPlatform();
        
        if (url == null || url.trim().isEmpty()) {
            throw new RuntimeException("歌单URL不能为空");
        }
        
        log.info("开始解析歌单: URL={}, Platform={}", url, platform);
        
        // 自动识别平台
        if ("auto".equals(platform)) {
            platform = detectPlatform(url);
        }
        
        // 根据平台选择解析器
        switch (platform.toLowerCase()) {
            case "qq":
                return qqMusicParser.parsePlaylist(url);
            case "netease":
                return netEaseParser.parsePlaylist(url);
            default:
                throw new RuntimeException("不支持的平台: " + platform);
        }
    }
    
    /**
     * 自动检测平台
     */
    private String detectPlatform(String url) {
        if (url.contains("y.qq.com")) {
            return "qq";
        } else if (url.contains("music.163.com")) {
            return "netease";
        } else {
            throw new RuntimeException("无法识别的音乐平台，请手动指定平台类型");
        }
    }
    
    /**
     * 验证URL是否支持
     */
    public boolean isSupportedUrl(String url) {
        try {
            detectPlatform(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取支持的平台列表
     */
    public String[] getSupportedPlatforms() {
        return new String[]{"qq", "netease"};
    }
}
