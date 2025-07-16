# 服务器敏感信息加密功能

## 概述

为了提高系统安全性，本项目已实现对服务器敏感信息的AES对称加密功能。所有服务器的密码、私钥和私钥密码都将在存储到数据库前进行加密，在使用时自动解密。

## 加密算法

- **算法**: AES-256-GCM
- **密钥长度**: 256位
- **模式**: GCM（Galois/Counter Mode）
- **特点**: 提供认证加密，确保数据的机密性和完整性

## 加密的字段

以下服务器配置字段会被自动加密：

1. `password` - 服务器登录密码
2. `private_key` - SSH私钥内容
3. `private_key_password` - 私钥密码

## 配置说明

### 基本配置

在 `application.yml` 或 `module.yml` 中：

```yaml
# 加密配置
encryption:
  # 是否启用加密功能
  enabled: true
  # AES加密配置
  aes:
    # AES加密密钥（可选，如果不设置会自动生成）
    key: ${ENCRYPTION_AES_KEY:}
```

### 环境变量配置（推荐）

生产环境建议通过环境变量设置加密密钥：

```bash
# 设置AES加密密钥
export ENCRYPTION_AES_KEY="your-base64-encoded-aes-key"

# 启用/禁用加密功能
export ENCRYPTION_ENABLED=true
```

### Docker环境配置

在 `docker-compose.yml` 中：

```yaml
services:
  cpanel:
    environment:
      - ENCRYPTION_ENABLED=true
      - ENCRYPTION_AES_KEY=your-base64-encoded-aes-key
```

## 密钥管理

### 自动生成密钥

如果未配置 `ENCRYPTION_AES_KEY` 环境变量，系统会在启动时自动生成一个新的AES密钥。

**注意**: 自动生成的密钥只在当前运行实例中有效，重启后会生成新密钥，导致无法解密之前的数据。

### 手动生成密钥

#### 方法1: 使用提供的脚本

**Linux/macOS:**
```bash
./scripts/generate-encryption-key.sh
```

**Windows:**
```cmd
scripts\generate-encryption-key.bat
```

#### 方法2: 使用Java工具类

```bash
cd backend
mvn compile exec:java -Dexec.mainClass="com.clover.cpanel.util.KeyGenerator"
```

#### 方法3: 使用命令行工具

**使用OpenSSL:**
```bash
openssl rand -base64 32
```

**使用PowerShell (Windows):**
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

#### 方法4: 使用Java代码

```java
String key = AESUtil.generateKey();
System.out.println("Generated AES Key: " + key);
```

### 密钥安全性

1. **生产环境**: 必须使用固定的密钥，通过环境变量设置
2. **备份密钥**: 请妥善保管加密密钥，丢失密钥将无法恢复加密数据
3. **密钥轮换**: 如需更换密钥，需要先解密所有数据，更换密钥后重新加密

## 数据迁移

### 自动迁移

系统启动时会自动检查并执行数据加密迁移：

1. 检查是否启用加密功能
2. 检查是否已完成迁移
3. 如果需要，自动将现有明文数据加密

### 手动迁移

可以通过API接口手动触发迁移：

```bash
# 检查加密状态
curl -X GET http://localhost:8080/api/encryption/status

# 执行迁移
curl -X POST http://localhost:8080/api/encryption/migrate

# 强制重新迁移
curl -X POST http://localhost:8080/api/encryption/force-migrate
```

## API接口

### 获取加密状态

```http
GET /api/encryption/status
```

响应示例：
```json
{
  "success": true,
  "data": {
    "encryptionEnabled": true,
    "migrationStatus": "数据加密迁移已完成",
    "needsMigration": false,
    "hasCustomKey": true
  }
}
```

### 获取加密配置

```http
GET /api/encryption/config
```

响应示例：
```json
{
  "success": true,
  "data": {
    "encryptionEnabled": true,
    "algorithm": "AES-256-GCM",
    "keySource": "环境变量"
  }
}
```

## 安全注意事项

1. **密钥保护**: 加密密钥是系统安全的关键，请妥善保管
2. **传输安全**: 确保API通信使用HTTPS
3. **访问控制**: 加密管理接口应该有适当的访问控制
4. **日志安全**: 确保敏感信息不会出现在日志中
5. **备份策略**: 制定包含密钥的完整备份策略

## 故障排除

### 常见问题

1. **解密失败**: 检查密钥是否正确，是否发生了密钥变更
2. **迁移失败**: 检查数据库连接和权限
3. **性能影响**: 加密/解密操作会有轻微的性能开销

### 日志查看

查看应用日志中的加密相关信息：

```bash
# 查看迁移日志
grep "数据加密迁移" application.log

# 查看加密错误
grep "加密失败\|解密失败" application.log
```

## 开发说明

### 添加新的敏感字段

如需对新字段进行加密：

1. 在 `ServerEncryptionService` 中添加加密/解密逻辑
2. 在 `DataEncryptionMigrationService` 中添加迁移逻辑
3. 更新相关的服务层代码

### 测试

运行加密功能测试：

```bash
mvn test -Dtest=AESUtilTest
```

## 版本兼容性

- 支持从明文数据自动迁移到加密数据
- 向后兼容：可以处理混合的明文和加密数据
- 前向兼容：新版本可以读取旧版本的加密数据
