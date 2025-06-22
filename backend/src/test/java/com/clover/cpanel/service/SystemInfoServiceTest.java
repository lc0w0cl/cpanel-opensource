package com.clover.cpanel.service;

import com.clover.cpanel.dto.SystemInfoDto;
import com.clover.cpanel.service.impl.SystemInfoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统信息服务测试类
 */
@SpringBootTest
public class SystemInfoServiceTest {

    private final SystemInfoService systemInfoService = new SystemInfoServiceImpl();

    @Test
    public void testGetSystemInfo() {
        SystemInfoDto systemInfo = systemInfoService.getSystemInfo();
        
        assertNotNull(systemInfo);
        assertNotNull(systemInfo.getCpu());
        assertNotNull(systemInfo.getMemory());
        assertNotNull(systemInfo.getDisks());
        assertNotNull(systemInfo.getSystem());
        
        System.out.println("=== 系统信息测试 ===");
        System.out.println("CPU型号: " + systemInfo.getCpu().getModel());
        System.out.println("CPU核心数: " + systemInfo.getCpu().getCores());
        System.out.println("CPU使用率: " + systemInfo.getCpu().getUsage() + "%");
        
        System.out.println("总内存: " + systemInfo.getMemory().getTotal() + " GB");
        System.out.println("已用内存: " + systemInfo.getMemory().getUsed() + " GB");
        System.out.println("内存使用率: " + systemInfo.getMemory().getUsage() + "%");
        
        System.out.println("磁盘数量: " + systemInfo.getDisks().size());
        for (SystemInfoDto.DiskInfo disk : systemInfo.getDisks()) {
            System.out.println("磁盘 " + disk.getName() + ": " + disk.getUsed() + "/" + disk.getTotal() + " GB (" + disk.getUsage() + "%)");
        }
        
        System.out.println("操作系统: " + systemInfo.getSystem().getOsName() + " " + systemInfo.getSystem().getOsVersion());
        System.out.println("主机名: " + systemInfo.getSystem().getHostname());
    }

    @Test
    public void testGetCpuInfo() {
        SystemInfoDto.CpuInfo cpuInfo = systemInfoService.getCpuInfo();
        
        assertNotNull(cpuInfo);
        assertTrue(cpuInfo.getCores() > 0);
        assertTrue(cpuInfo.getLogicalProcessors() > 0);
        assertTrue(cpuInfo.getUsage() >= 0 && cpuInfo.getUsage() <= 100);
        
        System.out.println("=== CPU信息测试 ===");
        System.out.println("CPU型号: " + cpuInfo.getModel());
        System.out.println("物理核心: " + cpuInfo.getCores());
        System.out.println("逻辑处理器: " + cpuInfo.getLogicalProcessors());
        System.out.println("CPU使用率: " + cpuInfo.getUsage() + "%");
        System.out.println("CPU频率: " + cpuInfo.getFrequency() + " Hz");
    }

    @Test
    public void testGetMemoryInfo() {
        SystemInfoDto.MemoryInfo memoryInfo = systemInfoService.getMemoryInfo();
        
        assertNotNull(memoryInfo);
        assertTrue(memoryInfo.getTotal() > 0);
        assertTrue(memoryInfo.getUsed() >= 0);
        assertTrue(memoryInfo.getAvailable() >= 0);
        assertTrue(memoryInfo.getUsage() >= 0 && memoryInfo.getUsage() <= 100);
        
        System.out.println("=== 内存信息测试 ===");
        System.out.println("总内存: " + memoryInfo.getTotal() + " GB");
        System.out.println("已用内存: " + memoryInfo.getUsed() + " GB");
        System.out.println("可用内存: " + memoryInfo.getAvailable() + " GB");
        System.out.println("内存使用率: " + memoryInfo.getUsage() + "%");
    }

    @Test
    public void testGetDiskInfo() {
        List<SystemInfoDto.DiskInfo> diskInfoList = systemInfoService.getDiskInfo();
        
        assertNotNull(diskInfoList);
        assertFalse(diskInfoList.isEmpty());
        
        System.out.println("=== 磁盘信息测试 ===");
        for (SystemInfoDto.DiskInfo disk : diskInfoList) {
            assertNotNull(disk.getName());
            assertTrue(disk.getTotal() >= 0);
            assertTrue(disk.getUsed() >= 0);
            assertTrue(disk.getAvailable() >= 0);
            assertTrue(disk.getUsage() >= 0 && disk.getUsage() <= 100);
            
            System.out.println("磁盘: " + disk.getName());
            System.out.println("  文件系统: " + disk.getFileSystem());
            System.out.println("  总容量: " + disk.getTotal() + " GB");
            System.out.println("  已用: " + disk.getUsed() + " GB");
            System.out.println("  可用: " + disk.getAvailable() + " GB");
            System.out.println("  使用率: " + disk.getUsage() + "%");
            System.out.println();
        }
    }

    @Test
    public void testGetSystemBasicInfo() {
        SystemInfoDto.SystemBasicInfo basicInfo = systemInfoService.getSystemBasicInfo();
        
        assertNotNull(basicInfo);
        assertNotNull(basicInfo.getOsName());
        assertNotNull(basicInfo.getHostname());
        assertTrue(basicInfo.getUptime() > 0);
        
        System.out.println("=== 系统基本信息测试 ===");
        System.out.println("操作系统: " + basicInfo.getOsName());
        System.out.println("系统版本: " + basicInfo.getOsVersion());
        System.out.println("系统架构: " + basicInfo.getArchitecture());
        System.out.println("主机名: " + basicInfo.getHostname());
        System.out.println("运行时间: " + basicInfo.getUptime() + " 秒");
    }
}
