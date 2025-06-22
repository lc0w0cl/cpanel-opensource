# JWT认证系统实现文档

## 📋 概述

本项目已成功实现基于JWT（JSON Web Token）的认证系统，替换了原有的Session-based认证方式。新系统提供了更好的安全性、可扩展性和前后端分离支持。

## 🔧 后端实现

### 1. 依赖配置

在 `backend/pom.xml` 中添加了JWT相关依赖：

```xml
<!-- JWT 依赖 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### 2. 核心组件

#### JWT工具类 (`JwtUtil.java`)
- **功能**: 生成、验证、解析JWT token
- **特性**: 
  - 支持访问token和刷新token
  - 自动过期检查
  - 安全的密钥管理

#### JWT配置类 (`JwtConfig.java`)
- **功能**: 集中管理JWT相关配置
- **配置项**: 密钥、过期时间、发行者等

#### JWT拦截器 (`JwtAuthInterceptor.java`)
- **功能**: 拦截API请求，验证JWT token
- **特性**: 
  - 自动token验证
  - 白名单路径支持
  - 友好的错误响应

#### Web配置类 (`WebConfig.java`)
- **功能**: 注册拦截器和CORS配置
- **特性**: 统一的跨域和认证配置

### 3. API接口更新

#### AuthController 更新
- **登录接口** (`/api/auth/login`): 返回JWT token而非设置session
- **状态检查** (`/api/auth/status`): 基于JWT token验证
- **token刷新** (`/api/auth/refresh`): 刷新访问token
- **登出接口** (`/api/auth/logout`): 清理token（可扩展黑名单）

### 4. 配置文件

在 `application.yml` 中添加JWT配置：

```yaml
# JWT配置
jwt:
  # JWT密钥（生产环境请使用更复杂的密钥）
  secret: cpanel-jwt-secret-key-for-authentication-system-2024-very-long-secret
  # 访问token过期时间（毫秒）- 24小时
  expiration: 86400000
  # 刷新token过期时间（毫秒）- 7天
  refresh-expiration: 604800000
  # 发行者
  issuer: cpanel
  # 受众
  audience: cpanel-users
```

## 🎨 前端实现

### 1. JWT工具函数 (`composables/useJwt.ts`)

提供完整的JWT客户端管理功能：

- **Token存储**: localStorage + sessionStorage双重存储
- **Token验证**: 本地解析和过期检查
- **自动刷新**: 智能token刷新机制
- **API请求**: 带认证头的请求函数

### 2. 认证中间件 (`middleware/auth.ts`)

- **路由保护**: 自动检查用户认证状态
- **智能缓存**: 减少不必要的API调用
- **错误处理**: 优雅的网络错误处理
- **自动重定向**: 未认证用户自动跳转登录页

### 3. 登录页面更新 (`pages/login.vue`)

- **JWT集成**: 登录成功后存储JWT token
- **状态检查**: 已登录用户自动重定向
- **错误处理**: 友好的错误消息显示
- **UI优化**: 保持原有的液态玻璃设计风格

### 4. 组件更新

#### Sidebar组件
- **登出功能**: 集成JWT登出逻辑
- **加载状态**: 登出过程的视觉反馈
- **错误处理**: 登出失败的容错处理

#### API调用更新
- **统一认证**: 所有API调用使用JWT认证
- **自动重试**: token过期时自动刷新并重试
- **错误处理**: 401错误的智能处理

### 5. 页面中间件应用

所有受保护的页面都应用了认证中间件：
- `pages/index.vue`
- `pages/navigation-panel.vue`
- `pages/settings.vue`
- `pages/about.vue`

## 🔐 安全特性

### 1. Token安全
- **双token机制**: 访问token + 刷新token
- **短期有效**: 访问token 24小时过期
- **安全存储**: 本地存储 + 会话存储
- **自动清理**: 登出时清除所有token

### 2. 传输安全
- **HTTPS支持**: 生产环境强制HTTPS
- **CORS配置**: 严格的跨域访问控制
- **请求头验证**: Bearer token标准格式

### 3. 后端验证
- **签名验证**: HMAC-SHA256签名算法
- **过期检查**: 服务端严格过期时间验证
- **路径白名单**: 精确的API路径控制

## 🚀 使用方式

### 1. 用户登录流程
1. 用户访问 `/login` 页面
2. 输入密码并提交
3. 后端验证密码并返回JWT token
4. 前端存储token并重定向到首页

### 2. API请求流程
1. 前端自动在请求头中添加 `Authorization: Bearer <token>`
2. 后端拦截器验证token有效性
3. 验证通过则继续处理请求
4. 验证失败则返回401错误

### 3. Token刷新流程
1. 前端检测访问token即将过期
2. 使用刷新token调用 `/api/auth/refresh`
3. 后端返回新的token对
4. 前端更新本地存储的token

### 4. 登出流程
1. 用户点击登出按钮
2. 前端调用 `/api/auth/logout` API
3. 清除本地存储的所有token
4. 重定向到登录页面

## 📝 配置说明

### 开发环境
- JWT密钥: 使用默认密钥（仅开发用）
- Token过期时间: 24小时（访问token）/ 7天（刷新token）
- API地址: `http://localhost:8080`

### 生产环境建议
- JWT密钥: 使用环境变量设置复杂密钥
- Token过期时间: 根据安全需求调整
- HTTPS: 强制使用HTTPS传输
- 密钥轮换: 定期更换JWT密钥

## 🔄 迁移说明

### 从Session到JWT的变化
1. **认证方式**: Session Cookie → JWT Token
2. **存储位置**: 服务端Session → 客户端Token
3. **传输方式**: Cookie → Authorization Header
4. **状态管理**: 有状态 → 无状态

### 兼容性处理
- 保持API接口路径不变
- 保持前端页面结构不变
- 保持用户体验一致性

## 🛠️ 故障排除

### 常见问题
1. **Token过期**: 检查系统时间同步
2. **CORS错误**: 确认前后端域名配置
3. **401错误**: 检查token格式和有效性
4. **网络错误**: 检查API地址配置

### 调试方法
1. 浏览器开发者工具查看Network请求
2. 检查localStorage中的token存储
3. 后端日志查看JWT验证过程
4. 使用jwt.io解析token内容

## 📈 性能优化

### 前端优化
- 智能缓存减少API调用
- 后台token刷新不阻塞用户操作
- 本地token验证减少网络请求

### 后端优化
- 无状态设计支持水平扩展
- 拦截器高效token验证
- 白名单减少不必要验证

## 🔮 未来扩展

### 可能的增强功能
1. **Token黑名单**: 实现token撤销机制
2. **多设备管理**: 支持多设备登录管理
3. **权限系统**: 基于JWT的细粒度权限控制
4. **审计日志**: 详细的认证和授权日志
5. **SSO集成**: 单点登录系统集成

---

## 总结

JWT认证系统的实现大大提升了应用的安全性和可扩展性。新系统保持了原有的用户体验，同时为未来的功能扩展奠定了坚实的基础。所有的API调用现在都通过JWT token进行认证，确保了系统的安全性和一致性。
