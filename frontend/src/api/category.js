import request from './request'
import adminRequest from './adminrequest'

export function getCategoryList() {
  return request.get('/category/list')
}

export function getAdminCategoryList() {
  return adminRequest.get('/admin/category/list')
}

export function addCategory(data) {
  return adminRequest.post('/admin/category/add', data)
}

export function updateCategory(data) {
  return adminRequest.put('/admin/category/update', data)
}

export function deleteCategory(id) {
  return adminRequest.delete(`/admin/category/delete/${id}`)
}
