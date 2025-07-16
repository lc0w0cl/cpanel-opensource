#!/bin/bash

# 生成AES加密密钥脚本
# 用于为CPanel应用生成安全的AES-256加密密钥

echo "=== CPanel AES加密密钥生成工具 ==="
echo ""

# 检查是否安装了openssl
if ! command -v openssl &> /dev/null; then
    echo "❌ 错误: 未找到openssl命令"
    echo "请先安装openssl:"
    echo "  Ubuntu/Debian: sudo apt-get install openssl"
    echo "  CentOS/RHEL: sudo yum install openssl"
    echo "  macOS: brew install openssl"
    exit 1
fi

# 生成32字节(256位)的随机密钥并进行Base64编码
echo "🔑 正在生成AES-256加密密钥..."
AES_KEY=$(openssl rand -base64 32)

echo ""
echo "✅ 密钥生成成功！"
echo ""
echo "📋 请将以下密钥添加到您的环境变量中:"
echo ""
echo "ENCRYPTION_AES_KEY=$AES_KEY"
echo ""
echo "📝 使用方法:"
echo ""
echo "1. 在 .env 文件中添加:"
echo "   ENCRYPTION_AES_KEY=$AES_KEY"
echo ""
echo "2. 或者在 docker-compose.yml 中设置环境变量:"
echo "   environment:"
echo "     ENCRYPTION_AES_KEY: $AES_KEY"
echo ""
echo "3. 或者在系统环境变量中设置:"
echo "   export ENCRYPTION_AES_KEY=\"$AES_KEY\""
echo ""
echo "⚠️  重要提醒:"
echo "   - 请妥善保管此密钥，丢失后将无法解密已加密的数据"
echo "   - 不要在日志或版本控制系统中暴露此密钥"
echo "   - 建议定期备份密钥到安全位置"
echo ""

# 询问是否要将密钥写入.env文件
if [ -f ".env" ]; then
    echo "🔍 检测到 .env 文件存在"
    read -p "是否要将密钥自动添加到 .env 文件中? (y/N): " -n 1 -r
    echo ""
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # 检查是否已存在ENCRYPTION_AES_KEY配置
        if grep -q "^ENCRYPTION_AES_KEY=" .env; then
            echo "⚠️  .env 文件中已存在 ENCRYPTION_AES_KEY 配置"
            read -p "是否要覆盖现有配置? (y/N): " -n 1 -r
            echo ""
            
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                # 替换现有配置
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    # macOS
                    sed -i '' "s/^ENCRYPTION_AES_KEY=.*/ENCRYPTION_AES_KEY=$AES_KEY/" .env
                else
                    # Linux
                    sed -i "s/^ENCRYPTION_AES_KEY=.*/ENCRYPTION_AES_KEY=$AES_KEY/" .env
                fi
                echo "✅ 已更新 .env 文件中的加密密钥"
            else
                echo "❌ 已取消更新"
            fi
        else
            # 添加新配置
            echo "" >> .env
            echo "# AES加密密钥" >> .env
            echo "ENCRYPTION_AES_KEY=$AES_KEY" >> .env
            echo "✅ 已将加密密钥添加到 .env 文件"
        fi
    else
        echo "❌ 已跳过自动添加到 .env 文件"
    fi
else
    echo "💡 提示: 未找到 .env 文件，请手动创建并添加密钥配置"
fi

echo ""
echo "🎉 密钥生成完成！"
