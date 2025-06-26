# 获取图标功能Token认证修复

## 问题描述

在实现"获取图标"功能时，前端请求没有携带认证token，导致后端的认证拦截器拦截了请求，返回401未授权错误。

## 问题原因

原始代码使用了原生的`fetch`方法发送请求，没有自动添加认证头：

```typescript
// 问题代码
const response = await fetch(`${API_BASE_URL}/navigation-items/fetch-icon?url=${encodeURIComponent(formData.value.url)}`)
```

这种方式不会自动添加`Authorization: Bearer <token>`头部，导致后端认证失败。

## 解决方案

### 1. 导入apiRequest方法

在`NavigationItemDialog.vue`中导入带有自动token管理的`apiRequest`方法：

```typescript
import { apiRequest } from '~/composables/useJwt'
```

### 2. 替换请求方法

将原生`fetch`替换为`apiRequest`：

```typescript
// 修复后的代码
const response = await apiRequest(`${API_BASE_URL}/navigation-items/fetch-icon?url=${encodeURIComponent(formData.value.url)}`)
```

## apiRequest方法的优势

### 1. 自动Token管理
- **自动添加认证头**：每次请求自动添加`Authorization: Bearer <token>`
- **Token刷新**：当token即将过期时自动刷新
- **401处理**：收到401响应时自动尝试刷新token并重试

### 2. 统一错误处理
- **认证失败处理**：token刷新失败时自动清除本地token并跳转登录页
- **网络错误处理**：统一的网络错误处理机制
- **重试机制**：401错误时的自动重试

### 3. FormData支持
- **智能检测**：自动检测是否为FormData请求
- **头部处理**：FormData请求时不设置Content-Type，让浏览器自动设置
- **兼容性**：与现有的文件上传功能完全兼容

## 修复验证

### 1. 请求头验证
修复后的请求会自动包含以下头部：
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

### 2. 功能测试
- ✅ 用户登录后可以正常使用获取图标功能
- ✅ Token过期时会自动刷新并重试
- ✅ 认证失败时会正确处理并跳转登录页

### 3. 错误处理
- ✅ 网络错误时显示友好提示
- ✅ 服务器错误时显示具体错误信息
- ✅ 认证错误时自动处理token刷新

## 代码对比

### 修复前
```typescript
// 获取网站图标
const fetchWebsiteIcon = async () => {
  // ... 验证逻辑 ...
  
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`
    
    // ❌ 使用原生fetch，没有token
    const response = await fetch(`${API_BASE_URL}/navigation-items/fetch-icon?url=${encodeURIComponent(formData.value.url)}`)
    const result = await response.json()
    
    // ... 处理结果 ...
  } catch (error) {
    // ... 错误处理 ...
  }
}
```

### 修复后
```typescript
// 获取网站图标
const fetchWebsiteIcon = async () => {
  // ... 验证逻辑 ...
  
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`
    
    // ✅ 使用apiRequest，自动添加token
    const response = await apiRequest(`${API_BASE_URL}/navigation-items/fetch-icon?url=${encodeURIComponent(formData.value.url)}`)
    const result = await response.json()
    
    // ... 处理结果 ...
  } catch (error) {
    // ... 错误处理 ...
  }
}
```

## 其他API调用的一致性

项目中其他API调用都使用了`apiRequest`方法，确保了认证处理的一致性：

### 分类管理
```typescript
const response = await apiRequest(`${API_BASE_URL}/categories`)
```

### 导航项管理
```typescript
const response = await apiRequest(`${API_BASE_URL}/navigation-items`)
```

### 系统信息
```typescript
const response = await apiRequest(`${API_BASE_URL}/system/info`)
```

### 认证相关
```typescript
const response = await apiRequest(`${API_BASE_URL}/auth/status`)
```

## 安全性提升

### 1. 统一认证
- 所有API请求都通过统一的认证机制
- 防止遗漏认证头的情况
- 确保安全性的一致性

### 2. Token生命周期管理
- 自动检测token过期时间
- 主动刷新即将过期的token
- 失效token的自动清理

### 3. 错误恢复
- 认证失败时的自动恢复机制
- 用户无感知的token刷新
- 失败时的优雅降级

## 总结

通过将原生`fetch`替换为项目统一的`apiRequest`方法，成功修复了获取图标功能的认证问题。这个修复不仅解决了当前的问题，还确保了与项目其他API调用的一致性，提升了整体的安全性和用户体验。

### 修复要点
1. ✅ 导入`apiRequest`方法
2. ✅ 替换原生`fetch`调用
3. ✅ 保持原有的错误处理逻辑
4. ✅ 确保与其他API调用的一致性

现在用户可以正常使用获取图标功能，无需担心认证问题！
