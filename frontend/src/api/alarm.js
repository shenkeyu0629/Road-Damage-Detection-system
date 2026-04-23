import request from '@/utils/request'

export function getAlarmList(params) {
  return request({
    url: '/alarm/list',
    method: 'get',
    params
  })
}

export function getAlarmById(id) {
  return request({
    url: `/alarm/${id}`,
    method: 'get'
  })
}

export function handleAlarm(id, data) {
  return request({
    url: `/alarm/${id}/handle`,
    method: 'put',
    params: data
  })
}

export function createDisposal(data) {
  return request({
    url: '/alarm/disposal',
    method: 'post',
    data
  })
}

export function getDisposalList(params) {
  return request({
    url: '/alarm/disposal/list',
    method: 'get',
    params
  })
}

export function updateDisposalStatus(id, status, verifyResult) {
  return request({
    url: `/alarm/disposal/${id}/status`,
    method: 'put',
    params: { status, verifyResult }
  })
}
