package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.dto.MusicSearchRequestDTO;
import com.clover.cpanel.dto.MusicSearchResultDTO;
import com.clover.cpanel.service.MusicSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
