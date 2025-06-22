<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import { Icon } from '@iconify/vue'

definePageMeta({
  layout: 'dashboard'
})

// 类型定义
interface Category {
  id: number
  name: string
  order: number
  createdAt: string
  updatedAt: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 响应式数据
const categories = ref<Category[]>([])
const loading = ref(false)
const saving = ref(false)

// API配置
const config = useRuntimeConfig()
const API_BASE_URL = `${config.public.apiBaseUrl}/api`

// 获取分组列表
const fetchCategories = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/categories`)
    const result: ApiResponse<Category[]> = await response.json()

    if (result.success) {
      categories.value = result.data
    } else {
      console.error('获取分组失败:', result.message)
    }
  } catch (error) {
    console.error('获取分组失败:', error)
  } finally {
    loading.value = false
  }
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
      // 重新获取数据以恢复原始顺序
      await fetchCategories()
    }
  } catch (error) {
    console.error('保存排序失败:', error)
    // 重新获取数据以恢复原始顺序
    await fetchCategories()
  } finally {
    saving.value = false
  }
}

// 处理拖拽结束
const handleDragEnd = () => {
  console.log('拖拽排序完成')
  saveCategoriesSort()
}

// 页面加载时获取数据
onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <NuxtLayout>
    <div class="settings-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">系统设置</h1>
        <p class="page-description">管理导航分组和系统配置</p>
      </div>

      <!-- 分组设置卡片 -->
      <div class="settings-card">
        <div class="card-header">
          <div class="header-content">
            <Icon icon="mdi:folder-multiple" class="header-icon" />
            <div>
              <h2 class="card-title">分组管理</h2>
              <p class="card-description">拖拽调整分组显示顺序</p>
            </div>
          </div>
          <div class="header-actions">
            <button
              v-if="saving"
              class="save-button saving"
              disabled
            >
              <Icon icon="mdi:loading" class="spin" />
              保存中...
            </button>
          </div>
        </div>

        <div class="card-content">
          <div v-if="loading" class="loading-state">
            <Icon icon="mdi:loading" class="loading-icon spin" />
            <p>加载分组数据中...</p>
          </div>

          <div v-else-if="categories.length === 0" class="empty-state">
            <Icon icon="mdi:folder-off" class="empty-icon" />
            <p>暂无分组数据</p>
          </div>

          <VueDraggable
            v-else
            v-model="categories"
            class="categories-list"
            :animation="200"
            ghost-class="ghost-item"
            chosen-class="chosen-item"
            drag-class="drag-item"
            @end="handleDragEnd"
          >
            <div
              v-for="(category, index) in categories"
              :key="category.id"
              class="category-item"
            >
              <div class="drag-handle">
                <Icon icon="mdi:drag-vertical" class="drag-icon" />
              </div>

              <div class="category-info">
                <div class="category-name">{{ category.name }}</div>
                <div class="category-meta">
                  排序: {{ index + 1 }} | ID: {{ category.id }}
                </div>
              </div>

              <div class="category-actions">
                <span class="order-badge">{{ index + 1 }}</span>
              </div>
            </div>
          </VueDraggable>
        </div>
      </div>

      <!-- 其他设置卡片可以在这里添加 -->
      <div class="settings-card">
        <div class="card-header">
          <div class="header-content">
            <Icon icon="mdi:cog" class="header-icon" />
            <div>
              <h2 class="card-title">系统配置</h2>
              <p class="card-description">其他系统设置选项</p>
            </div>
          </div>
        </div>

        <div class="card-content">
          <div class="coming-soon">
            <Icon icon="mdi:wrench" class="coming-soon-icon" />
            <p>更多设置功能即将推出...</p>
          </div>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>

<style scoped>
.settings-container {
  min-height: 100vh;
  padding: 2rem;
  max-width: 100%;
}

/* 页面标题 */
.page-header {
  margin-bottom: 2rem;
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}

.page-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.5rem 0;
  text-align: left;
}

.page-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  text-align: left;
}

/* 设置卡片 */
.settings-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 1rem;
  margin-bottom: 3rem;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-icon {
  width: 2rem;
  height: 2rem;
  color: rgba(59, 130, 246, 0.8);
}

.card-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
}

.card-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.save-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.save-button.saving {
  opacity: 0.7;
  cursor: not-allowed;
}

.card-content {
  padding: 1.5rem;
}

/* 加载和空状态 */
.loading-state,
.empty-state,
.coming-soon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.loading-icon,
.empty-icon,
.coming-soon-icon {
  width: 3rem;
  height: 3rem;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 1rem;
}

.loading-state p,
.empty-state p,
.coming-soon p {
  color: rgba(255, 255, 255, 0.6);
  font-size: 1rem;
  margin: 0;
}

/* 分组列表 */
.categories-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
  background: transparent;
  border: 2px solid transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: grab;
  position: relative;
  overflow: hidden;
}

.category-item:hover {
  transform: translateY(-2px) scale(1.02);
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  box-shadow:
    0 8px 25px rgba(0, 0, 0, 0.15),
    0 4px 10px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.category-item:active {
  cursor: grabbing;
}

.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.2) 0%,
    rgba(168, 85, 247, 0.1) 100%
  );
  border: 1px solid rgba(168, 85, 247, 0.3);
  cursor: grab;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  flex-shrink: 0;
}

.drag-handle:hover {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.3) 0%,
    rgba(168, 85, 247, 0.15) 100%
  );
  transform: translateY(-1px) scale(1.05);
  box-shadow: 0 4px 12px rgba(168, 85, 247, 0.3);
}

.drag-handle:active {
  cursor: grabbing;
}

.drag-icon {
  width: 1rem;
  height: 1rem;
  color: rgba(168, 85, 247, 0.8);
}

.category-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.category-name {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
  transition: all 0.3s ease;
}

.category-meta {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  text-align: left;
}

.category-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.order-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.75rem;
  font-weight: 600;
}

/* 拖拽状态样式 */
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

/* 动画 */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .settings-container {
    padding: 1rem;
  }

  .page-title {
    font-size: 1.5rem;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .header-content {
    width: 100%;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .category-item {
    padding: 0.75rem;
    gap: 0.75rem;
  }

  .category-name {
    font-size: 0.875rem;
  }

  .category-meta {
    font-size: 0.7rem;
  }
}
</style>