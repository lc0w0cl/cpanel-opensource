package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.config.EncryptionConfig;
import com.clover.cpanel.service.DataEncryptionMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 加密管理控制器
 * 提供加密状态查看和管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/encryption")
public class EncryptionController {

    @Autowired
    private EncryptionConfig encryptionConfig;

    @Autowired
    private DataEncryptionMigrationService dataEncryptionMigrationService;

    /**
     * 获取加密状态
     * @return 加密状态信息
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getEncryptionStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            status.put("encryptionEnabled", encryptionConfig.isEncryptionEnabled());
            status.put("migrationStatus", dataEncryptionMigrationService.getMigrationStatus());
            status.put("needsMigration", dataEncryptionMigrationService.needsMigration());
            
            // 不返回实际的密钥，只返回是否已配置
            status.put("hasCustomKey", encryptionConfig.getAesKey() != null && !encryptionConfig.getAesKey().isEmpty());
            
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取加密状态失败", e);
            return ApiResponse.error("获取加密状态失败：" + e.getMessage());
        }
    }

    /**
     * 执行数据加密迁移
     * @return 迁移结果
     */
    @PostMapping("/migrate")
    public ApiResponse<String> executeMigration() {
        try {
            if (!encryptionConfig.isEncryptionEnabled()) {
                return ApiResponse.error("加密功能未启用，无法执行迁移");
            }

            boolean success = dataEncryptionMigrationService.executeMigration();
            if (success) {
                log.info("手动执行数据加密迁移成功");
                return ApiResponse.success("数据加密迁移执行成功");
            } else {
                return ApiResponse.error("数据加密迁移执行失败");
            }
        } catch (Exception e) {
            log.error("执行数据加密迁移失败", e);
            return ApiResponse.error("执行数据加密迁移失败：" + e.getMessage());
        }
    }

    /**
     * 强制重新执行迁移（用于测试或修复）
     * @return 迁移结果
     */
    @PostMapping("/force-migrate")
    public ApiResponse<String> forceMigration() {
        try {
            if (!encryptionConfig.isEncryptionEnabled()) {
                return ApiResponse.error("加密功能未启用，无法执行迁移");
            }

            boolean success = dataEncryptionMigrationService.forceMigration();
            if (success) {
                log.warn("强制执行数据加密迁移成功");
                return ApiResponse.success("强制数据加密迁移执行成功");
            } else {
                return ApiResponse.error("强制数据加密迁移执行失败");
            }
        } catch (Exception e) {
            log.error("强制执行数据加密迁移失败", e);
            return ApiResponse.error("强制执行数据加密迁移失败：" + e.getMessage());
        }
    }

    /**
     * 获取加密配置信息（不包含敏感信息）
     * @return 配置信息
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getEncryptionConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            config.put("encryptionEnabled", encryptionConfig.isEncryptionEnabled());
            config.put("algorithm", "AES-256-GCM");
            config.put("keySource", encryptionConfig.getAesKey() != null && !encryptionConfig.getAesKey().isEmpty() 
                ? "配置文件/环境变量" : "自动生成");
            
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("获取加密配置失败", e);
            return ApiResponse.error("获取加密配置失败：" + e.getMessage());
        }
    }
}
