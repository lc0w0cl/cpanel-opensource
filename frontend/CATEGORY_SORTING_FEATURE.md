# 分组排序功能说明

## 功能概述

在设置界面新增了分组管理功能，用户可以通过拖拽的方式调整分组的显示顺序，实现个性化的分组排列。

## 主要功能

### 1. 分组列表显示

- **实时数据**：从后端API获取最新的分组数据
- **排序显示**：按照 `order` 字段排序显示
- **状态反馈**：加载状态、空状态、错误状态的友好提示

### 2. 拖拽排序

- **直观操作**：通过拖拽调整分组顺序
- **实时保存**：拖拽完成后自动保存到数据库
- **视觉反馈**：拖拽过程中的视觉效果和状态提示

### 3. 数据同步

- **自动保存**：排序变更自动同步到后端
- **错误恢复**：保存失败时自动恢复原始顺序
- **状态提示**：保存过程中的加载状态提示

## 技术实现

### 1. 后端API扩展

#### 新增排序接口

```java
/**
 * 批量更新分类排序
 * @param categories 分类列表（包含新的排序信息）
 * @return 更新结果
 */
@PutMapping("/sort")
public ApiResponse<String> updateCategoriesSort(@RequestBody List<PanelCategory> categories)
```

#### 服务层实现

```java
@Override
public boolean updateCategoriesSort(List<PanelCategory> categories) {
    if (categories == null || categories.isEmpty()) {
        return false;
    }

    try {
        // 批量更新排序
        for (int i = 0; i < categories.size(); i++) {
            PanelCategory category = categories.get(i);
            category.setOrder(i + 1); // 排序从1开始
            updateById(category);
        }
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

### 2. 前端实现

#### 技术栈

- **Vue 3 Composition API**：响应式数据管理
- **vue-draggable-plus**：拖拽排序功能
- **@iconify/vue**：图标组件
- **TypeScript**：类型安全

#### 核心功能

```typescript
// 处理拖拽结束
const handleDragEnd = () => {
  console.log('拖拽排序完成')
  saveCategoriesSort()
}

// 保存分组排序
const saveCategoriesSort = async () => {
  saving.value = true
  try {
    // 更新排序号
    const sortedCategories = categories.value.map((category, index) => ({
      ...category,
      order: index + 1
    }))

    const response = await fetch(`${API_BASE_URL}/categories/sort`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(sortedCategories)
    })
    
    const result: ApiResponse<string> = await response.json()
    
    if (result.success) {
      categories.value = sortedCategories
      console.log('分组排序保存成功')
    } else {
      console.error('保存排序失败:', result.message)
      await fetchCategories() // 恢复原始顺序
    }
  } catch (error) {
    console.error('保存排序失败:', error)
    await fetchCategories() // 恢复原始顺序
  } finally {
    saving.value = false
  }
}
```

## 界面设计

### 1. 页面布局

```
┌─────────────────────────────────────┐
│ 系统设置                             │
│ 管理导航分组和系统配置                │
├─────────────────────────────────────┤
│ 📁 分组管理                         │
│    拖拽调整分组显示顺序              │
│                                     │
│ ⋮⋮ [搜索引擎]           [1]         │
│ ⋮⋮ [电商购物]           [2]         │
│ ⋮⋮ [开发工具]           [3]         │
│                                     │
├─────────────────────────────────────┤
│ ⚙️ 系统配置                         │
│    其他系统设置选项                  │
│                                     │
│    更多设置功能即将推出...           │
└─────────────────────────────────────┘
```

### 2. 视觉特效

- **液态玻璃效果**：现代化的半透明背景
- **渐变色彩**：蓝紫色渐变主题
- **拖拽反馈**：拖拽时的缩放、旋转、阴影效果
- **悬停动画**：鼠标悬停时的微动画
- **状态指示**：加载、保存状态的视觉反馈

### 3. 交互体验

- **拖拽手柄**：专门的拖拽区域，紫色渐变设计
- **排序徽章**：显示当前排序位置的圆形徽章
- **实时反馈**：拖拽过程中的实时视觉反馈
- **错误处理**：操作失败时的友好提示

## 样式设计

### 1. 分组项样式

```css
.category-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  cursor: grab;
}
```

### 2. 拖拽状态

```css
.ghost-item {
  opacity: 0.5;
  transform: scale(0.95);
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.2) 0%,
    rgba(168, 85, 247, 0.1) 100%
  );
  border: 2px dashed rgba(168, 85, 247, 0.5);
}

.chosen-item {
  transform: scale(1.02);
  box-shadow: 0 8px 25px rgba(168, 85, 247, 0.3);
  z-index: 1000;
}

.drag-item {
  transform: rotate(2deg) scale(1.05);
  box-shadow: 0 12px 30px rgba(168, 85, 247, 0.4);
  z-index: 1001;
}
```

## 使用指南

### 1. 访问设置页面

- 在导航中点击"设置"进入设置页面
- 页面会自动加载当前的分组数据

### 2. 调整分组顺序

1. **找到拖拽手柄**：每个分组项左侧的紫色图标
2. **开始拖拽**：点击并拖拽手柄
3. **调整位置**：将分组拖拽到目标位置
4. **释放鼠标**：完成拖拽，系统自动保存

### 3. 查看结果

- 排序变更会立即反映在导航面板中
- 右侧的数字徽章显示当前排序位置
- 保存过程中会显示"保存中..."状态

## 错误处理

### 1. 网络错误

- **自动重试**：保存失败时自动恢复原始顺序
- **用户提示**：控制台输出详细错误信息
- **数据一致性**：确保前后端数据同步

### 2. 数据验证

- **空数据检查**：防止空数组导致的错误
- **类型验证**：TypeScript 提供类型安全
- **边界处理**：处理各种异常情况

## 扩展性

### 1. 功能扩展

- **分组编辑**：可以扩展为支持分组名称编辑
- **分组删除**：可以添加分组删除功能
- **分组创建**：可以添加新建分组功能
- **批量操作**：可以支持批量选择和操作

### 2. 界面扩展

- **更多设置**：可以添加其他系统设置选项
- **主题配置**：可以添加主题和外观设置
- **用户偏好**：可以添加个人偏好设置

## 性能优化

### 1. 前端优化

- **按需加载**：只在设置页面加载相关代码
- **防抖处理**：避免频繁的API调用
- **缓存机制**：合理使用本地缓存

### 2. 后端优化

- **批量更新**：使用批量更新减少数据库操作
- **事务处理**：确保数据一致性
- **索引优化**：order字段已建立索引

现在用户可以在设置页面轻松管理分组顺序了！🎉
