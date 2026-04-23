import request from '@/utils/request'

export function getRoadList(params) {
  return request({
    url: '/road/list',
    method: 'get',
    params
  })
}

export function getRoadById(id) {
  return request({
    url: `/road/${id}`,
    method: 'get'
  })
}

export function createRoad(data) {
  return request({
    url: '/road',
    method: 'post',
    data
  })
}

export function updateRoad(data) {
  return request({
    url: '/road',
    method: 'put',
    data
  })
}

export function deleteRoad(id) {
  return request({
    url: `/road/${id}`,
    method: 'delete'
  })
}

export function getSectionList(params) {
  return request({
    url: '/road/section/list',
    method: 'get',
    params
  })
}

export function getRoadSections(roadId) {
  return request({
    url: `/road/${roadId}/sections`,
    method: 'get'
  })
}

export function createSection(data) {
  return request({
    url: '/road/section',
    method: 'post',
    data
  })
}

export function updateSection(data) {
  return request({
    url: '/road/section',
    method: 'put',
    data
  })
}

export function deleteSection(id) {
  return request({
    url: `/road/section/${id}`,
    method: 'delete'
  })
}
