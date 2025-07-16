package com.clover.cpanel.config;

import com.clover.cpanel.util.AESUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 加密配置类
 * 管理AES加密密钥和相关配置
 */
@Slf4j
@Data
@Configuration
public class EncryptionConfig {

    /**
     * AES加密密钥，从环境变量或配置文件读取
     * 如果未配置，将自动生成一个新密钥
     */
    @Value("${encryption.aes.key:}")
    private String aesKey;

    /**
     * 是否启用加密功能
     */
    @Value("${encryption.enabled:true}")
    private boolean encryptionEnabled;

    /**
     * 初始化加密配置
     */
    public void init() {
        if (encryptionEnabled) {
            if (aesKey == null || aesKey.trim().isEmpty()) {
                // 如果没有配置密钥，生成一个新的
                aesKey = AESUtil.generateKey();
                log.warn("未配置AES加密密钥，已自动生成新密钥。建议在生产环境中通过环境变量 ENCRYPTION_AES_KEY 配置固定密钥");
                log.info("生成的AES密钥: {}", aesKey);
            } else {
                log.info("使用配置的AES加密密钥");
            }
        } else {
            log.warn("加密功能已禁用，敏感数据将以明文存储");
        }
    }

    /**
     * 获取AES加密密钥
     * @return AES密钥
     */
    public String getAesKey() {
        return aesKey;
    }

    /**
     * 检查加密功能是否启用
     * @return 是否启用加密
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * 加密敏感文本
     * @param plainText 明文
     * @return 加密后的文本，如果加密功能未启用则返回原文
     */
    public String encryptSensitiveData(String plainText) {
        if (!encryptionEnabled || plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            return AESUtil.encrypt(plainText, aesKey);
        } catch (Exception e) {
            log.error("加密敏感数据失败，返回原文: {}", e.getMessage());
            return plainText;
        }
    }

    /**
     * 解密敏感文本
     * @param encryptedText 密文
     * @return 解密后的文本，如果加密功能未启用或解密失败则返回原文
     */
    public String decryptSensitiveData(String encryptedText) {
        if (!encryptionEnabled || encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        // 检查是否已加密
        if (!AESUtil.isEncrypted(encryptedText)) {
            // 如果不是加密格式，直接返回（可能是历史明文数据）
            return encryptedText;
        }

        try {
            return AESUtil.decrypt(encryptedText, aesKey);
        } catch (Exception e) {
            log.error("解密敏感数据失败，返回原文: {}", e.getMessage());
            return encryptedText;
        }
    }
}
