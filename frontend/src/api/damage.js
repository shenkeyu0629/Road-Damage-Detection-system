import request from '@/utils/request'

export function detectDamage(data) {
  return request({
    url: '/damage/detect',
    method: 'post',
    data
  })
}

export function detectAndSave(data) {
  return request({
    url: '/damage/detect-and-save',
    method: 'post',
    data
  })
}

export function getRecordList(params) {
  return request({
    url: '/damage/record/list',
    method: 'get',
    params
  })
}

export function getRecordById(id) {
  return request({
    url: `/damage/record/${id}`,
    method: 'get'
  })
}

export function getDamageList(params) {
  return request({
    url: '/damage/list',
    method: 'get',
    params
  })
}

export function getDamageById(id) {
  return request({
    url: `/damage/${id}`,
    method: 'get'
  })
}

export function updateDamageStatus(id, status) {
  return request({
    url: `/damage/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function getDamagesByRecord(recordId) {
  return request({
    url: `/damage/record/${recordId}/damages`,
    method: 'get'
  })
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
