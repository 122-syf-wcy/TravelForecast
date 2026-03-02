package com.travel.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.entity.merchant.Todo;
import com.travel.mapper.merchant.TodoMapper;
import com.travel.service.merchant.TodoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 待办事项服务实现
 */
@Slf4j
@Service
public class TodoServiceImpl extends ServiceImpl<TodoMapper, Todo> implements TodoService {
    
    @Override
    public List<Todo> getUserTodos(Long userId) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
               .orderByDesc(Todo::getCompleted)  // 未完成的在前
               .orderByAsc(Todo::getDeadline);   // 截止日期近的在前
        
        return list(wrapper);
    }
    
    @Override
    @Transactional
    public Todo createTodo(Todo todo) {
        if (todo.getCompleted() == null) {
            todo.setCompleted(false);
        }
        if (todo.getPriority() == null) {
            todo.setPriority("medium");
        }
        
        save(todo);
        log.info("创建待办事项成功: userId={}, title={}", todo.getUserId(), todo.getTitle());
        return todo;
    }
    
    @Override
    @Transactional
    public Todo updateTodo(Todo todo) {
        updateById(todo);
        log.info("更新待办事项成功: id={}", todo.getId());
        return todo;
    }
    
    @Override
    @Transactional
    public void toggleComplete(Long todoId, Long userId) {
        Todo todo = getById(todoId);
        if (todo == null || !todo.getUserId().equals(userId)) {
            throw new RuntimeException("待办事项不存在或无权限操作");
        }
        
        todo.setCompleted(!todo.getCompleted());
        updateById(todo);
        log.info("切换待办事项完成状态: id={}, completed={}", todoId, todo.getCompleted());
    }
    
    @Override
    @Transactional
    public void deleteTodo(Long todoId, Long userId) {
        Todo todo = getById(todoId);
        if (todo == null || !todo.getUserId().equals(userId)) {
            throw new RuntimeException("待办事项不存在或无权限操作");
        }
        
        removeById(todoId);
        log.info("删除待办事项成功: id={}", todoId);
    }
}

