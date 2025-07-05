package com.clover.cpanel.service.impl;

import com.clover.cpanel.service.DatabaseVersionService;
import com.clover.cpanel.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数据库版本管理服务实现类
 */
@Slf4j
@Service
public class DatabaseVersionServiceImpl implements DatabaseVersionService {

    private static final String VERSION_KEY = "database_version";
    private static final String CURRENT_VERSION = "1.2.0"; // 当前应用的数据库版本

    @Autowired
    private SystemConfigService systemConfigService;

    @Override
    public String getCurrentVersion() {
        String version = systemConfigService.getConfigValue(VERSION_KEY);
        return version != null ? version : "1.0.0"; // 默认版本
    }

    @Override
    public void setVersion(String version) {
        systemConfigService.setConfigValue(VERSION_KEY, version, "数据库版本号", "system");
        log.info("数据库版本已更新为: {}", version);
    }

    @Override
    public boolean needsUpgrade(String targetVersion) {
        String currentVersion = getCurrentVersion();
        return !currentVersion.equals(targetVersion);
    }

    @Override
    public boolean upgradeDatabase(String fromVersion, String toVersion) {
        log.info("开始数据库升级: {} -> {}", fromVersion, toVersion);
        
        try {
            // 根据版本执行相应的升级脚本
            if ("1.0.0".equals(fromVersion) && "1.1.0".equals(toVersion)) {
                // 从1.0.0升级到1.1.0：添加TODO表
                return upgradeFrom1_0_0To1_1_0();
            } else if ("1.1.0".equals(fromVersion) && "1.2.0".equals(toVersion)) {
                // 从1.1.0升级到1.2.0：添加音乐配置
                return upgradeFrom1_1_0To1_2_0();
            } else if ("1.0.0".equals(fromVersion) && "1.2.0".equals(toVersion)) {
                // 从1.0.0直接升级到1.2.0：添加TODO表和音乐配置
                return upgradeFrom1_0_0To1_2_0();
            }

            // 可以在这里添加更多版本升级逻辑

            log.warn("未找到从版本 {} 到 {} 的升级路径", fromVersion, toVersion);
            return false;
        } catch (Exception e) {
            log.error("数据库升级失败: {} -> {}", fromVersion, toVersion, e);
            return false;
        }
    }

    /**
     * 从1.0.0升级到1.1.0
     * 主要变更：添加TODO功能相关表
     */
    private boolean upgradeFrom1_0_0To1_1_0() {
        try {
            log.info("执行1.0.0到1.1.0的数据库升级...");

            // 这里的升级逻辑会在DatabaseInitService中处理
            // 因为我们使用的是检查表是否存在的方式

            // 更新版本号
            setVersion("1.1.0");

            log.info("数据库升级完成: 1.0.0 -> 1.1.0");
            return true;
        } catch (Exception e) {
            log.error("1.0.0到1.1.0升级失败", e);
            return false;
        }
    }

    /**
     * 从1.1.0升级到1.2.0
     * 主要变更：添加音乐配置功能
     */
    private boolean upgradeFrom1_1_0To1_2_0() {
        try {
            log.info("执行1.1.0到1.2.0的数据库升级...");

            // 音乐配置的初始化逻辑会在DatabaseInitService中处理
            // 这里只需要更新版本号

            // 更新版本号
            setVersion("1.2.0");

            log.info("数据库升级完成: 1.1.0 -> 1.2.0");
            return true;
        } catch (Exception e) {
            log.error("1.1.0到1.2.0升级失败", e);
            return false;
        }
    }

    /**
     * 从1.0.0直接升级到1.2.0
     * 主要变更：添加TODO功能和音乐配置功能
     */
    private boolean upgradeFrom1_0_0To1_2_0() {
        try {
            log.info("执行1.0.0到1.2.0的数据库升级...");

            // 所有升级逻辑会在DatabaseInitService中处理
            // 这里只需要更新版本号

            // 更新版本号
            setVersion("1.2.0");

            log.info("数据库升级完成: 1.0.0 -> 1.2.0");
            return true;
        } catch (Exception e) {
            log.error("1.0.0到1.2.0升级失败", e);
            return false;
        }
    }

    /**
     * 检查并执行必要的数据库升级
     */
    public void checkAndUpgrade() {
        String currentVersion = getCurrentVersion();
        
        if (needsUpgrade(CURRENT_VERSION)) {
            log.info("检测到数据库需要升级: {} -> {}", currentVersion, CURRENT_VERSION);
            
            if (upgradeDatabase(currentVersion, CURRENT_VERSION)) {
                log.info("数据库升级成功");
            } else {
                log.error("数据库升级失败");
            }
        } else {
            log.info("数据库版本已是最新: {}", currentVersion);
        }
    }
}
