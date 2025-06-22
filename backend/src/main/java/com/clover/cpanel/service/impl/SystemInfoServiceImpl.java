package com.clover.cpanel.service.impl;

import com.clover.cpanel.dto.SystemInfoDto;
import com.clover.cpanel.service.SystemInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统信息服务实现类
 */
@Slf4j
@Service
public class SystemInfoServiceImpl implements SystemInfoService {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;

    public SystemInfoServiceImpl() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    @Override
    public SystemInfoDto getSystemInfo() {
        SystemInfoDto dto = new SystemInfoDto();
        dto.setCpu(getCpuInfo());
        dto.setMemory(getMemoryInfo());
        dto.setDisks(getDiskInfo());
        dto.setSystem(getSystemBasicInfo());
        return dto;
    }

    @Override
    public SystemInfoDto.CpuInfo getCpuInfo() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            SystemInfoDto.CpuInfo cpuInfo = new SystemInfoDto.CpuInfo();

            // CPU基本信息
            cpuInfo.setModel(processor.getProcessorIdentifier().getName());
            cpuInfo.setCores(processor.getPhysicalProcessorCount());
            cpuInfo.setLogicalProcessors(processor.getLogicalProcessorCount());
            cpuInfo.setFrequency(processor.getMaxFreq());

            // CPU使用率（需要两次采样计算）
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            try {
                Thread.sleep(1000); // 等待1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            double cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
            cpuInfo.setUsage(Math.round(cpuUsage * 100.0) / 100.0); // 保留两位小数

            return cpuInfo;
        } catch (Exception e) {
            log.error("获取CPU信息失败", e);
            return new SystemInfoDto.CpuInfo();
        }
    }

    @Override
    public SystemInfoDto.MemoryInfo getMemoryInfo() {
        try {
            GlobalMemory memory = hardware.getMemory();
            SystemInfoDto.MemoryInfo memoryInfo = new SystemInfoDto.MemoryInfo();

            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            long usedMemory = totalMemory - availableMemory;

            // 转换为GB并保留两位小数
            memoryInfo.setTotal(Math.round(totalMemory / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0);
            memoryInfo.setUsed(Math.round(usedMemory / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0);
            memoryInfo.setAvailable(Math.round(availableMemory / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0);

            // 计算使用率
            double usage = (double) usedMemory / totalMemory * 100;
            memoryInfo.setUsage(Math.round(usage * 100.0) / 100.0);

            return memoryInfo;
        } catch (Exception e) {
            log.error("获取内存信息失败", e);
            return new SystemInfoDto.MemoryInfo();
        }
    }

    @Override
    public List<SystemInfoDto.DiskInfo> getDiskInfo() {
        List<SystemInfoDto.DiskInfo> diskInfoList = new ArrayList<>();
        try {
            FileSystem fileSystem = operatingSystem.getFileSystem();
            List<OSFileStore> fileStores = fileSystem.getFileStores();

            for (OSFileStore store : fileStores) {
                SystemInfoDto.DiskInfo diskInfo = new SystemInfoDto.DiskInfo();

                diskInfo.setName(store.getMount());
                diskInfo.setFileSystem(store.getType());

                long totalSpace = store.getTotalSpace();
                long usableSpace = store.getUsableSpace();
                long usedSpace = totalSpace - usableSpace;

                // 转换为GB并保留两位小数
                diskInfo.setTotal(Math.round(totalSpace / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0);
                diskInfo.setUsed(Math.round(usedSpace / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0);
                diskInfo.setAvailable(Math.round(usableSpace / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0);

                // 计算使用率
                if (totalSpace > 0) {
                    double usage = (double) usedSpace / totalSpace * 100;
                    diskInfo.setUsage(Math.round(usage * 100.0) / 100.0);
                } else {
                    diskInfo.setUsage(0.0);
                }

                diskInfoList.add(diskInfo);
            }

            return diskInfoList;
        } catch (Exception e) {
            log.error("获取磁盘信息失败", e);
            return diskInfoList;
        }
    }

    @Override
    public SystemInfoDto.SystemBasicInfo getSystemBasicInfo() {
        try {
            SystemInfoDto.SystemBasicInfo basicInfo = new SystemInfoDto.SystemBasicInfo();

            basicInfo.setOsName(operatingSystem.getFamily());
            basicInfo.setOsVersion(operatingSystem.getVersionInfo().toString());
            basicInfo.setArchitecture(System.getProperty("os.arch"));
            basicInfo.setHostname(operatingSystem.getNetworkParams().getHostName());
            basicInfo.setUptime(operatingSystem.getSystemUptime());

            return basicInfo;
        } catch (Exception e) {
            log.error("获取系统基本信息失败", e);
            return new SystemInfoDto.SystemBasicInfo();
        }
    }
}
