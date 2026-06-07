import adminRequest from './adminrequest'

export function getSeckillBuyDegradeStatus() {
  return adminRequest.get('/admin/seckill/degrade/buy')
}

export function enableSeckillBuyDegrade() {
  return adminRequest.put('/admin/seckill/degrade/buy/enable')
}

export function disableSeckillBuyDegrade() {
  return adminRequest.put('/admin/seckill/degrade/buy/disable')
}

export function refreshSeckillListCache() {
  return adminRequest.put('/admin/seckill/cache/list/refresh')
}
