package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.service.SshService;
import com.clover.cpanel.service.TwoFactorAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 终端控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/terminal")
public class TerminalController {

    @Autowired
    private SshService sshService;

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    /**
     * 验证2FA并测试SSH连接
     */
    @PostMapping("/verify-and-connect")
    public ApiResponse<Map<String, Object>> verifyAndConnect(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            // 检查是否启用了2FA
            boolean twoFactorEnabled = twoFactorAuthService.isTwoFactorEnabled(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("twoFactorRequired", twoFactorEnabled);

            if (twoFactorEnabled) {
                // 需要2FA验证
                String verificationCode = (String) request.get("verificationCode");
                String backupCode = (String) request.get("backupCode");

                boolean verified = false;
                if (verificationCode != null && !verificationCode.trim().isEmpty()) {
                    verified = twoFactorAuthService.verifyTwoFactorCode(userId, verificationCode);
                } else if (backupCode != null && !backupCode.trim().isEmpty()) {
                    verified = twoFactorAuthService.verifyBackupCode(userId, backupCode);
                }

                if (!verified) {
                    result.put("verified", false);
                    return ApiResponse.error("2FA验证失败", result);
                }

                result.put("verified", true);
            }

            // 2FA验证通过或未启用2FA，继续SSH连接测试
            String host = (String) request.get("host");
            Integer port = (Integer) request.get("port");
            String username = (String) request.get("username");
            String authType = (String) request.get("authType");
            String password = (String) request.get("password");
            String privateKey = (String) request.get("privateKey");
            String privateKeyPassword = (String) request.get("privateKeyPassword");

            // 生成临时会话ID用于测试
            String testSessionId = "test_" + System.currentTimeMillis();

            try {
                // 尝试创建连接
                SshService.SshConnection connection = sshService.createConnection(
                    testSessionId, host, port != null ? port : 22, username,
                    authType, password, privateKey, privateKeyPassword
                );

                if (connection.isConnected()) {
                    // 立即断开测试连接
                    sshService.disconnect(testSessionId);
                    result.put("connectionSuccess", true);
                    result.put("message", "SSH连接测试成功");
                    return ApiResponse.success(result);
                } else {
                    result.put("connectionSuccess", false);
                    return ApiResponse.error("SSH连接测试失败", result);
                }
            } catch (Exception e) {
                result.put("connectionSuccess", false);
                return ApiResponse.error("SSH连接测试失败: " + e.getMessage(), result);
            }

        } catch (Exception e) {
            log.error("验证2FA并测试SSH连接失败", e);
            return ApiResponse.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 测试SSH连接（不需要2FA验证）
     */
    @PostMapping("/test-connection")
    public ApiResponse<String> testConnection(@RequestBody Map<String, Object> request) {
        try {
            String host = (String) request.get("host");
            Integer port = (Integer) request.get("port");
            String username = (String) request.get("username");
            String authType = (String) request.get("authType");
            String password = (String) request.get("password");
            String privateKey = (String) request.get("privateKey");
            String privateKeyPassword = (String) request.get("privateKeyPassword");

            // 生成临时会话ID用于测试
            String testSessionId = "test_" + System.currentTimeMillis();

            try {
                // 尝试创建连接
                SshService.SshConnection connection = sshService.createConnection(
                    testSessionId, host, port != null ? port : 22, username, 
                    authType, password, privateKey, privateKeyPassword
                );

                if (connection.isConnected()) {
                    // 立即断开测试连接
                    sshService.disconnect(testSessionId);
                    return ApiResponse.success("SSH连接测试成功");
                } else {
                    return ApiResponse.error("SSH连接测试失败");
                }
            } catch (Exception e) {
                return ApiResponse.error("SSH连接测试失败: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error("测试SSH连接失败", e);
            return ApiResponse.error("测试连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取活跃会话状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        try {
            int activeSessionCount = sshService.getActiveSessionCount();
            
            Map<String, Object> status = Map.of(
                "activeSessionCount", activeSessionCount,
                "maxSessions", 10, // 可配置的最大会话数
                "serverTime", System.currentTimeMillis()
            );
            
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取终端状态失败", e);
            return ApiResponse.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 断开指定会话
     */
    @PostMapping("/disconnect/{sessionId}")
    public ApiResponse<String> disconnectSession(@PathVariable String sessionId) {
        try {
            sshService.disconnect(sessionId);
            return ApiResponse.success("会话已断开");
        } catch (Exception e) {
            log.error("断开会话失败", e);
            return ApiResponse.error("断开会话失败: " + e.getMessage());
        }
    }

    /**
     * 断开所有会话
     */
    @PostMapping("/disconnect-all")
    public ApiResponse<String> disconnectAllSessions() {
        try {
            sshService.disconnectAll();
            return ApiResponse.success("所有会话已断开");
        } catch (Exception e) {
            log.error("断开所有会话失败", e);
            return ApiResponse.error("断开所有会话失败: " + e.getMessage());
        }
    }
}
