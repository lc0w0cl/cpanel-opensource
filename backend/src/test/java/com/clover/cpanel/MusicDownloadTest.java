package com.clover.cpanel;

import com.clover.cpanel.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 音乐下载功能测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class MusicDownloadTest {

    @Autowired
    private SystemConfigService systemConfigService;

    @Test
    public void testMusicConfigInitialization() {
        // 测试音乐配置是否正确初始化
        String downloadLocation = systemConfigService.getConfigValue("music_download_location");
        String serverDownloadPath = systemConfigService.getConfigValue("music_server_download_path");

        assertNotNull(downloadLocation, "音乐下载位置配置不应为空");
        assertNotNull(serverDownloadPath, "音乐服务器下载路径配置不应为空");

        assertTrue(downloadLocation.equals("local") || downloadLocation.equals("server"),
                "下载位置应该是 local 或 server");

        assertFalse(serverDownloadPath.trim().isEmpty(), "服务器下载路径不应为空");

        System.out.println("音乐下载位置: " + downloadLocation);
        System.out.println("服务器下载路径: " + serverDownloadPath);
    }

    @Test
    public void testMusicConfigUpdate() {
        // 测试音乐配置更新
        boolean success1 = systemConfigService.setConfigValue(
                "music_download_location",
                "server",
                "音乐下载位置设置",
                "music"
        );

        boolean success2 = systemConfigService.setConfigValue(
                "music_server_download_path",
                "uploads/test-music",
                "音乐服务器下载路径",
                "music"
        );

        assertTrue(success1, "更新下载位置配置应该成功");
        assertTrue(success2, "更新服务器路径配置应该成功");

        // 验证更新结果
        String updatedLocation = systemConfigService.getConfigValue("music_download_location");
        String updatedPath = systemConfigService.getConfigValue("music_server_download_path");

        assertEquals("server", updatedLocation, "下载位置应该更新为 server");
        assertEquals("uploads/test-music", updatedPath, "服务器路径应该更新为 uploads/test-music");

        // 恢复默认设置
        systemConfigService.setConfigValue("music_download_location", "local", "音乐下载位置设置", "music");
        systemConfigService.setConfigValue("music_server_download_path", "uploads/music", "音乐服务器下载路径", "music");
    }
}
