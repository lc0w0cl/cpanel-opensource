package com.clover.cpanel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clover.cpanel.entity.Todo;
import com.clover.cpanel.mapper.TodoMapper;
import com.clover.cpanel.service.TodoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TODO任务服务实现类
 */
@Slf4j
@Service
public class TodoServiceImpl extends ServiceImpl<TodoMapper, Todo> implements TodoService {

    @Override
    public List<Todo> getAllTodosOrdered() {
        return baseMapper.getAllTodosOrdered();
    }

    @Override
    public List<Todo> getTodosByCategoryId(Integer categoryId) {
        return baseMapper.getTodosByCategoryId(categoryId);
    }

    @Override
    public Todo createTodo(String text) {
        return createTodo(text, null);
    }

    @Override
    public Todo createTodo(String text, Integer categoryId) {
        try {
            // 获取下一个排序序号（按分组）
            Integer maxSortOrder;
            if (categoryId != null) {
                maxSortOrder = baseMapper.getMaxSortOrderByCategory(categoryId);
            } else {
                maxSortOrder = baseMapper.getMaxSortOrder();
            }
            int nextSortOrder = (maxSortOrder != null ? maxSortOrder : 0) + 1;

            Todo todo = new Todo();
            todo.setText(text);
            todo.setCompleted(false);
            todo.setCategoryId(categoryId);
            todo.setSortOrder(nextSortOrder);

            boolean success = save(todo);
            if (success) {
                log.info("创建TODO任务成功: text={}, categoryId={}", text, categoryId);
                return todo;
            } else {
                log.error("创建TODO任务失败: text={}, categoryId={}", text, categoryId);
                return null;
            }
        } catch (Exception e) {
            log.error("创建TODO任务异常: text={}, categoryId={}", text, categoryId, e);
            return null;
        }
    }

    @Override
    public boolean updateTodoText(Long id, String text) {
        try {
            UpdateWrapper<Todo> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", id)
                    .set("text", text);

            boolean success = update(updateWrapper);
            if (success) {
                log.info("更新TODO任务内容成功: id={}, text={}", id, text);
            } else {
                log.error("更新TODO任务内容失败: id={}, text={}", id, text);
            }
            return success;
        } catch (Exception e) {
            log.error("更新TODO任务内容异常: id={}, text={}", id, text, e);
            return false;
        }
    }

    @Override
    public boolean toggleTodoCompleted(Long id) {
        try {
            Todo todo = getById(id);
            if (todo == null) {
                log.error("TODO任务不存在: id={}", id);
                return false;
            }

            todo.setCompleted(!todo.getCompleted());
            boolean success = updateById(todo);
            if (success) {
                log.info("切换TODO任务完成状态成功: id={}, completed={}", id, todo.getCompleted());
            } else {
                log.error("切换TODO任务完成状态失败: id={}", id);
            }
            return success;
        } catch (Exception e) {
            log.error("切换TODO任务完成状态异常: id={}", id, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateTodosSortOrder(List<Long> todoIds) {
        try {
            for (int i = 0; i < todoIds.size(); i++) {
                Long todoId = todoIds.get(i);
                UpdateWrapper<Todo> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", todoId)
                        .set("sort_order", i + 1);

                boolean success = update(updateWrapper);
                if (!success) {
                    log.error("更新TODO任务排序失败: id={}, sortOrder={}", todoId, i + 1);
                    return false;
                }
            }
            log.info("批量更新TODO任务排序成功: {}", todoIds);
            return true;
        } catch (Exception e) {
            log.error("批量更新TODO任务排序异常: {}", todoIds, e);
            return false;
        }
    }

    @Override
    public boolean setAllTodosCompleted(boolean completed) {
        try {
            UpdateWrapper<Todo> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("completed", completed);

            boolean success = update(updateWrapper);
            if (success) {
                log.info("批量设置TODO任务完成状态成功: completed={}", completed);
            } else {
                log.error("批量设置TODO任务完成状态失败: completed={}", completed);
            }
            return success;
        } catch (Exception e) {
            log.error("批量设置TODO任务完成状态异常: completed={}", completed, e);
            return false;
        }
    }

    @Override
    public int deleteCompletedTodos() {
        try {
            QueryWrapper<Todo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("completed", true);

            int count = (int)count(queryWrapper);
            boolean success = remove(queryWrapper);
            if (success) {
                log.info("删除已完成TODO任务成功: 删除数量={}", count);
                return count;
            } else {
                log.error("删除已完成TODO任务失败");
                return 0;
            }
        } catch (Exception e) {
            log.error("删除已完成TODO任务异常", e);
            return 0;
        }
    }
}
