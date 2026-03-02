package com.travel.controller.merchant;

import com.travel.entity.merchant.Todo;
import com.travel.service.merchant.TodoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办事项Controller（商家端）
 */
@Slf4j
@RestController
@RequestMapping("/merchant/todos")
public class TodoController {
    
    @Autowired
    private TodoService todoService;
    
    /**
     * 获取待办事项列表
     */
    @GetMapping
    public Map<String, Object> getTodos(@RequestAttribute Long userId) {
        log.info("获取待办事项列表: userId={}", userId);
        
        List<Todo> todos = todoService.getUserTodos(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", todos);
        return result;
    }
    
    /**
     * 创建待办事项
     */
    @PostMapping
    public Map<String, Object> createTodo(@RequestAttribute Long userId, 
                                         @RequestBody Todo todo) {
        log.info("创建待办事项: userId={}, title={}", userId, todo.getTitle());
        
        todo.setUserId(userId);
        Todo created = todoService.createTodo(todo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "创建成功");
        result.put("data", created);
        return result;
    }
    
    /**
     * 更新待办事项
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateTodo(@RequestAttribute Long userId,
                                         @PathVariable Long id,
                                         @RequestBody Todo todo) {
        log.info("更新待办事项: userId={}, todoId={}", userId, id);
        
        todo.setId(id);
        todo.setUserId(userId);
        Todo updated = todoService.updateTodo(todo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        result.put("data", updated);
        return result;
    }
    
    /**
     * 切换完成状态
     */
    @PostMapping("/{id}/toggle")
    public Map<String, Object> toggleComplete(@RequestAttribute Long userId,
                                              @PathVariable Long id) {
        log.info("切换待办事项完成状态: userId={}, todoId={}", userId, id);
        
        todoService.toggleComplete(id, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        return result;
    }
    
    /**
     * 删除待办事项
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteTodo(@RequestAttribute Long userId,
                                         @PathVariable Long id) {
        log.info("删除待办事项: userId={}, todoId={}", userId, id);
        
        todoService.deleteTodo(id, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }
}

