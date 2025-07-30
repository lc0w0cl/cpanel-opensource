package com.clover.cpanel.controller;

import com.clover.cpanel.common.ApiResponse;
import com.clover.cpanel.entity.Todo;
import com.clover.cpanel.service.TodoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TODO任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    @Autowired
    private TodoService todoService;

    /**
     * 获取所有任务
     * @return 任务列表
     */
    @GetMapping
    public ApiResponse<List<Todo>> getAllTodos() {
        try {
            List<Todo> todos = todoService.getAllTodosOrdered();
            return ApiResponse.success(todos);
        } catch (Exception e) {
            log.error("获取TODO任务列表失败", e);
            return ApiResponse.error("获取任务列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据分组ID获取任务列表
     * @param categoryId 分组ID
     * @return 任务列表
     */
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<Todo>> getTodosByCategory(@PathVariable Integer categoryId) {
        try {
            List<Todo> todos = todoService.getTodosByCategoryId(categoryId);
            return ApiResponse.success(todos);
        } catch (Exception e) {
            log.error("获取分组TODO任务列表失败: categoryId={}", categoryId, e);
            return ApiResponse.error("获取分组任务列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据日期范围获取任务列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 任务列表
     */
    @GetMapping("/date-range")
    public ApiResponse<List<Todo>> getTodosByDateRange(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            List<Todo> todos = todoService.getTodosByDateRange(startDate, endDate);
            return ApiResponse.success(todos);
        } catch (Exception e) {
            log.error("根据日期范围获取TODO任务列表失败: startDate={}, endDate={}", startDate, endDate, e);
            return ApiResponse.error("根据日期范围获取任务列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据分组ID和日期范围获取任务列表
     * @param categoryId 分组ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 任务列表
     */
    @GetMapping("/category/{categoryId}/date-range")
    public ApiResponse<List<Todo>> getTodosByCategoryAndDateRange(@PathVariable Integer categoryId,
                                                                  @RequestParam String startDate,
                                                                  @RequestParam String endDate) {
        try {
            List<Todo> todos = todoService.getTodosByCategoryAndDateRange(categoryId, startDate, endDate);
            return ApiResponse.success(todos);
        } catch (Exception e) {
            log.error("根据分组ID和日期范围获取TODO任务列表失败: categoryId={}, startDate={}, endDate={}",
                    categoryId, startDate, endDate, e);
            return ApiResponse.error("根据分组和日期范围获取任务列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建新任务
     * @param request 请求参数
     * @return 创建的任务
     */
    @PostMapping
    public ApiResponse<Todo> createTodo(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ApiResponse.error("任务内容不能为空");
            }

            // 获取分组ID（可选）
            Integer categoryId = null;
            Object categoryIdObj = request.get("categoryId");
            if (categoryIdObj != null) {
                if (categoryIdObj instanceof Integer) {
                    categoryId = (Integer) categoryIdObj;
                } else if (categoryIdObj instanceof String) {
                    try {
                        categoryId = Integer.parseInt((String) categoryIdObj);
                    } catch (NumberFormatException e) {
                        return ApiResponse.error("分组ID格式错误");
                    }
                }
            }

            // 获取日期（可选）
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");

            Todo todo = todoService.createTodo(text.trim(), categoryId, startDate, endDate);
            if (todo != null) {
                return ApiResponse.success(todo);
            } else {
                return ApiResponse.error("创建任务失败");
            }
        } catch (Exception e) {
            log.error("创建TODO任务失败", e);
            return ApiResponse.error("创建任务失败：" + e.getMessage());
        }
    }

    /**
     * 更新任务内容
     * @param id 任务ID
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/{id}/text")
    public ApiResponse<String> updateTodoText(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ApiResponse.error("任务内容不能为空");
            }

            boolean success = todoService.updateTodoText(id, text.trim());
            if (success) {
                return ApiResponse.success("更新任务内容成功");
            } else {
                return ApiResponse.error("更新任务内容失败");
            }
        } catch (Exception e) {
            log.error("更新TODO任务内容失败: id={}", id, e);
            return ApiResponse.error("更新任务内容失败：" + e.getMessage());
        }
    }

    /**
     * 更新任务日期
     * @param id 任务ID
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/{id}/dates")
    public ApiResponse<String> updateTodoDates(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String startDate = request.get("startDate");
            String endDate = request.get("endDate");

            boolean success = todoService.updateTodoDates(id, startDate, endDate);
            if (success) {
                return ApiResponse.success("更新任务日期成功");
            } else {
                return ApiResponse.error("更新任务日期失败");
            }
        } catch (Exception e) {
            log.error("更新TODO任务日期失败: id={}", id, e);
            return ApiResponse.error("更新任务日期失败：" + e.getMessage());
        }
    }

    /**
     * 切换任务完成状态
     * @param id 任务ID
     * @return 操作结果
     */
    @PutMapping("/{id}/toggle")
    public ApiResponse<String> toggleTodoCompleted(@PathVariable Long id) {
        try {
            boolean success = todoService.toggleTodoCompleted(id);
            if (success) {
                return ApiResponse.success("切换任务状态成功");
            } else {
                return ApiResponse.error("切换任务状态失败");
            }
        } catch (Exception e) {
            log.error("切换TODO任务状态失败: id={}", id, e);
            return ApiResponse.error("切换任务状态失败：" + e.getMessage());
        }
    }

    /**
     * 删除任务
     * @param id 任务ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteTodo(@PathVariable Long id) {
        try {
            boolean success = todoService.removeById(id);
            if (success) {
                return ApiResponse.success("删除任务成功");
            } else {
                return ApiResponse.error("删除任务失败");
            }
        } catch (Exception e) {
            log.error("删除TODO任务失败: id={}", id, e);
            return ApiResponse.error("删除任务失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新任务排序
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/sort")
    public ApiResponse<String> updateTodosSortOrder(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> todoIds = request.get("todoIds");
            if (todoIds == null || todoIds.isEmpty()) {
                return ApiResponse.error("任务ID列表不能为空");
            }

            boolean success = todoService.updateTodosSortOrder(todoIds);
            if (success) {
                return ApiResponse.success("更新任务排序成功");
            } else {
                return ApiResponse.error("更新任务排序失败");
            }
        } catch (Exception e) {
            log.error("批量更新TODO任务排序失败", e);
            return ApiResponse.error("更新任务排序失败：" + e.getMessage());
        }
    }

    /**
     * 批量设置任务完成状态
     * @param request 请求参数
     * @return 操作结果
     */
    @PutMapping("/toggle-all")
    public ApiResponse<String> setAllTodosCompleted(@RequestBody Map<String, Boolean> request) {
        try {
            Boolean completed = request.get("completed");
            if (completed == null) {
                return ApiResponse.error("完成状态参数不能为空");
            }

            boolean success = todoService.setAllTodosCompleted(completed);
            if (success) {
                return ApiResponse.success("批量设置任务状态成功");
            } else {
                return ApiResponse.error("批量设置任务状态失败");
            }
        } catch (Exception e) {
            log.error("批量设置TODO任务状态失败", e);
            return ApiResponse.error("批量设置任务状态失败：" + e.getMessage());
        }
    }

    /**
     * 删除所有已完成的任务
     * @return 操作结果
     */
    @DeleteMapping("/completed")
    public ApiResponse<Map<String, Integer>> deleteCompletedTodos() {
        try {
            int deletedCount = todoService.deleteCompletedTodos();
            Map<String, Integer> result = Map.of("deletedCount", deletedCount);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("删除已完成TODO任务失败", e);
            return ApiResponse.error("删除已完成任务失败：" + e.getMessage());
        }
    }
}
