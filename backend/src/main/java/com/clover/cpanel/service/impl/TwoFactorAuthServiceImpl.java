package com.clover.cpanel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clover.cpanel.entity.TwoFactorAuth;
import com.clover.cpanel.mapper.TwoFactorAuthMapper;
import com.clover.cpanel.service.TwoFactorAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

/**
 * 2FA认证服务实现类
 */
@Slf4j
@Service
public class TwoFactorAuthServiceImpl extends ServiceImpl<TwoFactorAuthMapper, TwoFactorAuth> implements TwoFactorAuthService {

    @Autowired
    private ObjectMapper objectMapper;

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public TwoFactorAuth getTwoFactorAuth(String userId) {
        QueryWrapper<TwoFactorAuth> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.getOne(queryWrapper);
    }

    @Override
    public TwoFactorAuth generateTwoFactorAuth(String userId) {
        // 生成新的密钥
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secretKey = key.getKey();

        // 生成备用恢复码
        String[] backupCodes = generateBackupCodes();

        // 检查是否已存在配置
        TwoFactorAuth existingAuth = getTwoFactorAuth(userId);
        if (existingAuth != null) {
            // 更新现有配置
            existingAuth.setSecretKey(secretKey);
            existingAuth.setBackupCodes(arrayToJson(backupCodes));
            existingAuth.setEnabled(false); // 重新生成后需要重新启用
            this.updateById(existingAuth);
            return existingAuth;
        } else {
            // 创建新配置
            TwoFactorAuth twoFactorAuth = new TwoFactorAuth();
            twoFactorAuth.setUserId(userId);
            twoFactorAuth.setSecretKey(secretKey);
            twoFactorAuth.setEnabled(false);
            twoFactorAuth.setBackupCodes(arrayToJson(backupCodes));
            this.save(twoFactorAuth);
            return twoFactorAuth;
        }
    }

    @Override
    public boolean enableTwoFactorAuth(String userId, String verificationCode) {
        TwoFactorAuth twoFactorAuth = getTwoFactorAuth(userId);
        if (twoFactorAuth == null || twoFactorAuth.getSecretKey() == null) {
            log.warn("用户 {} 尝试启用2FA但未找到配置", userId);
            return false;
        }

        // 验证代码
        if (!verifyCode(twoFactorAuth.getSecretKey(), verificationCode)) {
            log.warn("用户 {} 启用2FA时验证码错误", userId);
            return false;
        }

        // 启用2FA
        twoFactorAuth.setEnabled(true);
        twoFactorAuth.setLastUsedCode(verificationCode);
        twoFactorAuth.setLastUsedTime(System.currentTimeMillis());
        boolean result = this.updateById(twoFactorAuth);

        if (result) {
            log.info("用户 {} 成功启用2FA", userId);
        }
        return result;
    }

    @Override
    public boolean disableTwoFactorAuth(String userId, String verificationCode) {
        TwoFactorAuth twoFactorAuth = getTwoFactorAuth(userId);
        if (twoFactorAuth == null || !twoFactorAuth.getEnabled()) {
            log.warn("用户 {} 尝试禁用2FA但未启用", userId);
            return false;
        }

        // 验证代码
        if (!verifyTwoFactorCode(userId, verificationCode)) {
            log.warn("用户 {} 禁用2FA时验证码错误", userId);
            return false;
        }

        // 禁用2FA
        twoFactorAuth.setEnabled(false);
        boolean result = this.updateById(twoFactorAuth);

        if (result) {
            log.info("用户 {} 成功禁用2FA", userId);
        }
        return result;
    }

    @Override
    public boolean verifyTwoFactorCode(String userId, String verificationCode) {
        TwoFactorAuth twoFactorAuth = getTwoFactorAuth(userId);
        if (twoFactorAuth == null || !twoFactorAuth.getEnabled()) {
            return false;
        }

        // 检查是否是重复使用的验证码
        if (isCodeRecentlyUsed(twoFactorAuth, verificationCode)) {
            log.warn("用户 {} 尝试重复使用验证码", userId);
            return false;
        }

        // 验证代码
        boolean isValid = verifyCode(twoFactorAuth.getSecretKey(), verificationCode);
        if (isValid) {
            // 更新最后使用的验证码
            twoFactorAuth.setLastUsedCode(verificationCode);
            twoFactorAuth.setLastUsedTime(System.currentTimeMillis());
            this.updateById(twoFactorAuth);
            log.info("用户 {} 2FA验证成功", userId);
        } else {
            log.warn("用户 {} 2FA验证失败", userId);
        }

        return isValid;
    }

    @Override
    public boolean verifyBackupCode(String userId, String backupCode) {
        TwoFactorAuth twoFactorAuth = getTwoFactorAuth(userId);
        if (twoFactorAuth == null || !twoFactorAuth.getEnabled()) {
            return false;
        }

        try {
            List<String> backupCodes = jsonToArray(twoFactorAuth.getBackupCodes());
            if (backupCodes.contains(backupCode)) {
                // 移除已使用的备用码
                backupCodes.remove(backupCode);
                twoFactorAuth.setBackupCodes(arrayToJson(backupCodes.toArray(new String[0])));
                this.updateById(twoFactorAuth);
                log.info("用户 {} 使用备用恢复码验证成功", userId);
                return true;
            }
        } catch (Exception e) {
            log.error("验证备用恢复码时发生错误", e);
        }

        log.warn("用户 {} 备用恢复码验证失败", userId);
        return false;
    }

    @Override
    public String generateQRCodeImage(String secretKey, String userId, String issuer) {
        try {
            String qrCodeText = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, userId, secretKey, issuer
            );

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 200, 200);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (WriterException | IOException e) {
            log.error("生成QR码失败", e);
            return null;
        }
    }

    @Override
    public boolean isTwoFactorEnabled(String userId) {
        TwoFactorAuth twoFactorAuth = getTwoFactorAuth(userId);
        return twoFactorAuth != null && twoFactorAuth.getEnabled();
    }

    @Override
    public String[] regenerateBackupCodes(String userId, String verificationCode) {
        TwoFactorAuth twoFactorAuth = getTwoFactorAuth(userId);
        if (twoFactorAuth == null || !twoFactorAuth.getEnabled()) {
            return null;
        }

        // 验证代码
        if (!verifyTwoFactorCode(userId, verificationCode)) {
            return null;
        }

        // 生成新的备用恢复码
        String[] newBackupCodes = generateBackupCodes();
        twoFactorAuth.setBackupCodes(arrayToJson(newBackupCodes));
        this.updateById(twoFactorAuth);

        log.info("用户 {} 重新生成备用恢复码", userId);
        return newBackupCodes;
    }

    /**
     * 验证TOTP代码
     */
    private boolean verifyCode(String secretKey, String verificationCode) {
        try {
            int code = Integer.parseInt(verificationCode);
            return googleAuthenticator.authorize(secretKey, code);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查验证码是否最近使用过
     */
    private boolean isCodeRecentlyUsed(TwoFactorAuth twoFactorAuth, String verificationCode) {
        if (twoFactorAuth.getLastUsedCode() == null || twoFactorAuth.getLastUsedTime() == null) {
            return false;
        }

        // 如果是相同的验证码且在30秒内使用过，则认为是重复使用
        long timeDiff = System.currentTimeMillis() - twoFactorAuth.getLastUsedTime();
        return verificationCode.equals(twoFactorAuth.getLastUsedCode()) && timeDiff < 30000;
    }

    /**
     * 生成备用恢复码
     */
    private String[] generateBackupCodes() {
        String[] codes = new String[10];
        for (int i = 0; i < 10; i++) {
            codes[i] = generateRandomCode();
        }
        return codes;
    }

    /**
     * 生成随机恢复码
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 数组转JSON
     */
    private String arrayToJson(String[] array) {
        try {
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            log.error("数组转JSON失败", e);
            return "[]";
        }
    }

    /**
     * JSON转数组
     */
    private List<String> jsonToArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("JSON转数组失败", e);
            return new ArrayList<>();
        }
    }
}
