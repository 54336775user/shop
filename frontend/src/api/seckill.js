import request from './request'

export function getSeckillList(params = {}) {
  return request.get('/seckill/list', { params })
}

export function getSeckillBuyDegradeStatus() {
  return request.get('/seckill/degrade/buy')
}

export function getSeckillToken(productId) {
  return request.get(`/seckill/${productId}/token`)
}

export function buySeckill(productId, data) {
  return request.post(`/seckill/${productId}/buy`, data)
}

export function getSeckillResult(requestId) {
  return request.get(`/seckill/result/${requestId}`)
}
