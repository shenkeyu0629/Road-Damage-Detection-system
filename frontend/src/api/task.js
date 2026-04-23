import request from '@/utils/request'

export function getTaskList(params) {
  return request({
    url: '/inspection/task/list',
    method: 'get',
    params
  })
}

export function getTaskById(id) {
  return request({
    url: `/inspection/task/${id}`,
    method: 'get'
  })
}

export function createTask(data) {
  return request({
    url: '/inspection/task',
    method: 'post',
    data
  })
}

export function updateTask(data) {
  return request({
    url: '/inspection/task',
    method: 'put',
    data
  })
}

export function deleteTask(id) {
  return request({
    url: `/inspection/task/${id}`,
    method: 'delete'
  })
}

export function startTask(id) {
  return request({
    url: `/inspection/task/${id}/start`,
    method: 'put'
  })
}

export function completeTask(id) {
  return request({
    url: `/inspection/task/${id}/complete`,
    method: 'put'
  })
}

export function getPendingTasks() {
  return request({
    url: '/inspection/task/pending',
    method: 'get'
  })
}
