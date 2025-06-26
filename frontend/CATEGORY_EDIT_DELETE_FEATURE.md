# 分组编辑和删除功能说明

## 功能概述

在设置页面的分组管理中新增了编辑分组名称和删除分组的功能，提供完整的分组管理体验。

## 主要功能

### 1. 编辑分组名称

#### 功能特性
- **内联编辑**：直接在分组列表中编辑，无需弹窗
- **实时验证**：输入验证和状态反馈
- **快捷操作**：支持回车保存、ESC取消
- **加载状态**：编辑过程中的加载提示

#### 操作流程
1. 点击分组项右侧的编辑按钮（铅笔图标）
2. 分组名称变为可编辑的输入框
3. 修改名称后点击保存按钮或按回车键
4. 点击取消按钮或按ESC键可取消编辑

### 2. 删除分组

#### 功能特性
- **确认对话框**：防止误删操作
- **警告提示**：明确告知删除后果
- **安全删除**：删除分组及其下所有导航项
- **状态反馈**：删除过程中的状态提示

#### 操作流程
1. 点击分组项右侧的删除按钮（垃圾桶图标）
2. 弹出确认对话框，显示分组名称和警告信息
3. 点击"确认删除"按钮执行删除
4. 点击"取消"按钮或点击对话框外部取消删除

## 技术实现

### 1. 前端实现

#### 新增响应式数据
```typescript
// 编辑分组相关
const editingCategoryId = ref<number | null>(null)
const editCategoryForm = ref({
  name: ''
})
const editCategoryLoading = ref(false)

// 删除分组相关
const showDeleteConfirm = ref(false)
const deletingCategory = ref<Category | null>(null)
const deleteCategoryLoading = ref(false)
```

#### 编辑功能方法
```typescript
// 开始编辑分组
const startEditCategory = (category: Category) => {
  editingCategoryId.value = category.id
  editCategoryForm.value.name = category.name
}

// 保存编辑分组
const saveEditCategory = async () => {
  // API调用和状态管理
}

// 取消编辑分组
const cancelEditCategory = () => {
  editingCategoryId.value = null
  editCategoryForm.value.name = ''
}
```

#### 删除功能方法
```typescript
// 显示删除确认对话框
const showDeleteCategoryConfirm = (category: Category) => {
  deletingCategory.value = category
  showDeleteConfirm.value = true
}

// 确认删除分组
const confirmDeleteCategory = async () => {
  // API调用和状态管理
}

// 取消删除分组
const cancelDeleteCategory = () => {
  deletingCategory.value = null
  showDeleteConfirm.value = false
}
```

### 2. 后端API

#### 更新分组接口
- **路径**：`PUT /api/categories/{id}`
- **参数**：分组ID和新的分组信息
- **返回**：更新结果

#### 删除分组接口
- **路径**：`DELETE /api/categories/{id}`
- **参数**：分组ID
- **返回**：删除结果

## UI设计

### 1. 编辑界面
- **内联编辑**：输入框直接替换分组名称显示
- **操作按钮**：保存（绿色勾号）和取消（红色叉号）
- **状态指示**：加载时显示旋转图标
- **键盘支持**：回车保存、ESC取消

### 2. 删除确认对话框
- **模态设计**：半透明背景，居中显示
- **警告图标**：黄色警告图标突出显示
- **信息展示**：分组名称和删除警告
- **操作按钮**：取消（灰色）和确认删除（红色）

### 3. 操作按钮
- **编辑按钮**：黄色渐变，铅笔图标
- **删除按钮**：红色渐变，垃圾桶图标
- **悬停效果**：按钮放大和颜色加深
- **禁用状态**：半透明显示

## 样式特性

### 1. 液态玻璃效果
- **背景**：半透明渐变背景
- **模糊**：backdrop-filter 模糊效果
- **边框**：细腻的半透明边框
- **阴影**：多层阴影营造深度感

### 2. 动画效果
- **对话框**：淡入和滑入动画
- **按钮**：悬停时的缩放和位移
- **状态切换**：平滑的过渡动画

### 3. 响应式设计
- **移动端适配**：按钮大小和间距调整
- **触摸友好**：足够的点击区域
- **屏幕适配**：不同尺寸下的布局调整

## 用户体验优化

### 1. 操作反馈
- **即时反馈**：操作状态的实时提示
- **错误处理**：友好的错误信息显示
- **成功提示**：操作成功后的确认

### 2. 安全机制
- **确认对话框**：防止误删重要数据
- **警告信息**：明确告知操作后果
- **可撤销性**：编辑过程可随时取消

### 3. 便捷操作
- **快捷键**：键盘快捷键支持
- **内联编辑**：减少页面跳转
- **批量操作**：为未来扩展预留接口

## 注意事项

1. **数据一致性**：编辑和删除后自动刷新列表
2. **状态管理**：正确处理加载和错误状态
3. **权限控制**：确保用户有相应操作权限
4. **数据验证**：前后端双重验证输入数据
5. **错误恢复**：操作失败时的数据恢复机制

## 未来扩展

1. **批量操作**：支持批量编辑和删除
2. **拖拽编辑**：拖拽时直接编辑名称
3. **历史记录**：操作历史和撤销功能
4. **权限管理**：基于角色的操作权限控制
