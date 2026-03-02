package com.travel.service.merchant;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.entity.merchant.Todo;

import java.util.List;

/**
 * 待办事项服务接口
 */
public interface TodoService extends IService<Todo> {
    
    /**
     * 获取用户的待办事项列表
     */
    List<Todo> getUserTodos(Long userId);
    
    /**
     * 创建待办事项
     */
    Todo createTodo(Todo todo);
    
    /**
     * 更新待办事项
     */
    Todo updateTodo(Todo todo);
    
    /**
     * 完成/取消完成待办事项
     */
    void toggleComplete(Long todoId, Long userId);
    
    /**
     * 删除待办事项
     */
    void deleteTodo(Long todoId, Long userId);
}

