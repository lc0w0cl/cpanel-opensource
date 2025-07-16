package com.clover.cpanel.service;

import com.clover.cpanel.config.EncryptionConfig;
import com.clover.cpanel.entity.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 服务器加密服务
 * 负责服务器敏感信息的加密和解密
 */
@Slf4j
@Service
public class ServerEncryptionService {

    @Autowired
    private EncryptionConfig encryptionConfig;

    /**
     * 加密服务器敏感信息（保存到数据库前调用）
     * @param server 服务器对象
     */
    public void encryptSensitiveFields(Server server) {
        if (server == null) {
            return;
        }

        try {
            // 加密密码
            if (server.getPassword() != null && !server.getPassword().isEmpty()) {
                server.setPassword(encryptionConfig.encryptSensitiveData(server.getPassword()));
            }

            // 加密私钥
            if (server.getPrivateKey() != null && !server.getPrivateKey().isEmpty()) {
                server.setPrivateKey(encryptionConfig.encryptSensitiveData(server.getPrivateKey()));
            }

            // 加密私钥密码
            if (server.getPrivateKeyPassword() != null && !server.getPrivateKeyPassword().isEmpty()) {
                server.setPrivateKeyPassword(encryptionConfig.encryptSensitiveData(server.getPrivateKeyPassword()));
            }

            log.debug("服务器敏感信息加密完成: {}", server.getServerName());
        } catch (Exception e) {
            log.error("加密服务器敏感信息失败: {}", server.getServerName(), e);
            throw new RuntimeException("加密服务器敏感信息失败", e);
        }
    }

    /**
     * 解密服务器敏感信息（从数据库读取后调用）
     * @param server 服务器对象
     */
    public void decryptSensitiveFields(Server server) {
        if (server == null) {
            return;
        }

        try {
            // 解密密码
            if (server.getPassword() != null && !server.getPassword().isEmpty()) {
                server.setPassword(encryptionConfig.decryptSensitiveData(server.getPassword()));
            }

            // 解密私钥
            if (server.getPrivateKey() != null && !server.getPrivateKey().isEmpty()) {
                server.setPrivateKey(encryptionConfig.decryptSensitiveData(server.getPrivateKey()));
            }

            // 解密私钥密码
            if (server.getPrivateKeyPassword() != null && !server.getPrivateKeyPassword().isEmpty()) {
                server.setPrivateKeyPassword(encryptionConfig.decryptSensitiveData(server.getPrivateKeyPassword()));
            }

            log.debug("服务器敏感信息解密完成: {}", server.getServerName());
        } catch (Exception e) {
            log.error("解密服务器敏感信息失败: {}", server.getServerName(), e);
            throw new RuntimeException("解密服务器敏感信息失败", e);
        }
    }

    /**
     * 创建服务器副本并解密敏感信息（用于返回给前端，不修改原对象）
     * @param server 原服务器对象
     * @return 解密后的服务器副本
     */
    public Server createDecryptedCopy(Server server) {
        if (server == null) {
            return null;
        }

        try {
            // 创建副本
            Server decryptedServer = new Server();
            
            // 复制所有字段
            decryptedServer.setId(server.getId());
            decryptedServer.setServerName(server.getServerName());
            decryptedServer.setHost(server.getHost());
            decryptedServer.setPort(server.getPort());
            decryptedServer.setUsername(server.getUsername());
            decryptedServer.setAuthType(server.getAuthType());
            decryptedServer.setDescription(server.getDescription());
            decryptedServer.setIcon(server.getIcon());
            decryptedServer.setCategoryId(server.getCategoryId());
            decryptedServer.setGroupName(server.getGroupName());
            decryptedServer.setIsDefault(server.getIsDefault());
            decryptedServer.setStatus(server.getStatus());
            decryptedServer.setSortOrder(server.getSortOrder());
            decryptedServer.setCreatedAt(server.getCreatedAt());
            decryptedServer.setUpdatedAt(server.getUpdatedAt());

            // 解密敏感字段
            if (server.getPassword() != null && !server.getPassword().isEmpty()) {
                decryptedServer.setPassword(encryptionConfig.decryptSensitiveData(server.getPassword()));
            } else {
                decryptedServer.setPassword(server.getPassword());
            }

            if (server.getPrivateKey() != null && !server.getPrivateKey().isEmpty()) {
                decryptedServer.setPrivateKey(encryptionConfig.decryptSensitiveData(server.getPrivateKey()));
            } else {
                decryptedServer.setPrivateKey(server.getPrivateKey());
            }

            if (server.getPrivateKeyPassword() != null && !server.getPrivateKeyPassword().isEmpty()) {
                decryptedServer.setPrivateKeyPassword(encryptionConfig.decryptSensitiveData(server.getPrivateKeyPassword()));
            } else {
                decryptedServer.setPrivateKeyPassword(server.getPrivateKeyPassword());
            }

            return decryptedServer;
        } catch (Exception e) {
            log.error("创建解密服务器副本失败: {}", server.getServerName(), e);
            throw new RuntimeException("创建解密服务器副本失败", e);
        }
    }
}
