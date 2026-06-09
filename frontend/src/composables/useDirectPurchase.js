import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderToken, createOrder } from '../api/order'
import { addToCart } from '../api/cart'

export function useDirectPurchase() {
  const router = useRouter()
  const buying = ref(false)

  const ensureLogin = () => {
    if (!localStorage.getItem('token')) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return false
    }
    return true
  }

  const buyNow = async (productId, quantity = 1) => {
    if (!ensureLogin()) {
      return false
    }
    if (!productId) {
      ElMessage.error('商品信息无效')
      return false
    }

    buying.value = true
    try {
      const tokenRes = await getOrderToken()
      if (tokenRes.code !== 200 || !tokenRes.data?.token) {
        ElMessage.error(tokenRes.message || '获取下单令牌失败')
        return false
      }

      const orderRes = await createOrder({
        productId,
        quantity,
        token: tokenRes.data.token
      })
      if (orderRes.code !== 200 || !orderRes.data?.orderId) {
        ElMessage.error(orderRes.message || '下单失败')
        return false
      }

      ElMessage.success('订单已创建，请完成支付')
      router.push({
        path: `/pay/${orderRes.data.orderId}`,
        query: { orderType: 1 }
      })
      return true
    } catch {
      return false
    } finally {
      buying.value = false
    }
  }

  const addProductToCart = async (productId, quantity = 1, productName = '') => {
    if (!ensureLogin()) {
      return false
    }

    try {
      const res = await addToCart({ productId, quantity })
      if (res.code === 200) {
        ElMessage.success(productName ? `「${productName}」已加入购物车` : '已加入购物车')
        return true
      }
      ElMessage.error(res.message || '加入购物车失败')
      return false
    } catch {
      return false
    }
  }

  return {
    buying,
    buyNow,
    addProductToCart,
    ensureLogin
  }
}
