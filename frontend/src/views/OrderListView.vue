<template>
  <div class="order-page">
    <header class="header">
      <el-button link type="primary" :icon="ArrowLeft" @click="goHome">返回首页</el-button>
      <div class="page-title">
        <h1>我的订单</h1>
        <p>查看下单记录、订单状态和商品明细</p>
      </div>
      <div class="header-actions">
        <el-button link type="primary" @click="refreshOrders">刷新</el-button>
        <el-button type="danger" link @click="handleLogout">退出</el-button>
      </div>
    </header>

    <main class="main-content" v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单">
        <el-button type="primary" @click="goHome">去逛逛</el-button>
      </el-empty>

      <div v-else class="order-list">
        <el-card v-for="order in orders" :key="orderKey(order)" class="order-card" shadow="hover">
          <div class="order-top">
            <div>
              <div class="order-no">
                订单号：{{ order.orderNo }}
                <el-tag v-if="order.orderType === 2" type="danger" effect="plain" size="small" class="seckill-tag">
                  秒杀
                </el-tag>
              </div>
              <div class="order-time">下单时间：{{ formatDateTime(order.createTime) }}</div>
            </div>
            <el-tag :type="statusTagType(order.status)">{{ order.statusText }}</el-tag>
          </div>

          <div class="order-summary">
            <div class="summary-item">
              <span class="label">订单金额</span>
              <span class="value price">¥ {{ formatMoney(order.totalAmount) }}</span>
            </div>
            <div class="summary-item">
              <span class="label">商品明细</span>
              <span class="value">{{ (order.items || []).length }} 件</span>
            </div>
          </div>

          <div class="items-preview">
            <div v-for="item in (order.items || []).slice(0, 3)" :key="item.id" class="item-row">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-info">
                <span>¥ {{ formatMoney(item.price) }}</span>
                <span>x {{ item.quantity }}</span>
              </div>
            </div>
            <div v-if="(order.items || []).length > 3" class="more-text">
              还有 {{ (order.items || []).length - 3 }} 件商品...
            </div>
          </div>

          <div class="order-actions">
            <el-button @click="openDetail(order)">查看详情</el-button>
            <el-button
              v-if="Number(order.status) === 0"
              type="primary"
              @click="goPay(order)"
            >
              去支付
            </el-button>
            <el-button
              v-if="canCancel(order)"
              type="danger"
              plain
              @click="handleCancel(order)"
            >
              取消订单
            </el-button>
          </div>
        </el-card>
      </div>
    </main>

    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <template v-if="detailData">
        <div class="detail-meta">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">
              {{ detailData.orderNo }}
              <el-tag v-if="detailData.orderType === 2" type="danger" effect="plain" size="small" class="seckill-tag">
                秒杀
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(detailData.status)">{{ detailData.statusText }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ formatDateTime(detailData.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥ {{ formatMoney(detailData.totalAmount) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <el-table :data="detailData.items || []" border style="width: 100%; margin-top: 16px">
          <el-table-column label="商品" min-width="220">
            <template #default="{ row }">
              <div class="detail-item">
                <div class="detail-thumb">
                  <template v-if="row.productImage">
                    <img :src="row.productImage" :alt="row.productName" />
                  </template>
                  <template v-else>暂无图片</template>
                </div>
                <div class="detail-name">{{ row.productName }}</div>
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
      </template>
      <template #footer>
        <div class="detail-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="detailData && Number(detailData.status) === 0"
            type="primary"
            @click="goPay(detailData)"
          >
            去支付
          </el-button>
          <el-button
            v-if="detailData && canCancel(detailData)"
            type="danger"
            plain
            @click="handleCancel(detailData)"
          >
            取消订单
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getOrderDetail, getOrderList } from '../api/order'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const orders = ref([])
const detailVisible = ref(false)
const detailData = ref(null)

const formatMoney = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '-'
  }
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
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const statusTagType = (status) => {
  const value = Number(status)
  if (value === 0) return 'danger'
  if (value === 1) return 'warning'
  if (value === 4) return 'info'
  if (value === 2) return 'success'
  if (value === 3) return 'success'
  return 'info'
}

const ORDER_TYPE_SECKILL = 2

const normalizeOrder = (item) => ({
  id: Number(item.id),
  orderType: Number(item.orderType ?? item.order_type ?? 1),
  orderNo: item.orderNo || item.order_no || '',
  totalAmount: item.totalAmount ?? item.total_amount ?? 0,
  status: Number(item.status ?? 0),
  statusText: item.statusText || item.status_text || '',
  createTime: item.createTime ?? item.create_time ?? null,
  items: Array.isArray(item.items) ? item.items.map(normalizeOrderItem) : []
})

const orderKey = (order) => `${order.orderType}-${order.id}`

const canCancel = (order) => {
  const status = Number(order?.status)
  if (Number(order?.orderType) === ORDER_TYPE_SECKILL) {
    return status === 0 || status === 1
  }
  return status === 1
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

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList()
    if (res.code === 200 && Array.isArray(res.data)) {
      orders.value = res.data.map(normalizeOrder)
      return
    }
  } catch {
    // error handled by request interceptor
  } finally {
    loading.value = false
  }
  orders.value = []
}

const refreshOrders = async () => {
  await loadOrders()
}

const openDetail = async (order) => {
  const orderId = typeof order === 'object' ? order.id : order
  const orderType = typeof order === 'object' ? order.orderType : undefined
  try {
    const res = await getOrderDetail(orderId, orderType)
    if (res.code === 200 && res.data) {
      const data = res.data
      detailData.value = {
        id: Number(data.id),
        orderType: Number(data.orderType ?? data.order_type ?? orderType ?? 1),
        orderNo: data.orderNo || data.order_no || '',
        totalAmount: data.totalAmount ?? data.total_amount ?? 0,
        status: Number(data.status ?? 0),
        statusText: data.statusText || data.status_text || '',
        createTime: data.createTime ?? data.create_time ?? null,
        updateTime: data.updateTime ?? data.update_time ?? null,
        items: Array.isArray(data.items) ? data.items.map(normalizeOrderItem) : []
      }
      detailVisible.value = true
    }
  } catch {
    // handled by interceptor
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm(`确定取消订单「${order.orderNo}」吗？`, '提示', {
      confirmButtonText: '取消订单',
      cancelButtonText: '关闭',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const res = await cancelOrder(order.id, order.orderType)
    if (res.code === 200) {
      ElMessage.success('订单已取消')
      await loadOrders()
      if (detailVisible.value && orderKey(detailData.value) === orderKey(order)) {
        await openDetail(order)
      }
    }
  } catch {
    // handled by interceptor
  }
}

const goPay = (order) => {
  const orderId = typeof order === 'object' ? order.id : order
  if (!orderId) {
    return
  }
  const query = typeof order === 'object' && order.orderType === ORDER_TYPE_SECKILL
    ? { orderType: ORDER_TYPE_SECKILL }
    : {}
  router.push({ path: `/pay/${orderId}`, query })
}

const goHome = () => {
  router.push('/home')
}

const handleLogout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(async () => {
  await loadOrders()
  const newOrderId = route.query.newOrderId ? Number(route.query.newOrderId) : null
  const newOrderType = route.query.orderType ? Number(route.query.orderType) : null
  if (newOrderId) {
    const matched = orders.value.find(
      item => item.id === newOrderId && (!newOrderType || item.orderType === newOrderType)
    )
    await openDetail(matched || { id: newOrderId, orderType: newOrderType || 1 })
  }
})
</script>

<style scoped>
.order-page {
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

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border-radius: 12px;
}

.order-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.order-no {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.seckill-tag {
  vertical-align: middle;
}

.order-time {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.order-summary {
  display: flex;
  gap: 18px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.summary-item {
  min-width: 150px;
  padding: 12px 14px;
  background: #f8faff;
  border-radius: 10px;
}

.label {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 6px;
}

.value {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.price {
  color: #f56c6c;
  font-size: 16px;
}

.items-preview {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.item-name {
  color: #303133;
  font-weight: 500;
}

.item-info {
  display: flex;
  gap: 14px;
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.more-text {
  color: #909399;
  font-size: 13px;
}

.order-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.detail-meta {
  margin-bottom: 12px;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-thumb {
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

.detail-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.detail-name {
  color: #303133;
  font-weight: 500;
}
</style>
