package com.clover.cpanel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.clover.cpanel.config.EncryptionConfig;
import com.clover.cpanel.entity.Server;
import com.clover.cpanel.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据加密迁移服务
 * 负责将现有明文敏感数据加密
 */
@Slf4j
@Service
public class DataEncryptionMigrationService {

    @Autowired
    private ServerService serverService;

    @Autowired
    private EncryptionConfig encryptionConfig;

    @Autowired
    private SystemConfigService systemConfigService;

    private static final String MIGRATION_FLAG_KEY = "data_encryption_migration_completed";

    /**
     * 检查是否需要执行数据加密迁移
     * @return 是否需要迁移
     */
    public boolean needsMigration() {
        if (!encryptionConfig.isEncryptionEnabled()) {
            log.info("加密功能未启用，跳过数据迁移");
            return false;
        }

        String migrationFlag = systemConfigService.getConfigValue(MIGRATION_FLAG_KEY);
        boolean completed = "true".equals(migrationFlag);
        
        if (completed) {
            log.info("数据加密迁移已完成，跳过");
            return false;
        }

        log.info("检测到需要执行数据加密迁移");
        return true;
    }

    /**
     * 执行数据加密迁移
     * @return 迁移是否成功
     */
    @Transactional
    public boolean executeMigration() {
        if (!needsMigration()) {
            return true;
        }

        try {
            log.info("开始执行数据加密迁移...");

            // 迁移服务器敏感数据
            boolean serverMigrationSuccess = migrateServerSensitiveData();
            if (!serverMigrationSuccess) {
                log.error("服务器数据加密迁移失败");
                return false;
            }

            // 标记迁移完成
            boolean flagSuccess = systemConfigService.setConfigValue(
                MIGRATION_FLAG_KEY, 
                "true", 
                "数据加密迁移完成标志", 
                "SYSTEM"
            );

            if (flagSuccess) {
                log.info("数据加密迁移完成");
                return true;
            } else {
                log.error("设置迁移完成标志失败");
                return false;
            }

        } catch (Exception e) {
            log.error("执行数据加密迁移失败", e);
            return false;
        }
    }

    /**
     * 迁移服务器敏感数据
     * @return 是否成功
     */
    private boolean migrateServerSensitiveData() {
        try {
            log.info("开始迁移服务器敏感数据...");

            // 获取所有服务器记录
            QueryWrapper<Server> queryWrapper = new QueryWrapper<>();
            List<Server> servers = serverService.list(queryWrapper);

            int totalCount = servers.size();
            int successCount = 0;
            int skipCount = 0;

            for (Server server : servers) {
                try {
                    boolean needsUpdate = false;

                    // 检查并加密密码
                    if (server.getPassword() != null && !server.getPassword().isEmpty()) {
                        if (!AESUtil.isEncrypted(server.getPassword())) {
                            server.setPassword(encryptionConfig.encryptSensitiveData(server.getPassword()));
                            needsUpdate = true;
                            log.debug("加密服务器密码: {}", server.getServerName());
                        }
                    }

                    // 检查并加密私钥
                    if (server.getPrivateKey() != null && !server.getPrivateKey().isEmpty()) {
                        if (!AESUtil.isEncrypted(server.getPrivateKey())) {
                            server.setPrivateKey(encryptionConfig.encryptSensitiveData(server.getPrivateKey()));
                            needsUpdate = true;
                            log.debug("加密服务器私钥: {}", server.getServerName());
                        }
                    }

                    // 检查并加密私钥密码
                    if (server.getPrivateKeyPassword() != null && !server.getPrivateKeyPassword().isEmpty()) {
                        if (!AESUtil.isEncrypted(server.getPrivateKeyPassword())) {
                            server.setPrivateKeyPassword(encryptionConfig.encryptSensitiveData(server.getPrivateKeyPassword()));
                            needsUpdate = true;
                            log.debug("加密服务器私钥密码: {}", server.getServerName());
                        }
                    }

                    if (needsUpdate) {
                        // 直接更新数据库，不通过service层（避免重复加密）
                        boolean updateSuccess = serverService.updateById(server);
                        if (updateSuccess) {
                            successCount++;
                            log.info("服务器敏感数据加密成功: {}", server.getServerName());
                        } else {
                            log.error("服务器敏感数据加密失败: {}", server.getServerName());
                        }
                    } else {
                        skipCount++;
                        log.debug("服务器敏感数据已加密，跳过: {}", server.getServerName());
                    }

                } catch (Exception e) {
                    log.error("加密服务器敏感数据失败: {}", server.getServerName(), e);
                }
            }

            log.info("服务器敏感数据迁移完成 - 总数: {}, 成功: {}, 跳过: {}", totalCount, successCount, skipCount);
            return true;

        } catch (Exception e) {
            log.error("迁移服务器敏感数据失败", e);
            return false;
        }
    }

    /**
     * 强制重新执行迁移（用于测试或修复）
     * @return 是否成功
     */
    @Transactional
    public boolean forceMigration() {
        try {
            log.warn("强制执行数据加密迁移...");

            // 删除迁移标志
            systemConfigService.deleteConfigByKey(MIGRATION_FLAG_KEY);

            // 执行迁移
            return executeMigration();

        } catch (Exception e) {
            log.error("强制执行数据加密迁移失败", e);
            return false;
        }
    }

    /**
     * 检查迁移状态
     * @return 迁移状态信息
     */
    public String getMigrationStatus() {
        if (!encryptionConfig.isEncryptionEnabled()) {
            return "加密功能未启用";
        }

        String migrationFlag = systemConfigService.getConfigValue(MIGRATION_FLAG_KEY);
        boolean completed = "true".equals(migrationFlag);

        if (completed) {
            return "数据加密迁移已完成";
        } else {
            return "数据加密迁移未完成";
        }
    }
}
