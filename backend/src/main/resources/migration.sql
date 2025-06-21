-- 数据库迁移脚本：为现有的 panel_navigation_items 表添加排序字段

-- 1. 添加 sort_order 字段（如果不存在）
ALTER TABLE panel_navigation_items 
ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0 COMMENT '排序序号';

-- 2. 添加排序字段的索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_sort_order ON panel_navigation_items(sort_order);

-- 3. 为现有数据设置排序号（按ID顺序）
UPDATE panel_navigation_items 
SET sort_order = (
    SELECT COUNT(*) 
    FROM (SELECT id FROM panel_navigation_items p2 WHERE p2.category_id = panel_navigation_items.category_id AND p2.id <= panel_navigation_items.id) AS temp
)
WHERE sort_order = 0 OR sort_order IS NULL;

-- 4. 验证数据
SELECT category_id, id, name, sort_order 
FROM panel_navigation_items 
ORDER BY category_id, sort_order;
