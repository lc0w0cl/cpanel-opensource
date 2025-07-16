package com.clover.cpanel.service;

import com.clover.cpanel.entity.TwoFactorAuth;

/**
 * 2FA认证服务接口
 */
public interface TwoFactorAuthService {

    /**
     * 获取用户的2FA配置
     * @param userId 用户ID
     * @return 2FA配置，如果不存在则返回null
     */
    TwoFactorAuth getTwoFactorAuth(String userId);

    /**
     * 生成2FA密钥和QR码
     * @param userId 用户ID
     * @return 包含密钥和QR码的配置信息
     */
    TwoFactorAuth generateTwoFactorAuth(String userId);

    /**
     * 启用2FA
     * @param userId 用户ID
     * @param verificationCode 验证码
     * @return 是否启用成功
     */
    boolean enableTwoFactorAuth(String userId, String verificationCode);

    /**
     * 禁用2FA
     * @param userId 用户ID
     * @param verificationCode 验证码
     * @return 是否禁用成功
     */
    boolean disableTwoFactorAuth(String userId, String verificationCode);

    /**
     * 验证2FA代码
     * @param userId 用户ID
     * @param verificationCode 验证码
     * @return 是否验证成功
     */
    boolean verifyTwoFactorCode(String userId, String verificationCode);

    /**
     * 验证备用恢复码
     * @param userId 用户ID
     * @param backupCode 备用恢复码
     * @return 是否验证成功
     */
    boolean verifyBackupCode(String userId, String backupCode);

    /**
     * 生成QR码图片的Base64编码
     * @param secretKey 密钥
     * @param userId 用户ID
     * @param issuer 发行者名称
     * @return QR码图片的Base64编码
     */
    String generateQRCodeImage(String secretKey, String userId, String issuer);

    /**
     * 检查用户是否启用了2FA
     * @param userId 用户ID
     * @return 是否启用了2FA
     */
    boolean isTwoFactorEnabled(String userId);

    /**
     * 重新生成备用恢复码
     * @param userId 用户ID
     * @param verificationCode 验证码
     * @return 新的备用恢复码列表
     */
    String[] regenerateBackupCodes(String userId, String verificationCode);
}
