package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.dto.SystemInfoDto;
import com.clover.cpanel.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统信息控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemInfoController {

    private final SystemInfoService systemInfoService;

    /**
     * 获取完整的系统信息
     * @return 系统信息
     */
    @GetMapping("/info")
    public ApiResponse<SystemInfoDto> getSystemInfo() {
        try {
            SystemInfoDto systemInfo = systemInfoService.getSystemInfo();
            return ApiResponse.success(systemInfo);
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return ApiResponse.error("获取系统信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取CPU信息
     * @return CPU信息
     */
    @GetMapping("/cpu")
    public ApiResponse<SystemInfoDto.CpuInfo> getCpuInfo() {
        try {
            SystemInfoDto.CpuInfo cpuInfo = systemInfoService.getCpuInfo();
            return ApiResponse.success(cpuInfo);
        } catch (Exception e) {
            log.error("获取CPU信息失败", e);
            return ApiResponse.error("获取CPU信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取内存信息
     * @return 内存信息
     */
    @GetMapping("/memory")
    public ApiResponse<SystemInfoDto.MemoryInfo> getMemoryInfo() {
        try {
            SystemInfoDto.MemoryInfo memoryInfo = systemInfoService.getMemoryInfo();
            return ApiResponse.success(memoryInfo);
        } catch (Exception e) {
            log.error("获取内存信息失败", e);
            return ApiResponse.error("获取内存信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取磁盘信息
     * @return 磁盘信息列表
     */
    @GetMapping("/disk")
    public ApiResponse<List<SystemInfoDto.DiskInfo>> getDiskInfo() {
        try {
            List<SystemInfoDto.DiskInfo> diskInfo = systemInfoService.getDiskInfo();
            return ApiResponse.success(diskInfo);
        } catch (Exception e) {
            log.error("获取磁盘信息失败", e);
            return ApiResponse.error("获取磁盘信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取系统基本信息
     * @return 系统基本信息
     */
    @GetMapping("/basic")
    public ApiResponse<SystemInfoDto.SystemBasicInfo> getSystemBasicInfo() {
        try {
            SystemInfoDto.SystemBasicInfo basicInfo = systemInfoService.getSystemBasicInfo();
            return ApiResponse.success(basicInfo);
        } catch (Exception e) {
            log.error("获取系统基本信息失败", e);
            return ApiResponse.error("获取系统基本信息失败：" + e.getMessage());
        }
    }
}
