# 文件上传功能测试指南

## 修改总结

已成功修改NavigationItemDialog.vue组件以支持MultipartFile文件上传：

### 前端修改
1. **NavigationItemDialog.vue**:
   - 添加了文件选择和预览功能
   - 修改了handleConfirm方法支持FormData
   - 添加了文件类型和大小验证
   - 支持三种图标模式：上传图片、在线图标、内置图标

2. **navigation-panel.vue**:
   - 添加了新的API方法`updateNavigationItemWithUpload`
   - 修改了`handleDialogConfirm`方法支持新的数据格式
   - 支持文件上传和传统模式的切换

### 后端修改
1. **application.yml**: 添加了文件上传配置
2. **FileUploadService**: 创建了文件上传服务
3. **NavigationItemController**: 添加了文件上传接口
4. **FileUploadConfig**: 配置了静态资源映射

## 测试步骤

### 1. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

### 2. 启动前端服务
```bash
cd frontend
npm run dev
```

### 3. 测试新增功能
1. 打开导航面板页面
2. 点击任意分类的"+"按钮
3. 在弹出的对话框中：
   - 输入导航项名称
   - 输入URL地址
   - 选择"上传图片"选项卡
   - 点击"选择图片文件"上传图标
   - 点击"新增"按钮

### 4. 测试编辑功能
1. 右键点击任意导航项
2. 选择"编辑"
3. 可以选择上传新的图标文件或保持原有图标
4. 点击"保存"按钮

### 5. 验证文件存储
- 检查`backend/uploads/`目录是否有上传的文件
- 检查数据库中logo字段是否存储了正确的路径（如：`/uploads/20241222_143025_a1b2c3d4.png`）
- 验证图片是否能正常显示

## API接口

### 新增导航项（文件上传）
- **URL**: `POST /api/navigation-items/upload`
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `logoFile`: 图标文件
  - `name`: 名称
  - `url`: URL地址
  - `categoryId`: 分类ID
  - `description`: 描述（可选）
  - `internalUrl`: 内网地址（可选）

### 更新导航项（文件上传）
- **URL**: `PUT /api/navigation-items/{id}/upload`
- **Content-Type**: `multipart/form-data`
- **参数**: 同上，logoFile为可选

## 注意事项

1. **文件类型限制**: 支持 jpg, jpeg, png, gif, bmp, webp, svg
2. **文件大小限制**: 最大10MB
3. **存储路径**: 文件存储在`backend/uploads/`目录
4. **访问路径**: 通过`/uploads/文件名`访问
5. **兼容性**: 保留了原有的JSON接口用于兼容性

## 可能的问题

1. **文件上传失败**: 检查文件类型和大小是否符合要求
2. **图片不显示**: 检查静态资源映射配置是否正确
3. **路径错误**: 确保前端API_BASE_URL配置正确

## 下一步优化

1. 添加图片压缩功能
2. 支持拖拽上传
3. 添加上传进度显示
4. 支持批量上传
