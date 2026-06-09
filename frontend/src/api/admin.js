import axios from 'axios'
import { ElMessage } from 'element-plus'
import adminRequest from './adminrequest'

const authFree = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 响应仍转成 data，和 request.js 一致
authFree.interceptors.response.use(
  response => response.data,
  error => {
    const msg = error.response?.data?.message || '网络异常，请稍后重试'
    ElMessage.error(msg)  // 需在文件里 import ElMessage
    return Promise.reject(error)
  }
)

export function adminLogin(data) {
  return authFree.post('/admin/login', data)
}
export function adminRegister(data) {
  return authFree.post('/admin/register', data)
}

export function getAdminOrderList(params = {}) {
  return adminRequest.get('/admin/order/list', { params })
}

export function getAdminOrderDetail(id, orderType) {
  const params = orderType ? { orderType } : {}
  return adminRequest.get(`/admin/order/${id}`, { params })
}

export function updateAdminOrderStatus(id, status) {
  return adminRequest.put('/admin/order/status', null, { params: { id, status } })
}

export function cancelAdminOrder(id) {
  return adminRequest.put(`/admin/order/cancel/${id}`)
}