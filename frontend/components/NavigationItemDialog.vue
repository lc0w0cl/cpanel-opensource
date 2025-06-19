<script setup lang="ts">
import { ref, watch } from 'vue'

// 定义组件的 props
interface Props {
  visible: boolean
  mode: 'add' | 'edit'
  categoryId?: string
  item?: {
    id: string
    name: string
    url: string
    logo: string
    categoryId: string
  }
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  mode: 'add',
  categoryId: '',
  item: undefined
})

// 定义组件的 emits
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'confirm': [data: any]
}>()

// 表单数据
const formData = ref({
  name: '',
  url: '',
  logo: '',
  categoryId: ''
})

// 监听 props 变化，初始化表单数据
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    if (props.mode === 'edit' && props.item) {
      // 编辑模式，填充现有数据
      formData.value = {
        name: props.item.name,
        url: props.item.url,
        logo: props.item.logo,
        categoryId: props.item.categoryId
      }
    } else {
      // 新增模式，重置表单
      formData.value = {
        name: '',
        url: '',
        logo: '',
        categoryId: props.categoryId || ''
      }
    }
  }
})

// 关闭弹窗
const closeDialog = () => {
  emit('update:visible', false)
}

// 确认操作
const handleConfirm = () => {
  // 这里暂时只是关闭弹窗，后续可以添加表单验证和数据处理
  console.log('表单数据:', formData.value)
  emit('confirm', formData.value)
  closeDialog()
}

// 处理遮罩层点击
const handleOverlayClick = (event: MouseEvent) => {
  if (event.target === event.currentTarget) {
    closeDialog()
  }
}
</script>

<template>
  <div
    v-if="visible"
    class="dialog-overlay"
    @click="handleOverlayClick"
  >
    <div class="dialog-container" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">
          {{ mode === 'add' ? '新增导航项' : '编辑导航项' }}
        </h3>
        <button class="close-button" @click="closeDialog">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
      
      <div class="dialog-content">
        <div class="form-group">
          <label class="form-label">名称</label>
          <input
            v-model="formData.name"
            type="text"
            class="form-input"
            placeholder="请输入导航项名称"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">链接</label>
          <input
            v-model="formData.url"
            type="url"
            class="form-input"
            placeholder="请输入链接地址"
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">图标</label>
          <input
            v-model="formData.logo"
            type="text"
            class="form-input"
            placeholder="请输入图标路径或URL"
          />
        </div>
      </div>
      
      <div class="dialog-actions">
        <button class="btn-cancel" @click="closeDialog">
          取消
        </button>
        <button class="btn-confirm" @click="handleConfirm">
          {{ mode === 'add' ? '新增' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: overlayFadeIn 0.3s ease;
}

.dialog-container {
  min-width: 500px;
  max-width: 600px;
  width: 90vw;
  border-radius: 1rem;
  overflow: hidden;

  /* 液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.2);

  /* 阴影效果 */
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.3),
    0 8px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);

  animation: dialogSlideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem 1.5rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.dialog-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.close-button {
  width: 2rem;
  height: 2rem;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

.close-button svg {
  width: 1.25rem;
  height: 1.25rem;
  stroke-width: 2;
}

.dialog-content {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 0.5rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.form-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.dialog-actions {
  display: flex;
  gap: 0.75rem;
  padding: 0 1.5rem 1.5rem;
  justify-content: flex-end;
}

.btn-cancel,
.btn-confirm {
  padding: 0.75rem 1.5rem;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 0.875rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.btn-cancel {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-cancel:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.btn-confirm {
  background: linear-gradient(135deg,
    #3b82f6 0%,
    #2563eb 100%
  );
  color: white;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.btn-confirm:hover {
  background: linear-gradient(135deg,
    #2563eb 0%,
    #1d4ed8 100%
  );
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* 动画 */
@keyframes overlayFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dialog-container {
    min-width: 320px;
    max-width: 95vw;
    margin: 1rem;
  }

  .dialog-actions {
    flex-direction: column-reverse;
  }

  .btn-cancel,
  .btn-confirm {
    width: 100%;
    justify-content: center;
  }
}
</style>
