<template>
  <div class="seckill-page">
    <header class="header">
      <el-button link type="primary" :icon="ArrowLeft" @click="goHome">返回首页</el-button>
      <div class="page-title">
        <h1>限时秒杀</h1>
        <p>独立秒杀页，支持倒计时展示、抢购提交和结果轮询</p>
      </div>
      <div class="header-actions">
        <el-button link type="primary" @click="refreshSeckillList">刷新</el-button>
        <el-button link type="primary" @click="goOrders">我的订单</el-button>
        <el-button type="danger" link @click="handleLogout">退出</el-button>
      </div>
    </header>

    <main class="main-content">
      <el-alert
        v-if="seckillBuyDegraded"
        title="当前抢购人数过多，请稍后再试"
        description="系统已临时开启抢购降级，您可以继续浏览商品，暂无法提交新的抢购请求。"
        type="warning"
        show-icon
        :closable="false"
        class="seckill-degrade-alert"
      />

      <section class="hero-card">
        <div class="hero-copy">
          <span class="hero-tag">Static Seckill Page</span>
          <h2>秒杀商品独立展示，不进入购物车</h2>
          <p>
            这里展示进行中与 10 分钟内即将开始的秒杀商品。未开始的商品可预览倒计时，开场后才可抢购。
          </p>
        </div>
        <div class="hero-stats">
          <div class="stat-box">
            <span class="stat-label">秒杀商品数</span>
            <strong>{{ seckillProducts.length }}</strong>
          </div>
          <div class="stat-box">
            <span class="stat-label">当前请求</span>
            <strong>{{ currentRequest?.statusText || '未发起' }}</strong>
          </div>
        </div>
      </section>

      <section class="content-grid">
        <div class="left-column">
          <el-card class="panel-card" shadow="hover">
            <template #header>
              <div class="panel-header">
                <div>
                  <h3 class="panel-title">秒杀商品列表</h3>
                  <p class="panel-subtitle">列表优先读本地/Redis 缓存，手动刷新可拉取最新数据</p>
                </div>
                <div class="panel-summary">当前展示 {{ seckillProducts.length }} 件商品</div>
              </div>
            </template>

            <el-skeleton v-if="loading" :rows="6" animated />

            <el-empty v-else-if="seckillProducts.length === 0" description="暂无秒杀商品">
              <el-button type="primary" @click="refreshSeckillList">重新加载</el-button>
            </el-empty>

            <div v-else class="seckill-list">
              <el-card
                v-for="item in seckillProducts"
                :id="`seckill-card-${item.id}`"
                :key="item.id"
                class="seckill-card"
                :class="{ active: Number(item.id) === Number(selectedProductId) }"
                shadow="hover"
                :body-style="{ padding: '0px' }"
              >
                <div class="card-image">
                  <template v-if="item.image">
                    <img :src="item.image" :alt="item.name" />
                  </template>
                  <template v-else>
                    <span>秒杀商品</span>
                  </template>
                </div>

                <div class="card-body">
                  <div class="card-top">
                    <div class="card-meta">
                      <el-tag size="small" type="danger">秒杀</el-tag>
                      <el-tag size="small" :type="getCountdownTagType(item)" effect="plain">
                        {{ getCountdownLabel(item) }}
                      </el-tag>
                    </div>
                    <span class="stock-text">库存 {{ item.flashStock }}</span>
                  </div>

                  <h4 class="product-name">{{ item.name }}</h4>
                  <p class="product-desc" v-if="item.description">{{ item.description }}</p>

                  <div class="countdown-box" v-if="flashSaleCountdowns[item.id]">
                    <span class="countdown-label">
                      {{
                        flashSaleCountdowns[item.id].expired
                          ? '活动已结束'
                          : flashSaleCountdowns[item.id].type === 'start'
                            ? '距开始'
                            : '距结束'
                      }}
                    </span>
                    <div v-if="!flashSaleCountdowns[item.id].expired" class="countdown-digits">
                      <span class="countdown-digit">{{ flashSaleCountdowns[item.id].hours }}</span>
                      <span class="countdown-sep">:</span>
                      <span class="countdown-digit">{{ flashSaleCountdowns[item.id].minutes }}</span>
                      <span class="countdown-sep">:</span>
                      <span class="countdown-digit">{{ flashSaleCountdowns[item.id].seconds }}</span>
                    </div>
                  </div>

                  <div class="price-row">
                    <span class="flash-price">¥ {{ formatMoney(item.flashPrice) }}</span>
                    <span class="original-price">¥ {{ formatMoney(item.originalPrice) }}</span>
                  </div>

                  <div class="progress-row">
                    <el-progress
                      :percentage="item.progress"
                      status="exception"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="progress-text">已抢 {{ item.progress }}%</span>
                  </div>

                  <div class="action-row">
                    <el-button size="small" @click="selectProduct(item)">查看详情</el-button>
                    <el-button
                      type="danger"
                      size="small"
                      :loading="buyingProductId === item.id"
                      :disabled="!isBuyable(item) || buyingProductId !== null"
                      @click="handleBuy(item)"
                    >
                      {{ getBuyButtonText(item) }}
                    </el-button>
                  </div>
                </div>
              </el-card>
            </div>
          </el-card>
        </div>

        <aside class="right-column">
          <el-card class="panel-card sticky-card" shadow="hover">
            <template #header>
              <div class="panel-header">
                <div>
                  <h3 class="panel-title">当前商品详情</h3>
                  <p class="panel-subtitle">点击左侧商品后，会在这里显示详细信息</p>
                </div>
              </div>
            </template>

            <template v-if="selectedProduct">
              <div class="detail-thumb">
                <template v-if="selectedProduct.image">
                  <img :src="selectedProduct.image" :alt="selectedProduct.name" />
                </template>
                <template v-else>暂无图片</template>
              </div>

              <div class="detail-name">{{ selectedProduct.name }}</div>
              <div class="detail-desc" v-if="selectedProduct.description">
                {{ selectedProduct.description }}
              </div>

              <el-descriptions :column="1" border class="detail-desc-box">
                <el-descriptions-item label="秒杀价">
                  ¥ {{ formatMoney(selectedProduct.flashPrice) }}
                </el-descriptions-item>
                <el-descriptions-item label="原价">
                  ¥ {{ formatMoney(selectedProduct.originalPrice) }}
                </el-descriptions-item>
                <el-descriptions-item label="秒杀库存">
                  {{ selectedProduct.flashStock }}
                </el-descriptions-item>
                <el-descriptions-item label="活动时间">
                  {{ formatDateTime(selectedProduct.flashStartTime) }} - {{ formatDateTime(selectedProduct.flashEndTime) }}
                </el-descriptions-item>
              </el-descriptions>
            </template>

            <el-empty v-else description="请选择一个商品查看详情" />
          </el-card>

          <el-card class="panel-card sticky-card" shadow="hover">
            <template #header>
              <div class="panel-header">
                <div>
                  <h3 class="panel-title">抢购结果</h3>
                  <p class="panel-subtitle">提交后会显示排队状态，请手动点击「重新查询」获取最终结果</p>
                </div>
              </div>
            </template>

            <template v-if="currentRequest">
              <el-descriptions :column="1" border class="detail-desc-box">
                <el-descriptions-item label="请求编号">
                  {{ currentRequest.requestId }}
                </el-descriptions-item>
                <el-descriptions-item label="商品名称">
                  {{ currentRequest.productName }}
                </el-descriptions-item>
                <el-descriptions-item label="当前状态">
                  <el-tag :type="requestTagType(currentRequest.status)">{{ currentRequest.statusText }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="提示信息">
                  {{ currentRequest.message || '-' }}
                </el-descriptions-item>
                <el-descriptions-item v-if="currentRequest.orderId" label="订单编号">
                  {{ currentRequest.orderId }}
                </el-descriptions-item>
              </el-descriptions>

              <div class="request-actions">
                <el-button
                  v-if="currentRequest.orderId && currentRequest.status === 1"
                  type="primary"
                  @click="goSeckillPay(currentRequest.orderId)"
                >
                  去支付
                </el-button>
                <el-button @click="refreshCurrentRequest">重新查询</el-button>
                <el-button type="primary" plain @click="clearCurrentRequest">清空结果</el-button>
              </div>
            </template>

            <el-empty v-else description="还没有发起抢购请求" />
          </el-card>

          <el-card class="panel-card sticky-card" shadow="hover">
            <template #header>
              <div class="panel-header">
                <div>
                  <h3 class="panel-title">使用说明</h3>
                  <p class="panel-subtitle">当前页面只负责展示和发起秒杀请求</p>
                </div>
              </div>
            </template>

            <ul class="tips-list">
              <li>秒杀商品不进入购物车，抢购直接走秒杀接口。</li>
              <li>商品列表采用缓存展示，倒计时在前端本地计算，减少服务器压力。</li>
              <li>抢购提交后不会自动轮询，请点击「重新查询」查看结果。</li>
            </ul>
          </el-card>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { buySeckill, getSeckillBuyDegradeStatus, getSeckillList, getSeckillResult, getSeckillToken } from '../api/seckill'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const seckillBuyDegraded = ref(false)
const seckillProducts = ref([])
const selectedProductId = ref(null)
const currentRequest = ref(null)
const buyingProductId = ref(null)
const nowTick = ref(Date.now())
const SECKILL_LIST_CACHE_KEY = 'shop:seckill:list:cache'
const SECKILL_LIST_CACHE_TTL_MS = 30 * 1000
let countdownTimer = null

const buildTimeOffset = (offsetMs) => {
  const date = new Date(Date.now() + offsetMs)
  return date.toISOString().replace('T', ' ').slice(0, 19)
}

const fallbackSeckillProducts = [
  {
    id: 101,
    name: '限量版机械键盘',
    categoryId: 1,
    originalPrice: 459.0,
    flashPrice: 199.0,
    stock: 50,
    flashStock: 50,
    progress: 80,
    image: '',
    description: '青轴手感，限时抢购',
    flashStartTime: buildTimeOffset(-2 * 60 * 60 * 1000),
    flashEndTime: buildTimeOffset(2 * 60 * 60 * 1000)
  },
  {
    id: 102,
    name: '无线快充移动电源',
    categoryId: 1,
    originalPrice: 129.0,
    flashPrice: 59.0,
    stock: 200,
    flashStock: 120,
    progress: 40,
    image: '',
    description: '便携大容量，出行必备',
    flashStartTime: buildTimeOffset(-1 * 60 * 60 * 1000),
    flashEndTime: buildTimeOffset(3 * 60 * 60 * 1000)
  },
  {
    id: 103,
    name: '电竞鼠标',
    categoryId: 1,
    originalPrice: 199.0,
    flashPrice: 99.0,
    stock: 95,
    flashStock: 18,
    progress: 95,
    image: '',
    description: '高精度传感器',
    flashStartTime: buildTimeOffset(-3 * 60 * 60 * 1000),
    flashEndTime: buildTimeOffset(30 * 60 * 1000)
  }
]

const formatMoney = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '-'
  }
  return num.toFixed(2)
}

const parseDateTime = (value) => {
  if (!value) {
    return null
  }
  const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDateTime = (value) => {
  const date = parseDateTime(value)
  if (!date) {
    return '-'
  }
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const pad2 = (value) => String(value).padStart(2, '0')

const getCountdownParts = (endTime, startTime = null) => {
  const end = parseDateTime(endTime)
  const start = parseDateTime(startTime)
  const now = new Date()

  if (start && now < start) {
    const totalSeconds = Math.max(0, Math.floor((start.getTime() - now.getTime()) / 1000))
    return {
      type: 'start',
      expired: false,
      hours: pad2(Math.floor(totalSeconds / 3600)),
      minutes: pad2(Math.floor((totalSeconds % 3600) / 60)),
      seconds: pad2(totalSeconds % 60)
    }
  }

  if (!end) {
    return { type: 'none', expired: false, hours: '00', minutes: '00', seconds: '00' }
  }

  const totalSeconds = Math.floor((end.getTime() - now.getTime()) / 1000)
  if (totalSeconds <= 0) {
    return { type: 'end', expired: true, hours: '00', minutes: '00', seconds: '00' }
  }

  return {
    type: 'end',
    expired: false,
    hours: pad2(Math.floor(totalSeconds / 3600)),
    minutes: pad2(Math.floor((totalSeconds % 3600) / 60)),
    seconds: pad2(totalSeconds % 60)
  }
}

const normalizeProduct = (item) => {
  const flashStock = Number(item.flashStock ?? item.flash_stock ?? item.stock ?? 0)
  const progress = item.progress != null
    ? Number(item.progress)
    : (() => {
      const originalStock = Number(item.stock ?? item.flash_stock ?? flashStock)
      return originalStock > 0
        ? Math.max(0, Math.min(100, Math.round(((originalStock - flashStock) / originalStock) * 100)))
        : 0
    })()

  return {
    id: Number(item.id),
    name: item.name || '',
    categoryId: item.categoryId ?? item.category_id ?? null,
    originalPrice: item.originalPrice ?? item.original_price ?? item.price ?? null,
    flashPrice: item.flashPrice ?? item.flash_price ?? null,
    stock: Number(item.stock ?? 0),
    flashStock,
    progress,
    image: item.image || '',
    description: item.description || '',
    status: Number(item.status ?? 1),
    flashStartTime: item.flashStartTime ?? item.flash_start_time ?? null,
    flashEndTime: item.flashEndTime ?? item.flash_end_time ?? null
  }
}

const sortSeckillProducts = (list) => {
  return [...list].sort((a, b) => {
    const aParts = getCountdownParts(a.flashEndTime, a.flashStartTime)
    const bParts = getCountdownParts(b.flashEndTime, b.flashStartTime)
    const aUpcoming = aParts.type === 'start'
    const bUpcoming = bParts.type === 'start'
    if (aUpcoming !== bUpcoming) {
      return aUpcoming ? -1 : 1
    }
    const aTime = parseDateTime(aUpcoming ? a.flashStartTime : a.flashEndTime)
    const bTime = parseDateTime(bUpcoming ? b.flashStartTime : b.flashEndTime)
    return (aTime?.getTime() ?? 0) - (bTime?.getTime() ?? 0)
  })
}

const flashSaleCountdowns = computed(() => {
  nowTick.value
  return Object.fromEntries(
    seckillProducts.value.map(item => [
      item.id,
      getCountdownParts(item.flashEndTime, item.flashStartTime)
    ])
  )
})

const selectedProduct = computed(() => {
  if (selectedProductId.value == null) {
    return seckillProducts.value[0] || null
  }
  return seckillProducts.value.find(item => Number(item.id) === Number(selectedProductId.value)) || seckillProducts.value[0] || null
})

const loadSeckillProducts = async (force = false) => {
  if (!force) {
    const cachedList = readSeckillListCache()
    if (cachedList) {
      seckillProducts.value = sortSeckillProducts(cachedList)
      ensureSelectedProduct()
      return
    }
  }

  loading.value = true
  try {
    const res = await getSeckillList()
    if (res.code === 200 && Array.isArray(res.data)) {
      const list = sortSeckillProducts(res.data.map(normalizeProduct))
      seckillProducts.value = list
      writeSeckillListCache(list)
      ensureSelectedProduct()
      return
    }
  } catch {
    // ignore and use fallback
  } finally {
    loading.value = false
  }
  const fallbackList = sortSeckillProducts(fallbackSeckillProducts.map(normalizeProduct))
  seckillProducts.value = fallbackList
  writeSeckillListCache(fallbackList)
  ensureSelectedProduct()
}

const readSeckillListCache = () => {
  try {
    const raw = sessionStorage.getItem(SECKILL_LIST_CACHE_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    if (!parsed?.list || !Array.isArray(parsed.list)) {
      return null
    }
    if (!parsed.cachedAt || Date.now() - parsed.cachedAt > SECKILL_LIST_CACHE_TTL_MS) {
      sessionStorage.removeItem(SECKILL_LIST_CACHE_KEY)
      return null
    }
    return parsed.list.map(normalizeProduct)
  } catch {
    return null
  }
}

const writeSeckillListCache = (list) => {
  try {
    sessionStorage.setItem(
      SECKILL_LIST_CACHE_KEY,
      JSON.stringify({
        cachedAt: Date.now(),
        list
      })
    )
  } catch {
    // ignore quota errors
  }
}

const ensureSelectedProduct = () => {
  if (selectedProductId.value != null) {
    const exists = seckillProducts.value.some(item => Number(item.id) === Number(selectedProductId.value))
    if (!exists) {
      selectedProductId.value = seckillProducts.value[0]?.id ?? null
    }
    return
  }
  selectedProductId.value = seckillProducts.value[0]?.id ?? null
}

const refreshSeckillList = async () => {
  sessionStorage.removeItem(SECKILL_LIST_CACHE_KEY)
  await Promise.all([loadSeckillProducts(true), loadSeckillBuyDegradeStatus()])
}

const loadSeckillBuyDegradeStatus = async () => {
  if (!localStorage.getItem('token')) {
    seckillBuyDegraded.value = false
    return
  }
  try {
    const res = await getSeckillBuyDegradeStatus()
    if (res.code === 200 && res.data) {
      seckillBuyDegraded.value = Boolean(res.data.degraded)
    }
  } catch {
    // 401 等错误交给拦截器；未登录时不影响浏览
  }
}

const extractErrorMessage = (error, fallback = '网络异常，请稍后重试') => {
  return error?.response?.data?.message || error?.message || fallback
}

const getCountdownLabel = (product) => {
  const parts = flashSaleCountdowns.value[product.id]
  if (!parts) {
    return '秒杀'
  }
  if (parts.expired) {
    return '已结束'
  }
  return parts.type === 'start' ? '即将开始' : '进行中'
}

const getCountdownTagType = (product) => {
  const parts = flashSaleCountdowns.value[product.id]
  if (!parts || parts.expired) {
    return 'info'
  }
  return parts.type === 'start' ? 'warning' : 'success'
}

const getBuyButtonText = (product) => {
  if (seckillBuyDegraded.value) {
    return '人数过多，请稍后再试'
  }
  const parts = flashSaleCountdowns.value[product.id]
  if (!parts) {
    return '立即抢购'
  }
  if (parts.expired) {
    return '已结束'
  }
  if (parts.type === 'start') {
    return '即将开始'
  }
  return '立即抢购'
}

const isBuyable = (product) => {
  if (seckillBuyDegraded.value) {
    return false
  }
  const parts = flashSaleCountdowns.value[product.id]
  return Boolean(parts && !parts.expired && parts.type === 'end')
}

const requestTagType = (status) => {
  const value = Number(status)
  if (value === 1) return 'success'
  if (value === 2) return 'danger'
  return 'warning'
}

const selectProduct = async (product) => {
  selectedProductId.value = product.id
  await nextTick()
  document.getElementById(`seckill-card-${product.id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const handleBuy = async (product) => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (seckillBuyDegraded.value) {
    ElMessage.warning('当前抢购人数过多，请稍后再试')
    return
  }

  if (!isBuyable(product)) {
    ElMessage.warning('当前商品暂时无法抢购')
    return
  }

  try {
    await ElMessageBox.confirm(`确定抢购「${product.name}」吗？`, '提示', {
      confirmButtonText: '立即抢购',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  buyingProductId.value = product.id
  try {
    const tokenRes = await getSeckillToken(product.id)
    if (tokenRes.code !== 200 || !tokenRes.data?.token) {
      ElMessage.error(tokenRes.message || '获取秒杀令牌失败')
      return
    }

    const buyRes = await buySeckill(product.id, {
      token: tokenRes.data.token,
      quantity: 1
    })

    if (buyRes.code !== 200) {
      ElMessage.error(buyRes.message || '当前抢购人数过多，请稍后再试')
      return
    }

    const requestId = buyRes.data?.requestId || buyRes.data?.request_id
    if (requestId) {
      currentRequest.value = {
        requestId,
        productId: product.id,
        productName: product.name,
        status: 0,
        statusText: '排队中',
        message: '请求已提交，正在排队中',
        orderId: null
      }
      ElMessage.success('抢购请求已提交，请点击「重新查询」查看结果')
      await fetchRequestResult(requestId)
      return
    }

    ElMessage.error(buyRes.message || '提交秒杀请求失败')
  } catch (error) {
    const msg = extractErrorMessage(error, '')
    if (msg.includes('人数过多')) {
      seckillBuyDegraded.value = true
    }
  } finally {
    buyingProductId.value = null
  }
}

const refreshCurrentRequest = async () => {
  if (!currentRequest.value?.requestId) {
    return
  }
  await fetchRequestResult(currentRequest.value.requestId)
}

const clearCurrentRequest = () => {
  currentRequest.value = null
}

const normalizeResult = (data) => ({
  requestId: data.requestId || data.request_id || currentRequest.value?.requestId || '',
  status: Number(data.status ?? 0),
  statusText: data.statusText || data.status_text || '排队中',
  message: data.message || data.msg || '',
  orderId: data.orderId ?? data.order_id ?? null
})

const fetchRequestResult = async (requestId) => {
  try {
    const res = await getSeckillResult(requestId)
    if (res.code === 200 && res.data) {
      const result = normalizeResult(res.data)
      currentRequest.value = {
        ...(currentRequest.value || {}),
        ...result
      }

      if (result.status === 0) {
        return false
      }

      if (result.status === 1) {
        ElMessage.success(result.message || '抢购成功')
      } else {
        ElMessage.error(result.message || '抢购失败')
      }
      return true
    }
  } catch {
    // handled by interceptor
  }
  return false
}

const startCountdownTimer = () => {
  if (countdownTimer) {
    return
  }
  countdownTimer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
}

const stopCountdownTimer = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const goHome = () => {
  router.push('/home')
}

const goOrders = () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/orders')
}

const goSeckillPay = (orderId) => {
  if (!orderId) {
    return
  }
  router.push({ path: `/pay/${orderId}`, query: { orderType: 2 } })
}

const handleLogout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

const syncQuerySelection = async () => {
  const queryProductId = Number(route.query.productId || 0)
  if (Number.isFinite(queryProductId) && queryProductId > 0) {
    selectedProductId.value = queryProductId
    await nextTick()
    document.getElementById(`seckill-card-${queryProductId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

watch(
  () => route.query.productId,
  async () => {
    await syncQuerySelection()
  }
)

onMounted(async () => {
  startCountdownTimer()
  await loadSeckillProducts()
  await loadSeckillBuyDegradeStatus()
  await syncQuerySelection()
})

onBeforeUnmount(() => {
  stopCountdownTimer()
})
</script>

<style scoped>
.seckill-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.seckill-degrade-alert {
  margin: 16px 40px 0;
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
  position: sticky;
  top: 0;
  z-index: 100;
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
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 40px 40px;
  box-sizing: border-box;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: stretch;
  padding: 24px 28px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
  border: 1px solid #fde2e2;
  margin-bottom: 20px;
}

.hero-copy {
  flex: 1;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 12px;
  margin-bottom: 12px;
}

.hero-copy h2 {
  margin: 0 0 10px;
  font-size: 28px;
  color: #303133;
}

.hero-copy p {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
  max-width: 720px;
}

.hero-stats {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.stat-box {
  min-width: 170px;
  padding: 14px 18px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stat-label {
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}

.stat-box strong {
  color: #303133;
  font-size: 18px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
  gap: 20px;
  align-items: start;
}

.left-column,
.right-column {
  min-width: 0;
}

.panel-card {
  border-radius: 16px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-title {
  margin: 0 0 4px;
  font-size: 18px;
  color: #303133;
}

.panel-subtitle {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.panel-summary {
  color: #f56c6c;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.seckill-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.seckill-card {
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.seckill-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
}

.seckill-card.active {
  border-color: #f56c6c;
  box-shadow: 0 10px 24px rgba(245, 108, 108, 0.15);
}

.card-image {
  height: 180px;
  background: #fef0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f56c6c;
  font-size: 16px;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-body {
  padding: 14px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.card-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stock-text {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.product-name {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}

.product-desc {
  margin: 0 0 12px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
  min-height: 42px;
}

.countdown-box {
  padding: 12px;
  border-radius: 12px;
  background: #f8faff;
  border: 1px solid #ebeef5;
  margin-bottom: 12px;
}

.countdown-label {
  color: #606266;
  font-size: 13px;
  display: block;
  margin-bottom: 8px;
}

.countdown-digits {
  display: flex;
  align-items: center;
  gap: 8px;
}

.countdown-digit {
  min-width: 36px;
  text-align: center;
  padding: 6px 8px;
  border-radius: 8px;
  background: #303133;
  color: #fff;
  font-weight: 700;
}

.countdown-sep {
  color: #909399;
  font-weight: 700;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.flash-price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: 700;
}

.original-price {
  color: #909399;
  font-size: 12px;
  text-decoration: line-through;
}

.progress-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.action-row {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.sticky-card + .sticky-card {
  margin-top: 16px;
}

.detail-thumb {
  height: 180px;
  border-radius: 12px;
  background: #f8faff;
  border: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #909399;
}

.detail-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.detail-name {
  margin-top: 12px;
  color: #303133;
  font-size: 18px;
  font-weight: 700;
}

.detail-desc {
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.detail-desc-box {
  margin-top: 14px;
}

.request-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.8;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
  }

  .hero-stats {
    flex-wrap: wrap;
  }

  .seckill-list {
    grid-template-columns: 1fr;
  }
}
</style>
