import request from '@/utils/request'

export function getDamageStatistics(params) {
  return request({
    url: '/statistics/damage',
    method: 'get',
    params
  })
}

export function getDamageTrend(params) {
  return request({
    url: '/statistics/trend',
    method: 'get',
    params
  })
}

export function getDamageDistribution(params) {
  return request({
    url: '/statistics/distribution',
    method: 'get',
    params
  })
}
