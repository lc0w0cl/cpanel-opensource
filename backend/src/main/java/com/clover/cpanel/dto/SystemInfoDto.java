package com.clover.cpanel.dto;

import lombok.Data;

import java.util.List;

/**
 * 系统信息DTO
 */
@Data
public class SystemInfoDto {

    /**
     * CPU信息
     */
    private CpuInfo cpu;

    /**
     * 内存信息
     */
    private MemoryInfo memory;

    /**
     * 磁盘信息列表
     */
    private List<DiskInfo> disks;

    /**
     * 系统基本信息
     */
    private SystemBasicInfo system;

    /**
     * CPU信息
     */
    @Data
    public static class CpuInfo {
        /**
         * CPU型号
         */
        private String model;

        /**
         * CPU核心数
         */
        private int cores;

        /**
         * CPU逻辑处理器数
         */
        private int logicalProcessors;

        /**
         * CPU使用率（百分比）
         */
        private double usage;

        /**
         * CPU频率（MHz）
         */
        private long frequency;
    }

    /**
     * 内存信息
     */
    @Data
    public static class MemoryInfo {
        /**
         * 总内存（GB）
         */
        private double total;

        /**
         * 已使用内存（GB）
         */
        private double used;

        /**
         * 可用内存（GB）
         */
        private double available;

        /**
         * 内存使用率（百分比）
         */
        private double usage;
    }

    /**
     * 磁盘信息
     */
    @Data
    public static class DiskInfo {
        /**
         * 磁盘名称/挂载点
         */
        private String name;

        /**
         * 文件系统类型
         */
        private String fileSystem;

        /**
         * 总容量（GB）
         */
        private double total;

        /**
         * 已使用容量（GB）
         */
        private double used;

        /**
         * 可用容量（GB）
         */
        private double available;

        /**
         * 使用率（百分比）
         */
        private double usage;
    }

    /**
     * 系统基本信息
     */
    @Data
    public static class SystemBasicInfo {
        /**
         * 操作系统名称
         */
        private String osName;

        /**
         * 操作系统版本
         */
        private String osVersion;

        /**
         * 系统架构
         */
        private String architecture;

        /**
         * 主机名
         */
        private String hostname;

        /**
         * 系统运行时间（秒）
         */
        private long uptime;
    }
}
