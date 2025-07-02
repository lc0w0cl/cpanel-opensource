package com.clover.cpanel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clover.cpanel.entity.Todo;

import java.util.List;

/**
 * TODO任务服务接口
 */
public interface TodoService extends IService<Todo> {

    /**
     * 获取所有任务，按排序序号和创建时间排序
     * @return 任务列表
     */
    List<Todo> getAllTodosOrdered();

    /**
     * 创建新任务
     * @param text 任务内容
     * @return 创建的任务
     */
    Todo createTodo(String text);

    /**
     * 更新任务内容
     * @param id 任务ID
     * @param text 新的任务内容
     * @return 是否更新成功
     */
    boolean updateTodoText(Long id, String text);

    /**
     * 切换任务完成状态
     * @param id 任务ID
     * @return 是否更新成功
     */
    boolean toggleTodoCompleted(Long id);

    /**
     * 批量更新任务排序
     * @param todoIds 任务ID列表，按新的排序顺序
     * @return 是否更新成功
     */
    boolean updateTodosSortOrder(List<Long> todoIds);

    /**
     * 批量设置任务完成状态
     * @param completed 完成状态
     * @return 是否更新成功
     */
    boolean setAllTodosCompleted(boolean completed);

    /**
     * 删除所有已完成的任务
     * @return 删除的任务数量
     */
    int deleteCompletedTodos();
}
