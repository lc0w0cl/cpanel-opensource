package com.clover.cpanel.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密工具类
 * 使用AES-256-GCM模式进行加密，提供认证加密功能
 */
@Slf4j
@Component
public class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // GCM模式推荐的IV长度
    private static final int GCM_TAG_LENGTH = 16; // GCM认证标签长度
    private static final int KEY_LENGTH = 256; // AES-256

    /**
     * 生成AES密钥
     * @return Base64编码的密钥字符串
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("生成AES密钥失败", e);
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }

    /**
     * 加密文本
     * @param plainText 明文
     * @param keyString Base64编码的密钥
     * @return Base64编码的密文（包含IV）
     */
    public static String encrypt(String plainText, String keyString) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 解码密钥
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // 加密
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文合并
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, encryptedWithIv, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * 解密文本
     * @param encryptedText Base64编码的密文（包含IV）
     * @param keyString Base64编码的密钥
     * @return 明文
     */
    public static String decrypt(String encryptedText, String keyString) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // 解码密钥
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // 解码密文
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);

            // 分离IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // 解密
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 检查文本是否已加密（简单检查是否为Base64格式且长度合理）
     * @param text 待检查的文本
     * @return 是否已加密
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            // 检查是否为有效的Base64
            byte[] decoded = Base64.getDecoder().decode(text);
            // 检查长度是否合理（至少包含IV + 最小密文长度）
            return decoded.length > GCM_IV_LENGTH + GCM_TAG_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
