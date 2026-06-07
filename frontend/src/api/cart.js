import request from './request'

export function getCartList() {
  return request.get('/cart/list')
}

export function addToCart(data) {
  return request.post('/cart/add', data)
}

export function updateCartQuantity(data) {
  return request.put('/cart/quantity', data)
}

export function removeCartItem(cartItemId) {
  return request.delete(`/cart/${cartItemId}`)
}
