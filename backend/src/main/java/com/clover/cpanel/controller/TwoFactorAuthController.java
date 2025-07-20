package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.entity.TwoFactorAuth;
import com.clover.cpanel.service.TwoFactorAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 2FA认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/2fa")
public class TwoFactorAuthController {

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    /**
     * 获取2FA状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus(HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            boolean enabled = twoFactorAuthService.isTwoFactorEnabled(userId);
            TwoFactorAuth twoFactorAuth = twoFactorAuthService.getTwoFactorAuth(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("enabled", enabled);
            result.put("configured", twoFactorAuth != null);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取2FA状态失败", e);
            return ApiResponse.error("获取2FA状态失败");
        }
    }

    /**
     * 生成2FA配置（密钥和QR码）
     */
    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            TwoFactorAuth twoFactorAuth = twoFactorAuthService.generateTwoFactorAuth(userId);
            String qrCodeImage = twoFactorAuthService.generateQRCodeImage(
                twoFactorAuth.getSecretKey(), 
                userId, 
                "CPanel"
            );

            Map<String, Object> result = new HashMap<>();
            result.put("secretKey", twoFactorAuth.getSecretKey());
            result.put("qrCodeImage", qrCodeImage);
            result.put("backupCodes", twoFactorAuth.getBackupCodes());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("生成2FA配置失败", e);
            return ApiResponse.error("生成2FA配置失败");
        }
    }

    /**
     * 启用2FA
     */
    @PostMapping("/enable")
    public ApiResponse<Map<String, Object>> enable(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            String verificationCode = request.get("verificationCode");
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return ApiResponse.error("验证码不能为空");
            }

            boolean success = twoFactorAuthService.enableTwoFactorAuth(userId, verificationCode);
            if (success) {
                // 启用成功后，返回备用代码
                TwoFactorAuth twoFactorAuth = twoFactorAuthService.getTwoFactorAuth(userId);
                Map<String, Object> result = new HashMap<>();
                result.put("message", "2FA启用成功");

                try {
                    String backupCodesJson = twoFactorAuth.getBackupCodes();
                    if (backupCodesJson != null && !backupCodesJson.trim().isEmpty()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        String[] backupCodes = mapper.readValue(backupCodesJson, String[].class);
                        result.put("backupCodes", backupCodes);
                    }
                } catch (Exception e) {
                    log.error("解析备用代码失败", e);
                }

                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("验证码错误或2FA启用失败");
            }
        } catch (Exception e) {
            log.error("启用2FA失败", e);
            return ApiResponse.error("启用2FA失败");
        }
    }

    /**
     * 禁用2FA
     */
    @PostMapping("/disable")
    public ApiResponse<String> disable(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            String verificationCode = request.get("verificationCode");
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return ApiResponse.error("验证码不能为空");
            }

            boolean success = twoFactorAuthService.disableTwoFactorAuth(userId, verificationCode);
            if (success) {
                return ApiResponse.success("2FA禁用成功");
            } else {
                return ApiResponse.error("验证码错误或2FA禁用失败");
            }
        } catch (Exception e) {
            log.error("禁用2FA失败", e);
            return ApiResponse.error("禁用2FA失败");
        }
    }

    /**
     * 验证2FA代码
     */
    @PostMapping("/verify")
    public ApiResponse<String> verify(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            String verificationCode = request.get("verificationCode");
            String backupCode = request.get("backupCode");

            boolean success = false;
            if (verificationCode != null && !verificationCode.trim().isEmpty()) {
                success = twoFactorAuthService.verifyTwoFactorCode(userId, verificationCode);
            } else if (backupCode != null && !backupCode.trim().isEmpty()) {
                success = twoFactorAuthService.verifyBackupCode(userId, backupCode);
            }

            if (success) {
                return ApiResponse.success("验证成功");
            } else {
                return ApiResponse.error("验证码错误");
            }
        } catch (Exception e) {
            log.error("验证2FA失败", e);
            return ApiResponse.error("验证2FA失败");
        }
    }

    /**
     * 获取备用恢复码状态（不返回实际代码，只返回是否存在）
     */
    @GetMapping("/backup-codes")
    public ApiResponse<Map<String, Object>> getBackupCodesStatus(HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            TwoFactorAuth twoFactorAuth = twoFactorAuthService.getTwoFactorAuth(userId);
            if (twoFactorAuth == null || !twoFactorAuth.getEnabled()) {
                return ApiResponse.error("2FA未启用");
            }

            // 只返回备用代码的状态信息，不返回实际代码
            Map<String, Object> status = new HashMap<>();
            try {
                String backupCodesJson = twoFactorAuth.getBackupCodes();
                if (backupCodesJson != null && !backupCodesJson.trim().isEmpty()) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String[] backupCodes = mapper.readValue(backupCodesJson, String[].class);
                    status.put("hasBackupCodes", true);
                    status.put("backupCodesCount", backupCodes.length);
                } else {
                    status.put("hasBackupCodes", false);
                    status.put("backupCodesCount", 0);
                }
            } catch (Exception e) {
                log.error("解析备用代码失败", e);
                status.put("hasBackupCodes", false);
                status.put("backupCodesCount", 0);
            }

            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取备用恢复码状态失败", e);
            return ApiResponse.error("获取备用恢复码状态失败");
        }
    }

    /**
     * 重新生成备用恢复码
     */
    @PostMapping("/regenerate-backup-codes")
    public ApiResponse<String[]> regenerateBackupCodes(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String userId = (String) httpRequest.getAttribute("currentUser");
            if (userId == null) {
                userId = "admin"; // 默认用户
            }

            String verificationCode = request.get("verificationCode");
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return ApiResponse.error("验证码不能为空");
            }

            String[] newBackupCodes = twoFactorAuthService.regenerateBackupCodes(userId, verificationCode);
            if (newBackupCodes != null) {
                return ApiResponse.success(newBackupCodes);
            } else {
                return ApiResponse.error("验证码错误或重新生成失败");
            }
        } catch (Exception e) {
            log.error("重新生成备用恢复码失败", e);
            return ApiResponse.error("重新生成备用恢复码失败");
        }
    }
}
