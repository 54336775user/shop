<template>
  <div class="payment-page">
    <header class="header">
      <el-button link type="primary" :icon="ArrowLeft" @click="goBack">返回订单列表</el-button>
      <div class="page-title">
        <h1>模拟支付</h1>
        <p>确认订单信息后，点击支付完成状态流转</p>
      </div>
      <div class="header-actions">
        <el-button link type="primary" @click="reloadOrder">刷新</el-button>
      </div>
    </header>

    <main class="main-content">
      <el-skeleton v-if="loading" :rows="8" animated />

      <el-empty v-else-if="!orderData" description="订单不存在或已失效">
        <el-button type="primary" @click="goBack">返回订单列表</el-button>
      </el-empty>

      <template v-else>
        <el-card class="payment-card" shadow="hover">
          <div class="card-header">
            <div>
              <h2>订单信息</h2>
              <p>订单号：{{ orderData.orderNo }}</p>
            </div>
            <el-tag :type="statusTagType(orderData.status)">{{ orderData.statusText }}</el-tag>
          </div>

          <el-descriptions :column="2" border class="summary">
            <el-descriptions-item label="订单金额">
              ¥ {{ formatMoney(orderData.totalAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">
              {{ formatDateTime(orderData.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDateTime(orderData.updateTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="订单状态">
              {{ orderData.statusText }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="payment-card" shadow="hover">
          <template #header>
            <div class="section-title">支付方式</div>
          </template>

          <div class="countdown-panel" :class="{ warning: isExpiringSoon, danger: isExpired }">
            <div class="countdown-main">
              <span class="countdown-label">剩余支付时间</span>
              <div class="countdown-value">{{ countdownText }}</div>
            </div>
            <el-alert
              v-if="isExpired"
              :title="expireSyncing ? '订单已超时，正在同步取消状态…' : '订单已超时并自动取消，请返回订单列表查看'"
              type="error"
              :closable="false"
              show-icon
            />
            <el-alert
              v-else-if="isExpiringSoon"
              title="订单即将超时，请尽快完成支付"
              type="warning"
              :closable="false"
              show-icon
            />
            <el-alert
              v-else
              title="模拟支付仅用于状态流转，点击后订单会直接进入待发货"
              type="info"
              :closable="false"
              show-icon
            />
          </div>

          <el-radio-group v-model="selectedMethod" class="payment-methods">
            <el-radio
              v-for="method in paymentMethods"
              :key="method.value"
              :label="method.value"
              class="payment-method"
            >
              <div class="method-card">
                <div class="method-top">
                  <div class="method-title">{{ method.title }}</div>
                  <el-tag size="small" effect="plain">{{ method.tag }}</el-tag>
                </div>
                <div class="method-desc">{{ method.desc }}</div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-card>

        <el-card class="payment-card" shadow="hover">
          <template #header>
            <div class="section-title">商品明细</div>
          </template>

          <el-table :data="orderData.items || []" border>
            <el-table-column label="商品" min-width="240">
              <template #default="{ row }">
                <div class="item-cell">
                  <div class="thumb">
                    <template v-if="row.productImage">
                      <img :src="row.productImage" :alt="row.productName" />
                    </template>
                    <template v-else>暂无图片</template>
                  </div>
                  <div class="item-name">{{ row.productName }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120">
              <template #default="{ row }">¥ {{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column label="小计" width="120">
              <template #default="{ row }">¥ {{ formatMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="payment-card payment-actions" shadow="hover">
          <div class="pay-tip">
            <p>这是模拟支付页面，点击后会直接把订单状态改成「待发货」。</p>
          </div>
          <div class="actions">
            <el-button @click="goBack">稍后再说</el-button>
            <el-button
              v-if="Number(orderData.status) === 0"
              type="primary"
              :loading="paying"
              :disabled="isExpired"
              @click="handlePay"
            >
              {{ isExpired ? '订单已超时' : `确认支付（${selectedMethodLabel}）` }}
            </el-button>
            <el-button
              v-else
              type="primary"
              @click="goBack"
            >
              返回订单列表
            </el-button>
          </div>
        </el-card>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { expireOrder, getOrderDetail, payOrder } from '../api/order'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const paying = ref(false)
const orderData = ref(null)
const selectedMethod = ref('alipay')
const remainingSeconds = ref(0)
const countdownTimer = ref(null)
const expireSyncing = ref(false)
const expireSynced = ref(false)

const PAYMENT_WINDOW_SECONDS = 15 * 60

const paymentMethods = [
  {
    value: 'alipay',
    title: '支付宝',
    tag: '推荐',
    desc: '扫码支付 / 扫码确认，适合快速完成模拟付款。'
  },
  {
    value: 'wechat',
    title: '微信支付',
    tag: '常用',
    desc: '打开微信完成确认，模拟日常高频支付场景。'
  },
  {
    value: 'bank',
    title: '银行卡',
    tag: '传统',
    desc: '模拟银行卡快捷支付，适合展示多支付方式。'
  }
]

const orderId = computed(() => Number(route.params.orderId || route.query.orderId || 0))
const orderType = computed(() => {
  const value = Number(route.query.orderType || 0)
  return value === 2 ? 2 : undefined
})
const selectedMethodLabel = computed(
  () => paymentMethods.find(item => item.value === selectedMethod.value)?.title || '支付'
)

const isExpired = computed(() => {
  if (!orderData.value) {
    return false
  }
  return Number(orderData.value.status) === 0 && remainingSeconds.value <= 0
})

const isExpiringSoon = computed(() => {
  if (!orderData.value) {
    return false
  }
  return Number(orderData.value.status) === 0 && remainingSeconds.value > 0 && remainingSeconds.value <= 5 * 60
})

const countdownText = computed(() => {
  if (!orderData.value || Number(orderData.value.status) !== 0) {
    return '无需支付'
  }
  if (remainingSeconds.value <= 0) {
    return '00:00:00'
  }
  const hours = Math.floor(remainingSeconds.value / 3600)
  const minutes = Math.floor((remainingSeconds.value % 3600) / 60)
  const seconds = remainingSeconds.value % 60
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
})

const formatMoney = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  return num.toFixed(2)
}

const parseDateTime = (value) => {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDateTime = (value) => {
  const date = parseDateTime(value)
  if (!date) return '-'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const statusTagType = (status) => {
  const value = Number(status)
  if (value === 0) return 'danger'
  if (value === 1) return 'warning'
  if (value === 2) return 'success'
  if (value === 3) return 'success'
  if (value === 4) return 'info'
  return 'info'
}

const normalizeOrderItem = (item) => ({
  id: Number(item.id),
  productId: Number(item.productId ?? item.product_id),
  productName: item.productName || item.product_name || '',
  productImage: item.productImage || item.product_image || '',
  price: item.price ?? 0,
  quantity: Number(item.quantity ?? 0),
  amount: item.amount ?? 0
})

const normalizeOrder = (data) => ({
  id: Number(data.id),
  orderType: Number(data.orderType ?? data.order_type ?? orderType.value ?? 1),
  orderNo: data.orderNo || data.order_no || '',
  totalAmount: data.totalAmount ?? data.total_amount ?? 0,
  status: Number(data.status ?? 0),
  statusText: data.statusText || data.status_text || '',
  createTime: data.createTime ?? data.create_time ?? null,
  updateTime: data.updateTime ?? data.update_time ?? null,
  items: Array.isArray(data.items) ? data.items.map(normalizeOrderItem) : []
})

const clearCountdownTimer = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
}

const updateCountdown = () => {
  if (!orderData.value || Number(orderData.value.status) !== 0) {
    remainingSeconds.value = 0
    return
  }

  const createTime = parseDateTime(orderData.value.createTime)
  if (!createTime) {
    remainingSeconds.value = 0
    return
  }

  const deadline = createTime.getTime() + PAYMENT_WINDOW_SECONDS * 1000
  const diffSeconds = Math.floor((deadline - Date.now()) / 1000)
  remainingSeconds.value = Math.max(0, diffSeconds)
}

const startCountdown = () => {
  clearCountdownTimer()
  updateCountdown()

  if (!orderData.value || Number(orderData.value.status) !== 0) {
    return
  }

  countdownTimer.value = window.setInterval(() => {
    updateCountdown()
    if (remainingSeconds.value <= 0) {
      clearCountdownTimer()
      syncExpireIfNeeded()
    }
  }, 1000)
}

const fetchOrderDetail = async () => {
  const res = await getOrderDetail(orderId.value, orderType.value)
  if (res.code === 200 && res.data) {
    orderData.value = normalizeOrder(res.data)
    return true
  }
  orderData.value = null
  return false
}

const syncExpireIfNeeded = async () => {
  if (!orderId.value || !orderData.value || Number(orderData.value.status) !== 0) {
    return
  }
  if (remainingSeconds.value > 0) {
    return
  }
  if (expireSyncing.value || expireSynced.value) {
    return
  }

  expireSyncing.value = true
  try {
    const res = await expireOrder(orderId.value, orderData.value.orderType)
    if (res.code === 200) {
      expireSynced.value = true
      await fetchOrderDetail()
      clearCountdownTimer()
    }
  } catch {
    // handled by interceptor
  } finally {
    expireSyncing.value = false
  }
}

const loadOrder = async () => {
  if (!orderId.value) {
    orderData.value = null
    clearCountdownTimer()
    return
  }

  expireSynced.value = false
  loading.value = true
  try {
    if (await fetchOrderDetail()) {
      startCountdown()
      if (Number(orderData.value.status) === 0 && remainingSeconds.value <= 0) {
        await syncExpireIfNeeded()
      }
      return
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
  clearCountdownTimer()
}

const reloadOrder = async () => {
  await loadOrder()
}

const handlePay = async () => {
  if (!orderData.value || Number(orderData.value.status) !== 0) {
    return
  }
  if (isExpired.value) {
    await syncExpireIfNeeded()
    ElMessage.warning('订单已超时，请返回订单列表查看')
    return
  }
  if (paying.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认使用「${selectedMethodLabel.value}」支付订单「${orderData.value.orderNo}」吗？`,
      '模拟支付',
      {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'warning'
      }
    )
  } catch {
    return
  }

  paying.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 900))
    const res = await payOrder(orderData.value.id, orderData.value.orderType)
    if (res.code === 200) {
      ElMessage.success(`${selectedMethodLabel.value}支付成功`)
      clearCountdownTimer()
      const query = { newOrderId: orderData.value.id }
      if (orderData.value.orderType === 2) {
        query.orderType = 2
      }
      router.replace({
        path: '/orders',
        query
      })
    }
  } catch {
    // handled by interceptor
  } finally {
    paying.value = false
  }
}

const goBack = () => {
  clearCountdownTimer()
  router.push('/orders')
}

watch(() => [route.params.orderId, route.query.orderType], () => {
  expireSynced.value = false
  loadOrder()
})

onMounted(loadOrder)

onBeforeUnmount(() => {
  clearCountdownTimer()
})
</script>

<style scoped>
.payment-page {
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

.main-content {
  flex: 1;
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px 40px 40px;
  box-sizing: border-box;
}

.payment-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-header h2 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #303133;
}

.card-header p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.summary {
  margin-top: 16px;
}

.countdown-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  margin-bottom: 16px;
  background: #f8faff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
}

.countdown-panel.warning {
  background: #fdf6ec;
  border-color: #f5dab1;
}

.countdown-panel.danger {
  background: #fef0f0;
  border-color: #fbc4c4;
}

.countdown-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.countdown-label {
  color: #606266;
  font-size: 13px;
}

.countdown-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 1px;
}

.payment-methods {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.payment-method {
  margin-right: 0;
  height: auto;
  align-items: stretch;
}

.method-card {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  background: #fff;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.payment-method:hover .method-card {
  border-color: #c0c4cc;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.method-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.method-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.method-desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  overflow: hidden;
  background: #f2f6fc;
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

.item-name {
  color: #303133;
  font-weight: 500;
}

.payment-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.pay-tip p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 960px) {
  .payment-methods {
    grid-template-columns: 1fr;
  }

  .payment-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .actions {
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}
</style>
