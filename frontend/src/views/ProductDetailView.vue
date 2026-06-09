<template>
  <div class="detail-page">
    <header class="header">
      <el-button link type="primary" :icon="ArrowLeft" @click="goBack">返回</el-button>
      <div class="header-title">商品详情</div>
      <div class="header-actions">
        <el-button link type="primary" @click="goCart">购物车</el-button>
        <el-button link type="primary" @click="goOrders">我的订单</el-button>
      </div>
    </header>

    <main class="main-content" v-loading="loading">
      <el-empty v-if="!loading && !product" description="商品不存在或已下架">
        <el-button type="primary" @click="goBack">返回上一页</el-button>
      </el-empty>

      <template v-else-if="product">
        <section class="detail-hero">
          <div class="gallery-panel">
            <div class="main-image">
              <template v-if="product.image">
                <img :src="product.image" :alt="product.name" />
              </template>
              <template v-else>
                <span>暂无图片</span>
              </template>
            </div>
          </div>

          <div class="info-panel">
            <div class="tag-row">
              <el-tag size="small" type="info">{{ categoryName }}</el-tag>
              <el-tag v-if="isFlashSaleProduct" size="small" type="danger">秒杀商品</el-tag>
              <el-tag v-if="seckillPhase === 'active'" size="small" type="success">秒杀进行中</el-tag>
              <el-tag v-else-if="seckillPhase === 'upcoming'" size="small" type="warning">即将开始</el-tag>
            </div>

            <h1 class="product-title">{{ product.name }}</h1>

            <div class="price-block">
              <template v-if="seckillPhase === 'active'">
                <span class="price-main">¥ {{ formatMoney(product.flashPrice) }}</span>
                <span class="price-original">¥ {{ formatMoney(product.price) }}</span>
                <el-tag type="danger" effect="plain" class="price-tag">秒杀价</el-tag>
              </template>
              <template v-else>
                <span class="price-main">¥ {{ formatMoney(product.price) }}</span>
                <span v-if="isFlashSaleProduct && product.flashPrice" class="price-hint">
                  秒杀价 ¥ {{ formatMoney(product.flashPrice) }}
                </span>
              </template>
            </div>

            <div v-if="seckillCountdown" class="countdown-card">
              <span class="countdown-label">{{ seckillCountdown.label }}</span>
              <div v-if="!seckillCountdown.expired" class="countdown-digits">
                <span class="countdown-digit">{{ seckillCountdown.hours }}</span>
                <span class="countdown-sep">:</span>
                <span class="countdown-digit">{{ seckillCountdown.minutes }}</span>
                <span class="countdown-sep">:</span>
                <span class="countdown-digit">{{ seckillCountdown.seconds }}</span>
              </div>
              <span v-else class="countdown-expired">活动已结束</span>
            </div>

            <el-descriptions :column="1" border class="meta-desc">
              <el-descriptions-item label="库存">
                {{ displayStock }}
              </el-descriptions-item>
              <el-descriptions-item v-if="isFlashSaleProduct" label="活动时间">
                {{ formatDateTime(product.flashStartTime) }} - {{ formatDateTime(product.flashEndTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="商品描述">
                {{ product.description || '暂无描述' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="quantity-row">
              <span class="quantity-label">购买数量</span>
              <el-input-number v-model="quantity" :min="1" :max="maxQuantity" />
            </div>
          </div>
        </section>

        <section class="group-buy-section">
          <div class="section-header">
            <div>
              <h2 class="section-title">拼单专区</h2>
              <p class="section-subtitle">支持发起拼单或直接加入已有拼团</p>
            </div>
            <el-tag :type="groupBuyActivity ? 'success' : 'info'">
              {{ groupBuyActivity ? '拼团进行中' : '暂无拼团' }}
            </el-tag>
          </div>

          <div v-if="groupBuyLoading" class="group-buy-placeholder">
            <el-skeleton :rows="3" animated />
          </div>

          <el-empty
            v-else-if="!groupBuyActivity && groupBuyTeams.length === 0"
            description="当前商品暂无拼团活动"
          />

          <div v-else class="group-buy-placeholder">
            <div v-for="item in groupBuyTeamsDisplay" :key="item.id" class="team-row">
              <div class="team-user">
                <div class="team-avatar">{{ getLeaderAvatar(item) }}</div>
                <div>
                  <div class="team-name">{{ item.leaderNickname || '拼团用户' }}</div>
                  <div class="team-meta">
                    还差 {{ item.remainSize ?? 0 }} 人成团 · {{ item.statusText || '拼团中' }}
                  </div>
                </div>
              </div>
              <div class="team-countdown">
                <template v-if="item.displayRemainSeconds > 0">
                  拼单即将结束 {{ formatRemainTime(item.displayRemainSeconds) }}
                </template>
                <template v-else>
                  拼单即将结束
                </template>
              </div>
              <el-button type="danger" plain @click="handleJoinGroup(item)">直接拼成</el-button>
            </div>
          </div>
        </section>

        <section class="review-section">
          <div class="section-header">
            <div>
              <h2 class="section-title">用户评价</h2>
              <p class="section-subtitle">评价功能即将上线</p>
            </div>
          </div>

          <el-empty description="暂无评价，购买后可发表第一条评论" />
        </section>
      </template>
    </main>

    <footer v-if="product" class="action-bar">
      <div class="action-bar-inner">
        <div class="action-side">
          <el-button text @click="goHome">首页</el-button>
          <el-button text @click="handleAddToCart" :loading="cartLoading">加入购物车</el-button>
        </div>
        <div class="action-buttons">
          <el-button
            size="large"
            class="buy-normal-btn"
            :loading="buying && actionType === 'normal'"
            :disabled="!canBuyNormal"
            @click="handleBuyNormal"
          >
            {{ normalBuyText }}
          </el-button>
          <el-button
            v-if="seckillPhase === 'active'"
            type="danger"
            size="large"
            class="buy-seckill-btn"
            :loading="seckillBuying"
            :disabled="seckillBuyDegraded"
            @click="handleBuySeckill"
          >
            {{ seckillBuyDegraded ? '人数过多' : '立即抢购' }}
          </el-button>
          <el-button
            v-else-if="groupBuyActivity"
            type="danger"
            size="large"
            class="buy-group-btn"
            :loading="groupBuyJoining"
            @click="handleOpenGroupBuy"
          >
            发起拼单
          </el-button>
          <el-button
            v-else
            type="danger"
            size="large"
            class="buy-group-btn"
            disabled
          >
            暂无拼团
          </el-button>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductDetail } from '../api/product'
import { getCategoryList } from '../api/category'
import { buySeckill, getSeckillBuyDegradeStatus, getSeckillToken } from '../api/seckill'
import { getGroupBuyActivity, getGroupBuyTeams, joinGroupBuy, openGroupBuy } from '../api/groupBuy'
import { useDirectPurchase } from '../composables/useDirectPurchase'
import { useCartCount } from '../composables/useCartCount'

const route = useRoute()
const router = useRouter()
const { buying, buyNow, addProductToCart } = useDirectPurchase()
const { refreshCartCount } = useCartCount()

const loading = ref(false)
const cartLoading = ref(false)
const seckillBuying = ref(false)
const seckillBuyDegraded = ref(false)
const groupBuyLoading = ref(false)
const groupBuyJoining = ref(false)
const actionType = ref('')
const product = ref(null)
const groupBuyActivity = ref(null)
const groupBuyTeams = ref([])
const categoryOptions = ref([])
const quantity = ref(1)
const nowTick = ref(Date.now())
let countdownTimer = null

const productId = computed(() => Number(route.params.id))

const categoryMap = computed(() => {
  return new Map(categoryOptions.value.map(item => [Number(item.id), item.name]))
})

const categoryName = computed(() => {
  if (!product.value) {
    return '-'
  }
  return categoryMap.value.get(Number(product.value.categoryId)) || '未分类'
})

const isFlashSaleProduct = computed(() => Number(product.value?.isFlashSale) === 1)

const seckillPhase = computed(() => {
  if (!isFlashSaleProduct.value || !product.value) {
    return 'none'
  }
  const start = parseDateTime(product.value.flashStartTime)
  const end = parseDateTime(product.value.flashEndTime)
  const now = new Date(nowTick.value)
  if (!start || !end) {
    return 'none'
  }
  if (now < start) {
    return 'upcoming'
  }
  if (now > end) {
    return 'ended'
  }
  return 'active'
})

const seckillCountdown = computed(() => {
  if (!isFlashSaleProduct.value || !product.value || seckillPhase.value === 'none' || seckillPhase.value === 'ended') {
    return null
  }
  const target = seckillPhase.value === 'upcoming'
    ? parseDateTime(product.value.flashStartTime)
    : parseDateTime(product.value.flashEndTime)
  if (!target) {
    return null
  }
  const diff = target.getTime() - nowTick.value
  if (diff <= 0) {
    return {
      label: seckillPhase.value === 'upcoming' ? '距开始' : '距结束',
      expired: true
    }
  }
  const totalSeconds = Math.floor(diff / 1000)
  const hours = String(Math.floor(totalSeconds / 3600)).padStart(2, '0')
  const minutes = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return {
    label: seckillPhase.value === 'upcoming' ? '距秒杀开始' : '距秒杀结束',
    expired: false,
    hours,
    minutes,
    seconds
  }
})

const groupBuyTeamsDisplay = computed(() => {
  nowTick.value
  return groupBuyTeams.value.map(team => {
    const expireTime = parseDateTime(team.expireTime)
    const serverRemainSeconds = Number(team.remainSeconds ?? 0)
    const displayRemainSeconds = expireTime
      ? Math.max(Math.floor((expireTime.getTime() - nowTick.value) / 1000), 0)
      : Math.max(serverRemainSeconds, 0)

    return {
      ...team,
      displayRemainSeconds
    }
  })
})

const displayStock = computed(() => {
  if (!product.value) {
    return 0
  }
  if (seckillPhase.value === 'active') {
    return product.value.flashStock ?? product.value.stock ?? 0
  }
  return product.value.stock ?? 0
})

const maxQuantity = computed(() => {
  const stock = Number(displayStock.value)
  return stock > 0 ? Math.min(stock, 99) : 1
})

const canBuyNormal = computed(() => {
  if (!product.value || Number(product.value.status) !== 1) {
    return false
  }
  return Number(product.value.stock) > 0
})

const normalBuyText = computed(() => {
  if (!product.value || Number(product.value.status) !== 1) {
    return '商品已下架'
  }
  if (Number(product.value.stock) <= 0) {
    return '库存不足'
  }
  if (seckillPhase.value === 'active') {
    return '单独购买'
  }
  return '立即购买'
})

const formatMoney = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '-'
  }
  return num.toFixed(2)
}

const formatDateTime = (value) => {
  const date = parseDateTime(value)
  if (!date) {
    return '-'
  }
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const parseDateTime = (value) => {
  if (!value) {
    return null
  }
  const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

const normalizeProduct = (item) => ({
  id: Number(item.id),
  name: item.name || '',
  categoryId: Number(item.categoryId ?? item.category_id ?? 0),
  price: Number(item.price ?? 0),
  stock: Number(item.stock ?? 0),
  image: item.image || '',
  description: item.description || '',
  status: Number(item.status ?? 1),
  isFlashSale: Number(item.isFlashSale ?? item.is_flash_sale ?? 0),
  flashPrice: item.flashPrice ?? item.flash_price ?? null,
  flashStock: item.flashStock ?? item.flash_stock ?? null,
  flashStartTime: item.flashStartTime ?? item.flash_start_time ?? null,
  flashEndTime: item.flashEndTime ?? item.flash_end_time ?? null
})

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryOptions.value = res.data.map(item => ({
        id: Number(item.id),
        name: item.name || ''
      }))
    }
  } catch {
    // ignore
  }
}

const loadProduct = async () => {
  if (!Number.isFinite(productId.value) || productId.value <= 0) {
    product.value = null
    return
  }

  loading.value = true
  try {
    const res = await getProductDetail(productId.value)
    if (res.code === 200 && res.data) {
      product.value = normalizeProduct(res.data)
      if (quantity.value > maxQuantity.value) {
        quantity.value = maxQuantity.value
      }
      return
    }
    product.value = null
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
}

const loadGroupBuy = async () => {
  if (!Number.isFinite(productId.value) || productId.value <= 0) {
    groupBuyActivity.value = null
    groupBuyTeams.value = []
    return
  }

  groupBuyLoading.value = true
  try {
    const [activityRes, teamsRes] = await Promise.all([
      getGroupBuyActivity(productId.value),
      getGroupBuyTeams(productId.value)
    ])

    groupBuyActivity.value = activityRes.code === 200 ? activityRes.data : null
    groupBuyTeams.value = teamsRes.code === 200 && Array.isArray(teamsRes.data) ? teamsRes.data : []
  } catch {
    groupBuyActivity.value = null
    groupBuyTeams.value = []
  } finally {
    groupBuyLoading.value = false
  }
}

const loadSeckillDegradeStatus = async () => {
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
    // ignore
  }
}

const handleBuyNormal = async () => {
  if (!canBuyNormal.value) {
    ElMessage.warning('当前商品无法购买')
    return
  }
  actionType.value = 'normal'
  const success = await buyNow(product.value.id, quantity.value)
  if (success) {
    actionType.value = ''
  }
}

const handleAddToCart = async () => {
  if (!product.value || Number(product.value.status) !== 1) {
    ElMessage.warning('商品已下架')
    return
  }
  cartLoading.value = true
  try {
    const success = await addProductToCart(product.value.id, quantity.value, product.value.name)
    if (success) {
      await refreshCartCount()
    }
  } finally {
    cartLoading.value = false
  }
}

const handleBuySeckill = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (seckillBuyDegraded.value) {
    ElMessage.warning('当前抢购人数过多，请稍后再试')
    return
  }
  if (seckillPhase.value !== 'active') {
    ElMessage.warning('秒杀活动未开始或已结束')
    return
  }

  try {
    await ElMessageBox.confirm(`确定以秒杀价抢购「${product.value.name}」吗？`, '秒杀确认', {
      confirmButtonText: '立即抢购',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  seckillBuying.value = true
  try {
    const tokenRes = await getSeckillToken(product.value.id)
    if (tokenRes.code !== 200 || !tokenRes.data?.token) {
      ElMessage.error(tokenRes.message || '获取秒杀令牌失败')
      return
    }

    const buyRes = await buySeckill(product.value.id, {
      token: tokenRes.data.token,
      quantity: 1
    })
    if (buyRes.code !== 200) {
      ElMessage.error(buyRes.message || '抢购失败')
      return
    }

    ElMessage.success('抢购请求已提交，请前往秒杀页或订单列表查看结果')
    router.push({
      path: '/seckill',
      query: { productId: product.value.id }
    })
  } catch {
    // handled by interceptor
  } finally {
    seckillBuying.value = false
  }
}

const handleOpenGroupBuy = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!groupBuyActivity.value?.id) {
    ElMessage.warning('当前商品暂无拼团活动')
    return
  }

  groupBuyJoining.value = true
  try {
    const res = await openGroupBuy(groupBuyActivity.value.id)
    if (res.code !== 200 || !res.data?.orderId) {
      ElMessage.error(res.message || '发起拼单失败')
      return
    }
    ElMessage.success('拼单已创建，请完成支付')
    router.push({ path: `/pay/${res.data.orderId}`, query: { orderType: 3 } })
  } catch {
    // handled by interceptor
  } finally {
    groupBuyJoining.value = false
  }
}

const handleJoinGroup = async (team) => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!team?.id) {
    ElMessage.warning('拼团信息无效')
    return
  }

  groupBuyJoining.value = true
  try {
    const res = await joinGroupBuy(team.id)
    if (res.code !== 200 || !res.data?.orderId) {
      ElMessage.error(res.message || '参团失败')
      return
    }
    ElMessage.success('已加入拼团，请完成支付')
    router.push({ path: `/pay/${res.data.orderId}`, query: { orderType: 3 } })
  } catch {
    // handled by interceptor
  } finally {
    groupBuyJoining.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/home')
}

const goHome = () => router.push('/home')

const goCart = () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/cart')
}

const goOrders = () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/orders')
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

const getLeaderAvatar = (team) => {
  const nickname = String(team?.leaderNickname || '拼').trim()
  return nickname ? nickname.slice(0, 1) : '拼'
}

const formatRemainTime = (seconds) => {
  const total = Math.max(Number(seconds) || 0, 0)
  const hours = String(Math.floor(total / 3600)).padStart(2, '0')
  const minutes = String(Math.floor((total % 3600) / 60)).padStart(2, '0')
  const secs = String(total % 60).padStart(2, '0')
  return `${hours}:${minutes}:${secs}`
}

watch(
  () => route.params.id,
  async () => {
    quantity.value = 1
    await loadProduct()
    await loadGroupBuy()
    await loadSeckillDegradeStatus()
  }
)

onMounted(async () => {
  startCountdownTimer()
  await Promise.all([loadCategories(), loadProduct(), loadGroupBuy(), loadSeckillDegradeStatus()])
})

onBeforeUnmount(() => {
  stopCountdownTimer()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 96px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 70px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.main-content {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
}

.detail-hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.gallery-panel,
.info-panel {
  min-width: 0;
}

.main-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  background: #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.product-title {
  margin: 0 0 16px;
  font-size: 28px;
  line-height: 1.4;
  color: #303133;
}

.price-block {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.price-main {
  color: #f56c6c;
  font-size: 34px;
  font-weight: 700;
}

.price-original {
  color: #909399;
  text-decoration: line-through;
  font-size: 16px;
}

.price-hint {
  color: #f56c6c;
  font-size: 14px;
}

.price-tag {
  margin-left: 4px;
}

.countdown-card {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffecec 100%);
  border: 1px solid #fde2e2;
}

.countdown-label {
  display: block;
  font-size: 13px;
  color: #f56c6c;
  font-weight: 600;
  margin-bottom: 8px;
}

.countdown-digits {
  display: flex;
  align-items: center;
  gap: 4px;
}

.countdown-digit {
  background: #f56c6c;
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 700;
  min-width: 28px;
  text-align: center;
}

.countdown-sep {
  color: #f56c6c;
  font-weight: 700;
}

.countdown-expired {
  color: #909399;
  font-size: 14px;
}

.meta-desc {
  margin-bottom: 20px;
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.quantity-label {
  color: #606266;
  font-size: 14px;
}

.group-buy-section,
.review-section {
  margin-top: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.section-subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.group-buy-placeholder {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
}

.team-row.is-disabled {
  opacity: 0.72;
}

.team-user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.team-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #fde2e2;
  color: #f56c6c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.team-name {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.team-meta,
.team-countdown {
  font-size: 13px;
  color: #909399;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 120;
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid #ebeef5;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(8px);
}

.action-bar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.action-side {
  display: flex;
  gap: 8px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.buy-normal-btn,
.buy-seckill-btn,
.buy-group-btn {
  min-width: 140px;
}

.buy-normal-btn {
  border-color: #f56c6c;
  color: #f56c6c;
}

@media (max-width: 960px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .team-row {
    grid-template-columns: 1fr;
  }

  .action-bar-inner {
    flex-direction: column;
    align-items: stretch;
  }

  .action-buttons {
    width: 100%;
  }

  .buy-normal-btn,
  .buy-seckill-btn,
  .buy-group-btn {
    flex: 1;
  }
}
</style>
