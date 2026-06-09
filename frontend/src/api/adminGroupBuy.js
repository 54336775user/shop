import adminRequest from './adminrequest'

export function getAdminGroupBuyList(params = {}) {
  return adminRequest.get('/admin/group-buy/list', { params })
}

export function getAdminGroupBuyDetail(id) {
  return adminRequest.get(`/admin/group-buy/${id}`)
}

export function addGroupBuyActivity(data) {
  return adminRequest.post('/admin/group-buy/add', data)
}

export function updateGroupBuyActivity(data) {
  return adminRequest.put('/admin/group-buy/update', data)
}

export function enableGroupBuyActivity(id) {
  return adminRequest.put(`/admin/group-buy/enable/${id}`)
}

export function disableGroupBuyActivity(id) {
  return adminRequest.put(`/admin/group-buy/disable/${id}`)
}
