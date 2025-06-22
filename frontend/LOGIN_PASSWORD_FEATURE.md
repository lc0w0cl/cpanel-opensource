# 面板登录密码功能说明

## 功能概述

新增了面板登录密码功能，用户第一次访问时需要输入密码登录，登录后可以在设置页面修改密码。这为导航面板提供了基本的安全保护。

## 主要功能

### 1. 密码验证

- **默认密码**：首次使用时默认密码为 "admin"
- **密码存储**：密码经过MD5哈希后存储在数据库中
- **会话管理**：登录后使用HTTP Session管理登录状态

### 2. 密码设置

- **密码修改**：在设置页面可以修改登录密码
- **密码验证**：修改时需要输入当前密码验证
- **密码强度**：要求密码至少4位字符

### 3. 安全特性

- **会话保护**：未登录用户无法访问受保护的API
- **密码哈希**：密码不以明文存储
- **状态检查**：提供登录状态检查接口

## 技术实现

### 1. 后端实现

#### 数据库设计

```sql
-- 系统配置表
CREATE TABLE system_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL UNIQUE,
  config_value TEXT,
  description VARCHAR(255),
  created_at VARCHAR(19),
  updated_at VARCHAR(19),
  INDEX idx_config_key (config_key)
);
```

#### 核心API接口

1. **登录验证** - `POST /api/auth/login`
2. **登录状态检查** - `GET /api/auth/status`
3. **登出** - `POST /api/auth/logout`
4. **修改密码** - `POST /api/auth/change-password`

#### 服务层实现

```java
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    
    // 验证登录密码
    public boolean verifyLoginPassword(String password) {
        String storedPasswordHash = getConfigValue("login_password");
        if (storedPasswordHash == null) {
            return "admin".equals(password.trim()); // 默认密码
        }
        String inputPasswordHash = hashPassword(password.trim());
        return storedPasswordHash.equals(inputPasswordHash);
    }
    
    // 设置登录密码
    public boolean setLoginPassword(String password) {
        String passwordHash = hashPassword(password.trim());
        return setConfigValue("login_password", passwordHash, "面板登录密码");
    }
    
    // MD5哈希
    private String hashPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }
}
```

### 2. 前端实现

#### 设置页面集成

在设置页面添加了密码设置卡片：

```vue
<!-- 密码设置卡片 -->
<div class="settings-card">
  <div class="card-header">
    <div class="header-content">
      <Icon icon="mdi:lock" class="header-icon" />
      <div>
        <h2 class="card-title">登录密码</h2>
        <p class="card-description">设置面板登录密码</p>
      </div>
    </div>
    <div class="header-actions">
      <button class="change-password-btn" @click="showPasswordForm = true">
        修改密码
      </button>
    </div>
  </div>
  
  <div class="card-content">
    <!-- 密码修改表单 -->
  </div>
</div>
```

#### 密码修改逻辑

```typescript
const changePassword = async () => {
  const response = await fetch(`${API_BASE_URL}/auth/change-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include', // 包含会话信息
    body: JSON.stringify({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword
    })
  })
  
  const result = await response.json()
  if (result.success) {
    // 密码修改成功
  }
}
```

## 界面设计

### 1. 密码设置卡片

```
┌─────────────────────────────────────┐
│ 🔒 登录密码              [修改密码]  │
│    设置面板登录密码                  │
├─────────────────────────────────────┤
│ ℹ️ 当前状态                         │
│    密码保护已启用，默认密码为 "admin" │
└─────────────────────────────────────┘
```

### 2. 密码修改表单

```
┌─────────────────────────────────────┐
│ 当前密码                             │
│ [输入当前密码........................] │
│                                     │
│ 新密码                               │
│ [输入新密码（至少4位）...............] │
│                                     │
│ 确认新密码                           │
│ [再次输入新密码.....................] │
│                                     │
│                        [取消] [保存] │
└─────────────────────────────────────┘
```

## 使用流程

### 1. 首次使用

1. **访问面板**：用户首次访问导航面板
2. **输入密码**：系统提示输入登录密码
3. **使用默认密码**：输入默认密码 "admin"
4. **成功登录**：进入导航面板主界面

### 2. 修改密码

1. **进入设置**：点击导航中的"设置"
2. **找到密码设置**：在设置页面找到"登录密码"卡片
3. **点击修改**：点击"修改密码"按钮
4. **填写表单**：输入当前密码和新密码
5. **保存设置**：点击"保存"完成密码修改

### 3. 后续登录

1. **访问面板**：再次访问导航面板
2. **输入新密码**：使用修改后的密码登录
3. **保持登录**：会话期间保持登录状态

## 安全考虑

### 1. 密码存储

- **哈希处理**：密码使用MD5哈希存储，不保存明文
- **数据库安全**：配置信息存储在专门的配置表中
- **默认密码**：提供默认密码便于首次使用

### 2. 会话管理

- **HTTP Session**：使用标准的HTTP Session管理登录状态
- **自动过期**：会话超时后自动要求重新登录
- **安全登出**：提供登出功能清除会话

### 3. API保护

- **认证检查**：敏感API需要检查登录状态
- **错误处理**：登录失败时提供适当的错误信息
- **防暴力破解**：可以扩展添加登录尝试限制

## 扩展功能

### 1. 可能的改进

- **更强密码算法**：使用BCrypt等更安全的哈希算法
- **密码强度检查**：添加密码复杂度要求
- **登录尝试限制**：防止暴力破解攻击
- **双因素认证**：添加2FA支持

### 2. 用户体验优化

- **记住登录状态**：可选的"记住我"功能
- **密码找回**：管理员密码重置功能
- **登录日志**：记录登录历史和异常

### 3. 企业级功能

- **LDAP集成**：企业用户目录集成
- **SSO支持**：单点登录集成
- **权限管理**：多用户和角色管理

## 配置说明

### 1. 默认设置

- **默认密码**：admin
- **会话超时**：30分钟（可配置）
- **密码最小长度**：4位

### 2. 环境变量

可以通过环境变量配置：
- `DEFAULT_ADMIN_PASSWORD`：默认管理员密码
- `SESSION_TIMEOUT`：会话超时时间
- `PASSWORD_MIN_LENGTH`：密码最小长度

### 3. 数据库配置

系统配置存储在 `system_config` 表中：
- `login_password`：登录密码哈希值
- 其他配置项可以扩展添加

现在面板具备了基本的安全保护功能！🔒
