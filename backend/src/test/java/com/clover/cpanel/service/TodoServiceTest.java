package com.clover.cpanel.service;

import com.clover.cpanel.entity.Todo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO任务服务测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TodoServiceTest {

    @Autowired
    private TodoService todoService;

    @Test
    public void testCreateTodo() {
        // 测试创建任务
        String text = "测试任务";
        Todo todo = todoService.createTodo(text);
        
        assertNotNull(todo);
        assertEquals(text, todo.getText());
        assertFalse(todo.getCompleted());
        assertNotNull(todo.getSortOrder());
        assertNotNull(todo.getCreatedAt());
        assertNotNull(todo.getUpdatedAt());
    }

    @Test
    public void testGetAllTodosOrdered() {
        // 创建几个测试任务
        Todo todo1 = todoService.createTodo("任务1");
        Todo todo2 = todoService.createTodo("任务2");
        Todo todo3 = todoService.createTodo("任务3");
        
        // 获取所有任务
        List<Todo> todos = todoService.getAllTodosOrdered();
        
        assertNotNull(todos);
        assertTrue(todos.size() >= 3);
        
        // 验证包含我们创建的任务
        assertTrue(todos.stream().anyMatch(t -> t.getText().equals("任务1")));
        assertTrue(todos.stream().anyMatch(t -> t.getText().equals("任务2")));
        assertTrue(todos.stream().anyMatch(t -> t.getText().equals("任务3")));
    }

    @Test
    public void testUpdateTodoText() {
        // 创建任务
        Todo todo = todoService.createTodo("原始任务");
        assertNotNull(todo);
        
        // 更新任务内容
        String newText = "更新后的任务";
        boolean success = todoService.updateTodoText(todo.getId(), newText);
        
        assertTrue(success);
        
        // 验证更新结果
        Todo updatedTodo = todoService.getById(todo.getId());
        assertNotNull(updatedTodo);
        assertEquals(newText, updatedTodo.getText());
    }

    @Test
    public void testToggleTodoCompleted() {
        // 创建任务
        Todo todo = todoService.createTodo("测试任务");
        assertNotNull(todo);
        assertFalse(todo.getCompleted());
        
        // 切换完成状态
        boolean success = todoService.toggleTodoCompleted(todo.getId());
        assertTrue(success);
        
        // 验证状态已切换
        Todo updatedTodo = todoService.getById(todo.getId());
        assertNotNull(updatedTodo);
        assertTrue(updatedTodo.getCompleted());
        
        // 再次切换
        success = todoService.toggleTodoCompleted(todo.getId());
        assertTrue(success);
        
        updatedTodo = todoService.getById(todo.getId());
        assertNotNull(updatedTodo);
        assertFalse(updatedTodo.getCompleted());
    }

    @Test
    public void testUpdateTodosSortOrder() {
        // 创建几个任务
        Todo todo1 = todoService.createTodo("任务1");
        Todo todo2 = todoService.createTodo("任务2");
        Todo todo3 = todoService.createTodo("任务3");
        
        // 更新排序
        List<Long> todoIds = Arrays.asList(todo3.getId(), todo1.getId(), todo2.getId());
        boolean success = todoService.updateTodosSortOrder(todoIds);
        
        assertTrue(success);
        
        // 验证排序结果
        List<Todo> orderedTodos = todoService.getAllTodosOrdered();
        assertNotNull(orderedTodos);
        
        // 找到我们的任务并验证排序
        Todo foundTodo3 = orderedTodos.stream().filter(t -> t.getId().equals(todo3.getId())).findFirst().orElse(null);
        Todo foundTodo1 = orderedTodos.stream().filter(t -> t.getId().equals(todo1.getId())).findFirst().orElse(null);
        Todo foundTodo2 = orderedTodos.stream().filter(t -> t.getId().equals(todo2.getId())).findFirst().orElse(null);
        
        assertNotNull(foundTodo3);
        assertNotNull(foundTodo1);
        assertNotNull(foundTodo2);
        
        assertEquals(1, foundTodo3.getSortOrder().intValue());
        assertEquals(2, foundTodo1.getSortOrder().intValue());
        assertEquals(3, foundTodo2.getSortOrder().intValue());
    }

    @Test
    public void testSetAllTodosCompleted() {
        // 创建几个任务
        todoService.createTodo("任务1");
        todoService.createTodo("任务2");
        todoService.createTodo("任务3");
        
        // 设置所有任务为已完成
        boolean success = todoService.setAllTodosCompleted(true);
        assertTrue(success);
        
        // 验证所有任务都已完成
        List<Todo> todos = todoService.getAllTodosOrdered();
        for (Todo todo : todos) {
            assertTrue(todo.getCompleted());
        }
        
        // 设置所有任务为未完成
        success = todoService.setAllTodosCompleted(false);
        assertTrue(success);
        
        // 验证所有任务都未完成
        todos = todoService.getAllTodosOrdered();
        for (Todo todo : todos) {
            assertFalse(todo.getCompleted());
        }
    }

    @Test
    public void testDeleteCompletedTodos() {
        // 创建几个任务
        Todo todo1 = todoService.createTodo("任务1");
        Todo todo2 = todoService.createTodo("任务2");
        Todo todo3 = todoService.createTodo("任务3");
        
        // 设置部分任务为已完成
        todoService.toggleTodoCompleted(todo1.getId());
        todoService.toggleTodoCompleted(todo3.getId());
        
        // 删除已完成的任务
        int deletedCount = todoService.deleteCompletedTodos();
        assertEquals(2, deletedCount);
        
        // 验证只剩下未完成的任务
        List<Todo> remainingTodos = todoService.getAllTodosOrdered();
        assertEquals(1, remainingTodos.stream().filter(t -> t.getText().equals("任务2")).count());
        assertEquals(0, remainingTodos.stream().filter(t -> t.getText().equals("任务1")).count());
        assertEquals(0, remainingTodos.stream().filter(t -> t.getText().equals("任务3")).count());
    }
}
