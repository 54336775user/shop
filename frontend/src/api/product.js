import adminRequest from './adminrequest'
import request from './request'

export function getProductList(params = {}) {
  return adminRequest.get('/admin/product/list', { params })
}

export function getPublicProductList(params = {}) {
  return request.get('/product/list', { params })
}

export function getPublicFlashSaleList(params = {}) {
  return request.get('/product/flash-sale', { params })
}

export function getPublicProductByCategory(categoryId) {
  return request.get(`/product/category/${categoryId}`)
}

export function getProductDetail(id) {
  return request.get(`/product/${id}`)
}

export function addProduct(data) {
  return adminRequest.post('/admin/product/add', data)
}

export function updateProduct(data) {
  return adminRequest.put('/admin/product/update', data)
}

export function deleteProduct(id) {
  return adminRequest.delete(`/admin/product/delete/${id}`)
}

export function onShelfProduct(id) {
  return adminRequest.put(`/admin/product/on-shelf/${id}`)
}

export function offShelfProduct(id) {
  return adminRequest.put(`/admin/product/off-shelf/${id}`)
}
