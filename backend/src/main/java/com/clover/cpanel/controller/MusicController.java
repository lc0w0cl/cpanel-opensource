package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.dto.MusicSearchRequestDTO;
import com.clover.cpanel.dto.MusicSearchResultDTO;
import com.clover.cpanel.service.MusicSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicSearchService musicSearchService;

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
}
