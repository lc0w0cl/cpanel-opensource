package com.clover.cpanel.util;

/**
 * 密钥生成工具
 * 可以独立运行来生成AES加密密钥
 */
public class KeyGenerator {

    /**
     * 主方法，用于生成AES密钥
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("=== CPanel AES加密密钥生成工具 ===");
        System.out.println();
        
        try {
            // 生成AES密钥
            String aesKey = AESUtil.generateKey();
            
            System.out.println("✅ 密钥生成成功！");
            System.out.println();
            System.out.println("📋 请将以下密钥添加到您的环境变量中:");
            System.out.println();
            System.out.println("ENCRYPTION_AES_KEY=" + aesKey);
            System.out.println();
            System.out.println("📝 使用方法:");
            System.out.println();
            System.out.println("1. 在 .env 文件中添加:");
            System.out.println("   ENCRYPTION_AES_KEY=" + aesKey);
            System.out.println();
            System.out.println("2. 或者在 docker-compose.yml 中设置环境变量:");
            System.out.println("   environment:");
            System.out.println("     ENCRYPTION_AES_KEY: " + aesKey);
            System.out.println();
            System.out.println("3. 或者在系统环境变量中设置:");
            System.out.println("   export ENCRYPTION_AES_KEY=\"" + aesKey + "\"");
            System.out.println();
            System.out.println("⚠️  重要提醒:");
            System.out.println("   - 请妥善保管此密钥，丢失后将无法解密已加密的数据");
            System.out.println("   - 不要在日志或版本控制系统中暴露此密钥");
            System.out.println("   - 建议定期备份密钥到安全位置");
            System.out.println();
            System.out.println("🎉 密钥生成完成！");
            
        } catch (Exception e) {
            System.err.println("❌ 密钥生成失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
