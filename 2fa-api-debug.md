# 2FA API 调试指南

## 问题描述
前端访问2FA API时收到HTML响应而不是JSON，错误信息：
```
SyntaxError: Unexpected token '<', "<!DOCTYPE "... is not valid JSON
```

## 可能的原因

### 1. 后端服务未启动
- 后端Spring Boot应用没有运行
- 端口8080没有被监听

### 2. API路由未正确注册
- TwoFactorAuthController没有被Spring扫描到
- 路由映射有问题

### 3. 依赖问题
- 新添加的Google Authenticator依赖没有正确加载
- Maven依赖冲突

### 4. 数据库问题
- 新的2FA表没有创建
- 数据库连接失败

## 诊断步骤

### 步骤1: 检查后端服务状态
```bash
# 检查端口是否被监听
netstat -an | findstr :8080

# 或者使用PowerShell
Get-NetTCPConnection -LocalPort 8080
```

### 步骤2: 重新编译和启动后端
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

### 步骤3: 检查启动日志
查看后端启动日志中是否有：
- TwoFactorAuthController被注册
- 2FA相关的路由映射
- 数据库表创建成功
- 依赖加载成功

### 步骤4: 使用调试页面测试
访问 `http://localhost:3000/debug-2fa` 页面进行API连接测试。

### 步骤5: 手动测试API
```bash
# 测试基础API连接
curl -X GET http://localhost:8080/api/system/info

# 测试2FA状态API
curl -X GET http://localhost:8080/api/2fa/status
```

## 修复方案

### 方案1: 重新启动后端服务
1. 停止当前的后端服务
2. 重新编译项目：`mvn clean compile`
3. 重新启动：`mvn spring-boot:run`

### 方案2: 检查依赖
确保 `pom.xml` 中的新依赖正确添加：
```xml
<dependency>
    <groupId>com.warrenstrange</groupId>
    <artifactId>googleauth</artifactId>
    <version>1.5.0</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

### 方案3: 检查数据库
确保2FA表正确创建：
```sql
SHOW TABLES LIKE 'panel_two_factor_auth';
DESCRIBE panel_two_factor_auth;
```

### 方案4: 临时禁用2FA检查
如果需要快速测试其他功能，可以临时修改前端代码：

在 `useTwoFactorAuth.ts` 中：
```typescript
const checkTwoFactorRequired = async (): Promise<boolean> => {
  // 临时返回false，跳过2FA检查
  return false
}
```

## 前端改进

我已经在 `useTwoFactorAuth.ts` 中添加了详细的调试日志：
- 请求URL记录
- 响应状态检查
- 错误详情输出

这些日志会在浏览器控制台中显示，帮助诊断问题。

## 验证步骤

1. **启动后端服务**
2. **访问调试页面**: `http://localhost:3000/debug-2fa`
3. **点击"测试API连接"**检查基础连接
4. **点击"测试2FA状态"**检查2FA API
5. **查看浏览器控制台**获取详细错误信息

## 常见解决方案

### 如果返回404
- 检查TwoFactorAuthController是否正确注册
- 检查@RequestMapping路径是否正确

### 如果返回500
- 检查数据库连接
- 检查2FA表是否存在
- 检查依赖是否正确加载

### 如果返回HTML页面
- 可能是Spring Boot的错误页面
- 检查启动日志中的异常信息
- 可能是路由配置问题

按照这个诊断流程，应该能够快速定位和解决2FA API的问题。
