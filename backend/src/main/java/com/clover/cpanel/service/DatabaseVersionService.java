package com.clover.cpanel.service;

/**
 * 数据库版本管理服务接口
 */
public interface DatabaseVersionService {

    /**
     * 获取当前数据库版本
     * @return 版本号
     */
    String getCurrentVersion();

    /**
     * 设置数据库版本
     * @param version 版本号
     */
    void setVersion(String version);

    /**
     * 检查是否需要升级
     * @param targetVersion 目标版本
     * @return 是否需要升级
     */
    boolean needsUpgrade(String targetVersion);

    /**
     * 执行数据库升级
     * @param fromVersion 起始版本
     * @param toVersion 目标版本
     * @return 是否升级成功
     */
    boolean upgradeDatabase(String fromVersion, String toVersion);
}
