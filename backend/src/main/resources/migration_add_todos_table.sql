-- 数据库迁移脚本：添加TODO任务表
-- 执行时间：2024年

-- 创建TODO任务表（如果不存在）
CREATE TABLE IF NOT EXISTS panel_todos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID，自增主键',
  text VARCHAR(500) NOT NULL COMMENT '任务内容',
  completed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已完成',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
  INDEX idx_sort_order (sort_order),
  INDEX idx_completed (completed),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TODO任务表';

-- 验证表创建结果
SELECT 'TODO任务表创建完成' as message;

-- 显示表结构
DESCRIBE panel_todos;
