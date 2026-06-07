<template>
  <div class="cart-page">
    <header class="header">
      <el-button link type="primary" :icon="ArrowLeft" @click="goHome">返回首页</el-button>
      <div class="page-title">
        <h1>购物车</h1>
        <p>管理你的购物商品，修改数量或删除不需要的商品</p>
      </div>
      <div class="header-actions">
        <el-button link type="primary" @click="refreshCart">刷新</el-button>
        <el-button type="danger" link @click="handleLogout">退出</el-button>
      </div>
    </header>

    <main class="main-content" v-loading="loading">
      <el-empty v-if="!loading && cartItems.length === 0" description="购物车还是空的">
        <el-button type="primary" @click="goHome">去逛逛</el-button>
      </el-empty>

      <template v-else>
        <el-table
          ref="tableRef"
          :data="cartItems"
          border
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" :selectable="canSelectRow" />
          <el-table-column label="商品" min-width="280">
            <template #default="{ row }">
              <div class="product-cell">
                <div class="thumb">
                  <template v-if="row.productImage">
                    <img :src="row.productImage" :alt="row.productName" />
                  </template>
                  <template v-else>
                    <span>暂无图片</span>
                  </template>
                </div>
                <div class="product-meta">
                  <div class="name">{{ row.productName }}</div>
                  <div class="sub">
                    <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
                      {{ row.status === 1 ? '上架中' : '已下架' }}
                    </el-tag>
                    <span>库存 {{ row.stock }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">¥ {{ formatMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="180">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="row.stock"
                :disabled="!row.selectable"
                size="small"
                @change="value => handleQuantityChange(row, value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥ {{ formatMoney(row.subtotal) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="danger" link @click="handleRemove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="summary-bar">
          <div class="summary-left">
            <el-checkbox
              :model-value="allSelected"
              :indeterminate="indeterminate"
              @change="toggleSelectAll"
            >
              全选
            </el-checkbox>
            <span>已选 {{ selectedRows.length }} 件商品</span>
            <span>合计 ¥ {{ formatMoney(selectedTotalAmount) }}</span>
          </div>
          <div class="summary-right">
            <el-button
              type="danger"
              :loading="checkoutLoading"
              :disabled="selectedRows.length === 0 || checkoutLoading"
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCartList, removeCartItem, updateCartQuantity } from '../api/cart'
import { createOrder, getOrderToken } from '../api/order'
import { useCartCount } from '../composables/useCartCount'

const router = useRouter()
const { refreshCartCount, clearCartCount } = useCartCount()
const loading = ref(false)
const cartItems = ref([])
const selectedRows = ref([])
const tableRef = ref(null)
const checkoutLoading = ref(false)

const canSelectRow = (row) => !!row.selectable

const formatMoney = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '-'
  }
  return num.toFixed(2)
}

const normalizeCartItem = (item) => ({
  cartItemId: Number(item.cartItemId ?? item.id),
  productId: Number(item.productId ?? item.product_id),
  productName: item.productName || item.product_name || '',
  productImage: item.productImage || item.product_image || '',
  price: item.price ?? 0,
  quantity: Number(item.quantity ?? 1),
  stock: Number(item.stock ?? 0),
  status: Number(item.status ?? 1),
  subtotal: item.subtotal ?? 0,
  selectable: Boolean(item.selectable ?? true),
  createTime: item.createTime ?? item.create_time ?? null
})

const selectedTotalAmount = computed(() => {
  return selectedRows.value.reduce((sum, item) => sum + Number(item.subtotal ?? 0), 0)
})

const allSelected = computed(() => {
  const selectableRows = cartItems.value.filter(item => item.selectable)
  if (selectableRows.length === 0) {
    return false
  }
  return selectedRows.value.length === selectableRows.length
})

const indeterminate = computed(() => {
  return selectedRows.value.length > 0 && !allSelected.value
})

const loadCart = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    if (res.code === 200 && Array.isArray(res.data)) {
      cartItems.value = res.data.map(normalizeCartItem)
      await nextTick()
      selectedRows.value = cartItems.value.filter(item => item.selectable)
      tableRef.value?.clearSelection?.()
      cartItems.value.forEach(row => {
        if (row.selectable) {
          tableRef.value?.toggleRowSelection?.(row, true)
        }
      })
      await refreshCartCount()
      return
    }
    cartItems.value = []
    selectedRows.value = []
    await refreshCartCount()
  } catch {
    cartItems.value = []
    selectedRows.value = []
    await refreshCartCount()
  } finally {
    loading.value = false
  }
}

const refreshCart = async () => {
  await loadCart()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const toggleSelectAll = (checked) => {
  tableRef.value?.clearSelection?.()
  if (checked) {
    cartItems.value.forEach(row => {
      if (row.selectable) {
        tableRef.value?.toggleRowSelection?.(row, true)
      }
    })
  }
}

const handleQuantityChange = async (row, value) => {
  const quantity = Number(value)
  if (!quantity || quantity < 1) {
    ElMessage.warning('数量必须大于 0')
    await loadCart()
    return
  }
  if (quantity > Number(row.stock ?? 0)) {
    ElMessage.warning('库存不足')
    await loadCart()
    return
  }

  try {
    const res = await updateCartQuantity({
      cartItemId: row.cartItemId,
      quantity
    })
    if (res.code === 200) {
      ElMessage.success('数量已更新')
      await loadCart()
      return
    }
  } catch {
    // 交给统一错误提示处理
  }
  await loadCart()
}

const handleRemove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.productName}」吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const res = await removeCartItem(row.cartItemId)
    if (res.code === 200) {
      ElMessage.success('已删除')
      await loadCart()
    }
  } catch {
    // 统一错误提示已处理
  }
}

const handleCheckout = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  if (checkoutLoading.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确定对已选的 ${selectedRows.value.length} 件商品下单吗？`, '提示', {
      confirmButtonText: '去下单',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  checkoutLoading.value = true
  try {
    const tokenRes = await getOrderToken()
    if (tokenRes.code !== 200 || !tokenRes.data?.token) {
      throw new Error('获取订单令牌失败')
    }

    const res = await createOrder({
      cartItemIds: selectedRows.value.map(item => item.cartItemId),
      token: tokenRes.data.token
    })
    if (res.code === 200 && res.data?.orderId) {
      ElMessage.success('下单成功')
      await loadCart()
      await refreshCartCount()
      router.push({
        path: `/pay/${res.data.orderId}`
      })
    }
  } catch {
    // 统一错误提示已处理
  } finally {
    checkoutLoading.value = false
  }
}

const goHome = () => {
  router.push('/home')
}

const handleLogout = () => {
  localStorage.removeItem('token')
  clearCartCount()
  router.push('/login')
}

onMounted(() => {
  loadCart()
  refreshCartCount()
})
</script>

<style scoped>
.cart-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 40px;
  height: 70px;
  background-color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.page-title {
  flex: 1;
}

.page-title h1 {
  margin: 0 0 4px;
  font-size: 24px;
  color: #303133;
}

.page-title p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.main-content {
  flex: 1;
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px 40px 40px;
  box-sizing: border-box;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 88px;
  height: 88px;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f2f6fc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  flex-shrink: 0;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.sub {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 12px;
}

.summary-bar {
  margin-top: 18px;
  padding: 16px 20px;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 18px;
  color: #606266;
  font-size: 14px;
  flex-wrap: wrap;
}
</style>
