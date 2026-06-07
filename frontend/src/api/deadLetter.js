import adminRequest from './adminrequest'

export function getDeadLetterPendingCount() {
  return adminRequest.get('/admin/mq/dead-letter/pending-count')
}

export function getDeadLetterList(params = {}) {
  return adminRequest.get('/admin/mq/dead-letter/list', { params })
}

export function getDeadLetterDetail(id) {
  return adminRequest.get(`/admin/mq/dead-letter/${id}`)
}

export function markDeadLetterDone(id, remark) {
  return adminRequest.put(`/admin/mq/dead-letter/${id}/done`, null, {
    params: remark ? { remark } : undefined
  })
}
