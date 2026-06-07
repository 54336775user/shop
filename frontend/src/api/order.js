import request from './request'

export function getOrderToken() {
  return request.get('/order/token')
}

export function createOrder(data) {
  return request.post('/order/create', data)
}

export function getOrderList() {
  return request.get('/order/list')
}

export function getOrderDetail(orderId, orderType) {
  const params = orderType ? { orderType } : {}
  return request.get(`/order/${orderId}`, { params })
}

export function cancelOrder(orderId, orderType) {
  const params = orderType ? { orderType } : {}
  return request.put(`/order/${orderId}/cancel`, null, { params })
}

export function payOrder(orderId, orderType) {
  const params = orderType ? { orderType } : {}
  return request.put(`/order/pay/${orderId}`, null, { params })
}

export function expireOrder(orderId, orderType) {
  const params = orderType ? { orderType } : {}
  return request.put(`/order/${orderId}/expire`, null, { params })
}
