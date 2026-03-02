/**
 * 待办事项API
 */
import request from '@/utils/request'

/**
 * 获取待办事项列表
 */
export function getTodos() {
  return request({
    url: '/merchant/todos',
    method: 'get'
  })
}

/**
 * 创建待办事项
 */
export function createTodo(data: {
  title: string
  priority: string
  deadline: string
}) {
  return request({
    url: '/merchant/todos',
    method: 'post',
    data
  })
}

/**
 * 更新待办事项
 */
export function updateTodo(id: number, data: {
  title: string
  priority: string
  deadline: string
}) {
  return request({
    url: `/merchant/todos/${id}`,
    method: 'put',
    data
  })
}

/**
 * 切换完成状态
 */
export function toggleTodoComplete(id: number) {
  return request({
    url: `/merchant/todos/${id}/toggle`,
    method: 'post'
  })
}

/**
 * 删除待办事项
 */
export function deleteTodo(id: number) {
  return request({
    url: `/merchant/todos/${id}`,
    method: 'delete'
  })
}

