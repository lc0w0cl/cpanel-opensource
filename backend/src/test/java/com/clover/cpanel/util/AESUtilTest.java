package com.clover.cpanel.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AES加密工具类测试
 */
public class AESUtilTest {

    @Test
    public void testGenerateKey() {
        String key = AESUtil.generateKey();
        assertNotNull(key);
        assertFalse(key.isEmpty());
        System.out.println("Generated key: " + key);
    }

    @Test
    public void testEncryptDecrypt() {
        String key = AESUtil.generateKey();
        String plainText = "这是一个测试密码123!@#";
        
        // 加密
        String encrypted = AESUtil.encrypt(plainText, key);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        System.out.println("Original: " + plainText);
        System.out.println("Encrypted: " + encrypted);
        
        // 解密
        String decrypted = AESUtil.decrypt(encrypted, key);
        assertEquals(plainText, decrypted);
        System.out.println("Decrypted: " + decrypted);
    }

    @Test
    public void testEncryptDecryptPrivateKey() {
        String key = AESUtil.generateKey();
        String privateKey = """
                -----BEGIN OPENSSH PRIVATE KEY-----
                b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAFwAAAAdzc2gtcn
                NhAAAAAwEAAQAAAQEA1234567890abcdefghijklmnopqrstuvwxyz
                -----END OPENSSH PRIVATE KEY-----
                """;
        
        // 加密
        String encrypted = AESUtil.encrypt(privateKey, key);
        assertNotNull(encrypted);
        assertNotEquals(privateKey, encrypted);
        
        // 解密
        String decrypted = AESUtil.decrypt(encrypted, key);
        assertEquals(privateKey, decrypted);
    }

    @Test
    public void testIsEncrypted() {
        String key = AESUtil.generateKey();
        String plainText = "test password";
        
        // 明文应该返回false
        assertFalse(AESUtil.isEncrypted(plainText));
        
        // 加密后应该返回true
        String encrypted = AESUtil.encrypt(plainText, key);
        assertTrue(AESUtil.isEncrypted(encrypted));
        
        // 空字符串和null应该返回false
        assertFalse(AESUtil.isEncrypted(""));
        assertFalse(AESUtil.isEncrypted(null));
    }

    @Test
    public void testEncryptEmptyAndNull() {
        String key = AESUtil.generateKey();
        
        // 测试空字符串
        String emptyEncrypted = AESUtil.encrypt("", key);
        assertEquals("", emptyEncrypted);
        
        // 测试null
        String nullEncrypted = AESUtil.encrypt(null, key);
        assertNull(nullEncrypted);
    }

    @Test
    public void testDecryptEmptyAndNull() {
        String key = AESUtil.generateKey();
        
        // 测试空字符串
        String emptyDecrypted = AESUtil.decrypt("", key);
        assertEquals("", emptyDecrypted);
        
        // 测试null
        String nullDecrypted = AESUtil.decrypt(null, key);
        assertNull(nullDecrypted);
    }

    @Test
    public void testDifferentKeys() {
        String key1 = AESUtil.generateKey();
        String key2 = AESUtil.generateKey();
        String plainText = "test password";
        
        // 用key1加密
        String encrypted = AESUtil.encrypt(plainText, key1);
        
        // 用key1解密应该成功
        String decrypted1 = AESUtil.decrypt(encrypted, key1);
        assertEquals(plainText, decrypted1);
        
        // 用key2解密应该失败（抛出异常）
        assertThrows(RuntimeException.class, () -> {
            AESUtil.decrypt(encrypted, key2);
        });
    }
}
