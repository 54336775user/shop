import { ref } from 'vue'
import { getCartList } from '../api/cart'

const cartCount = ref(0)

export function useCartCount() {
  const refreshCartCount = async () => {
    if (!localStorage.getItem('token')) {
      cartCount.value = 0
      return cartCount.value
    }

    try {
      const res = await getCartList()
      if (res.code === 200 && Array.isArray(res.data)) {
        cartCount.value = res.data.reduce((sum, item) => sum + Number(item.quantity ?? 0), 0)
        return cartCount.value
      }
    } catch {
      // ignore
    }

    cartCount.value = 0
    return cartCount.value
  }

  const clearCartCount = () => {
    cartCount.value = 0
  }

  return {
    cartCount,
    refreshCartCount,
    clearCartCount
  }
}
