import request from './request'

export function getGroupBuyActivity(productId) {
  return request.get(`/group-buy/activity/${productId}`)
}

export function getGroupBuyTeams(productId) {
  return request.get(`/group-buy/product/${productId}/teams`)
}

export function openGroupBuy(activityId) {
  return request.post(`/group-buy/activity/${activityId}/open`)
}

export function joinGroupBuy(teamId) {
  return request.post(`/group-buy/team/${teamId}/join`)
}
