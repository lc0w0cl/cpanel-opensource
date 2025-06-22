package com.clover.cpanel.service;

import com.clover.cpanel.dto.SystemInfoDto;

/**
 * 系统信息服务接口
 */
public interface SystemInfoService {

    /**
     * 获取完整的系统信息
     * @return 系统信息DTO
     */
    SystemInfoDto getSystemInfo();

    /**
     * 获取CPU信息
     * @return CPU信息
     */
    SystemInfoDto.CpuInfo getCpuInfo();

    /**
     * 获取内存信息
     * @return 内存信息
     */
    SystemInfoDto.MemoryInfo getMemoryInfo();

    /**
     * 获取磁盘信息
     * @return 磁盘信息列表
     */
    java.util.List<SystemInfoDto.DiskInfo> getDiskInfo();

    /**
     * 获取系统基本信息
     * @return 系统基本信息
     */
    SystemInfoDto.SystemBasicInfo getSystemBasicInfo();
}
