<template>
  <div class="home-container">
    <header class="header">
      <div class="logo">🛒 网店商城</div>
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品、分类、描述..."
          size="large"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>
      <div class="user-actions">
        <el-button type="primary" link @click="goOrders">我的订单</el-button>
        <el-button type="danger" link @click="handleLogout">退出</el-button>
      </div>
    </header>

    <main class="main-content">
      <section class="activity-section">
        <el-carousel height="320px" type="card">
          <el-carousel-item v-for="item in activities" :key="item.id">
            <div class="carousel-item-content" :style="{ backgroundColor: item.color }">
              <h2>{{ item.title }}</h2>
              <p>{{ item.desc }}</p>
            </div>
          </el-carousel-item>
        </el-carousel>
      </section>

      <section class="filter-section">
        <div class="section-header">
          <h2 class="section-title">分类筛选</h2>
          <div class="section-hint">支持按分类与关键词筛选商品</div>
        </div>

        <div class="filter-toolbar">
          <div class="category-chips">
            <el-button
              size="small"
              round
              :type="selectedCategoryId === 0 ? 'primary' : 'default'"
              @click="selectCategory(0)"
            >
              全部
            </el-button>
            <el-button
              v-for="category in categoryOptions"
              :key="category.id"
              size="small"
              round
              :type="selectedCategoryId === category.id ? 'primary' : 'default'"
              @click="selectCategory(category.id)"
            >
              {{ category.name }}
            </el-button>
          </div>

          <div class="filter-summary">
            当前展示 <strong>{{ productData.length }}</strong> 件商品
          </div>
        </div>
      </section>

      <section class="flash-sale-section" v-loading="flashSaleLoading">
        <div class="section-header">
          <h2 class="section-title">⚡ 限时秒杀</h2>
          <div v-if="sectionCountdown" class="countdown">
            <span class="countdown-label">{{ sectionCountdown.expired ? '活动已结束' : '距最近结束' }}</span>
            <template v-if="!sectionCountdown.expired">
              <span class="countdown-digit">{{ sectionCountdown.hours }}</span>
              <span class="countdown-sep">:</span>
              <span class="countdown-digit">{{ sectionCountdown.minutes }}</span>
              <span class="countdown-sep">:</span>
              <span class="countdown-digit">{{ sectionCountdown.seconds }}</span>
            </template>
          </div>
        </div>

        <el-empty
          v-if="!flashSaleLoading && flashSaleProducts.length === 0"
          description="暂无秒杀商品"
        />
        <el-row v-else :gutter="20">
          <el-col
            :span="6"
            v-for="item in flashSaleProducts"
            :key="item.id"
            class="product-col"
          >
            <el-card shadow="hover" class="flash-card" :body-style="{ padding: '0px' }">
              <div class="image-placeholder flash-image">
                <template v-if="item.image">
                  <img class="cover-image" :src="item.image" :alt="item.name" />
                </template>
                <template v-else>
                  秒杀商品
                </template>
              </div>
              <div class="product-info">
                <div class="product-meta">
                  <el-tag size="small" type="danger">秒杀</el-tag>
                  <span class="product-stock">库存 {{ item.stock }}</span>
                </div>
                <h4 class="product-name">{{ item.name }}</h4>
                <p class="product-desc" v-if="item.description">{{ item.description }}</p>
                <div
                  v-if="flashSaleCountdowns[item.id]?.type !== 'none'"
                  class="flash-countdown"
                >
                  <span class="flash-countdown-label">
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
                <el-button type="danger" class="flash-btn" size="small" round @click="handleFlashSale(item)">
                  立即抢购
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </section>

      <section ref="productSectionRef" class="product-section" v-loading="productLoading">
        <div class="section-header">
          <h2 class="section-title">热门推荐</h2>
          <div class="section-hint">{{ productSummaryText }}</div>
        </div>

        <el-empty v-if="!productLoading && productData.length === 0" description="暂无商品数据" />

        <el-row v-else :gutter="20">
          <el-col
            :span="6"
            v-for="product in productData"
            :key="product.id"
            class="product-col"
          >
            <el-card shadow="hover" class="product-card" :body-style="{ padding: '0px' }">
              <div class="image-placeholder">
                <template v-if="product.image">
                  <img class="cover-image" :src="product.image" :alt="product.name" />
                </template>
                <template v-else>
                  图片占位
                </template>
              </div>
              <div class="product-info">
                <div class="product-meta">
                  <el-tag size="small" type="info">{{ getCategoryName(product.categoryId) }}</el-tag>
                  <el-tag size="small" :type="Number(product.isFlashSale) === 1 ? 'danger' : 'success'">
                    {{ Number(product.isFlashSale) === 1 ? '秒杀商品' : '普通商品' }}
                  </el-tag>
                </div>
                <h4 class="product-name">{{ product.name }}</h4>
                <p class="product-desc" v-if="product.description">{{ product.description }}</p>
                <div class="price-row">
                  <span class="product-price">¥ {{ formatMoney(product.price) }}</span>
                  <span class="stock-text">库存 {{ product.stock }}</span>
                </div>
                <div class="action-row">
                  <el-button type="primary" size="small" round :icon="ShoppingCart" @click="handleAddToCart(product)">
                    加入购物车
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </section>
    </main>
    <el-badge
      class="cart-float"
      :value="cartCount"
      :hidden="cartCount === 0"
      :max="99"
      type="danger"
    >
      <el-button class="cart-float-button" circle :icon="ShoppingCart" @click="goCart" />
    </el-badge>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart } from '@element-plus/icons-vue'
import { addToCart } from '../api/cart'
import { getPublicFlashSaleList, getPublicProductList } from '../api/product'
import { getCategoryList } from '../api/category'
import { useCartCount } from '../composables/useCartCount'

const router = useRouter()
const { cartCount, refreshCartCount: loadCartCount, clearCartCount } = useCartCount()
const searchKeyword = ref('')
const selectedCategoryId = ref(0)
const productLoading = ref(false)
const flashSaleLoading = ref(false)
const productSectionRef = ref(null)
const nowTick = ref(Date.now())
let countdownTimer = null

const activities = ref([
  { id: 1, title: '夏季大促', desc: '全场满 300 减 50', color: '#ff9a9e' },
  { id: 2, title: '新品上市', desc: '数码 3C 抢先体验', color: '#a18cd1' },
  { id: 3, title: '会员专享', desc: '积分双倍抵扣', color: '#84fab0' }
])

const fallbackCategories = [
  { id: 1, name: '数码电器', sort: 1, status: 1 },
  { id: 2, name: '家居生活', sort: 2, status: 1 },
  { id: 3, name: '美妆护肤', sort: 3, status: 1 }
]

const fallbackProducts = [
  {
    id: 1,
    name: '高性能游戏本',
    categoryId: 1,
    price: 6999,
    stock: 120,
    image: '',
    description: '轻薄游戏本，适合高性能办公与游戏',
    status: 1,
    isFlashSale: 0,
    flashPrice: null,
    flashStock: null
  },
  {
    id: 2,
    name: '降噪蓝牙耳机',
    categoryId: 1,
    price: 899,
    stock: 350,
    image: '',
    description: '主动降噪，通勤必备',
    status: 1,
    isFlashSale: 1,
    flashPrice: 199,
    flashStock: 50
  },
  {
    id: 3,
    name: '智能手表',
    categoryId: 1,
    price: 1099,
    stock: 80,
    image: '',
    description: '支持运动健康监测',
    status: 1,
    isFlashSale: 0,
    flashPrice: null,
    flashStock: null
  },
  {
    id: 4,
    name: '人体工学椅',
    categoryId: 2,
    price: 888,
    stock: 45,
    image: '',
    description: '久坐办公更舒适',
    status: 1,
    isFlashSale: 0,
    flashPrice: null,
    flashStock: null
  }
]

const fallbackFlashSaleProducts = [
  {
    id: 101,
    name: '限量版机械键盘',
    categoryId: 1,
    originalPrice: 459.0,
    flashPrice: 199.0,
    stock: 50,
    progress: 80,
    image: '',
    description: '青轴手感，限时抢购',
    flashStartTime: '2026-05-25T10:00:00',
    flashEndTime: '2026-05-25T18:00:00'
  },
  {
    id: 102,
    name: '无线快充移动电源',
    categoryId: 1,
    originalPrice: 129.0,
    flashPrice: 59.0,
    stock: 200,
    progress: 50,
    image: '',
    description: '便携大容量，出行必备',
    flashStartTime: '2026-05-25T09:00:00',
    flashEndTime: '2026-05-25T20:00:00'
  },
  {
    id: 103,
    name: '电竞鼠标',
    categoryId: 1,
    originalPrice: 199.0,
    flashPrice: 99.0,
    stock: 95,
    progress: 95,
    image: '',
    description: '高精度传感器',
    flashStartTime: '2026-05-25T08:00:00',
    flashEndTime: '2026-05-25T22:00:00'
  },
  {
    id: 104,
    name: '高清投影仪',
    categoryId: 1,
    originalPrice: 1599.0,
    flashPrice: 899.0,
    stock: 30,
    progress: 30,
    image: '',
    description: '居家观影体验升级',
    flashStartTime: '2026-05-25T12:00:00',
    flashEndTime: '2026-05-25T23:59:59'
  }
]

const categoryOptions = ref([])
const productData = ref([])
const flashSaleProducts = ref([])

const categoryMap = computed(() => {
  return new Map(categoryOptions.value.map(item => [Number(item.id), item.name]))
})

const formatMoney = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '-'
  }
  return num.toFixed(2)
}

const getCategoryName = (categoryId) => {
  if (!categoryId && categoryId !== 0) {
    return '-'
  }
  return categoryMap.value.get(Number(categoryId)) || '-'
}

const parseDateTime = (value) => {
  if (!value) {
    return null
  }
  const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

const pad2 = (value) => String(value).padStart(2, '0')

const getCountdownParts = (endTime, startTime = null) => {
  const end = parseDateTime(endTime)
  const start = parseDateTime(startTime)
  const now = new Date()

  if (start && now < start) {
    const totalSeconds = Math.floor((start.getTime() - now.getTime()) / 1000)
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

const flashSaleCountdowns = computed(() => {
  nowTick.value
  return Object.fromEntries(
    flashSaleProducts.value.map(item => [
      item.id,
      getCountdownParts(item.flashEndTime, item.flashStartTime)
    ])
  )
})

const sectionCountdown = computed(() => {
  nowTick.value
  const endTimes = flashSaleProducts.value
    .map(item => parseDateTime(item.flashEndTime))
    .filter(Boolean)
    .sort((a, b) => a.getTime() - b.getTime())

  if (!endTimes.length) {
    return null
  }

  return getCountdownParts(endTimes[0])
})

const normalizeCategory = (item) => ({
  id: Number(item.id),
  name: item.name || '',
  sort: Number(item.sort ?? 0),
  status: Number(item.status ?? 1)
})

const normalizeProduct = (item) => ({
  id: Number(item.id),
  name: item.name || '',
  categoryId: item.categoryId ?? item.category_id ?? null,
  price: item.price ?? null,
  stock: Number(item.stock ?? 0),
  image: item.image || '',
  description: item.description || '',
  status: Number(item.status ?? 1),
  isFlashSale: Number(item.isFlashSale ?? item.is_flash_sale ?? 0),
  flashPrice: item.flashPrice ?? item.flash_price ?? null,
  flashStock: item.flashStock ?? item.flash_stock ?? null
})

const normalizeFlashSaleProduct = (item) => {
  const stock = Number(item.flashStock ?? item.flash_stock ?? item.stock ?? 0)
  const originalStock = Number(item.stock ?? item.flash_stock ?? stock)
  const progress = originalStock > 0 ? Math.max(0, Math.min(100, Math.round(((originalStock - stock) / originalStock) * 100))) : 0
  const flashStartTime = item.flashStartTime ?? item.flash_start_time ?? null
  const flashEndTime = item.flashEndTime ?? item.flash_end_time ?? null

  return {
    id: Number(item.id),
    name: item.name || '',
    categoryId: item.categoryId ?? item.category_id ?? null,
    originalPrice: item.price ?? item.originalPrice ?? 0,
    flashPrice: item.flashPrice ?? item.flash_price ?? item.price ?? 0,
    stock,
    progress,
    image: item.image || '',
    description: item.description || '',
    flashStartTime,
    flashEndTime
  }
}

const buildProductQueryParams = () => ({
  categoryId: selectedCategoryId.value === 0 ? undefined : selectedCategoryId.value
})

const productSummaryText = computed(() => {
  const total = productData.value.length
  if (selectedCategoryId.value !== 0) {
    return `当前分类「${getCategoryName(selectedCategoryId.value)}」共 ${total} 件商品`
  }
  return `当前共 ${total} 件上架商品`
})

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryOptions.value = res.data.map(normalizeCategory)
      return
    }
  } catch {
    // ignore and use fallback
  }
  categoryOptions.value = fallbackCategories.map(normalizeCategory)
}

const loadProducts = async () => {
  productLoading.value = true
  const params = buildProductQueryParams()
  try {
    const res = await getPublicProductList(params)
    if (res.code === 200 && Array.isArray(res.data)) {
      productData.value = res.data.map(normalizeProduct)
      return
    }
  } catch {
    // ignore and use fallback
  } finally {
    productLoading.value = false
  }
  productData.value = fallbackProducts.map(normalizeProduct)
}

const loadFlashSaleProducts = async () => {
  flashSaleLoading.value = true
  const params = buildProductQueryParams()
  try {
    const res = await getPublicFlashSaleList(params)
    if (res.code === 200 && Array.isArray(res.data)) {
      flashSaleProducts.value = res.data.map(normalizeFlashSaleProduct)
      return
    }
  } catch {
    // ignore and use fallback
  } finally {
    flashSaleLoading.value = false
  }
  flashSaleProducts.value = fallbackFlashSaleProducts.map(normalizeFlashSaleProduct)
}

const refreshProductLists = async () => {
  await Promise.all([loadProducts(), loadFlashSaleProducts()])
}

const selectCategory = async (categoryId) => {
  selectedCategoryId.value = Number(categoryId)
  await refreshProductLists()
  nextTick(() => {
    productSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.info('请输入搜索关键词')
    return
  }
  router.push({
    path: '/search',
    query: {
      keyword,
      ...(selectedCategoryId.value > 0 ? { categoryId: selectedCategoryId.value } : {})
    }
  })
}

const handleAddToCart = async (product) => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    const res = await addToCart({
      productId: product.id,
      quantity: 1
    })
    if (res.code === 200) {
      ElMessage.success(`「${product.name}」已加入购物车`)
      await loadCartCount()
    }
  } catch {
    // request 拦截器会给出错误提示
  }
}

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

const handleFlashSale = (product) => {
  router.push({
    path: '/seckill',
    query: {
      productId: product.id
    }
  })
}

const handleLogout = () => {
  localStorage.removeItem('token')
  clearCartCount()
  router.push('/login')
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

onMounted(async () => {
  startCountdownTimer()
  await Promise.all([loadCategories(), refreshProductLists()])
  await loadCartCount()
})

onBeforeUnmount(() => {
  stopCountdownTimer()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 70px;
  background-color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  width: 200px;
}

.search-box {
  flex: 1;
  max-width: 560px;
  margin: 0 20px;
}

.user-actions {
  width: 200px;
  display: flex;
  justify-content: flex-end;
  gap: 15px;
}

.cart-float {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 1000;
}

.cart-float-button {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(245, 108, 108, 0.28);
  font-size: 24px;
  animation: cartPulse 2.4s ease-in-out infinite;
}

.cart-float :deep(.el-badge__content) {
  min-width: 20px;
  height: 20px;
  line-height: 20px;
  padding: 0 5px;
  border: none;
  border-radius: 10px;
  font-size: 12px;
}

@keyframes cartPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.06);
  }
}

.main-content {
  flex: 1;
  padding: 24px 40px;
  max-width: 1240px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.activity-section,
.filter-section,
.flash-sale-section,
.product-section {
  margin-bottom: 40px;
}

.carousel-item-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  border-radius: 8px;
  text-shadow: 1px 1px 4px rgba(0, 0, 0, 0.2);
}

.carousel-item-content h2 {
  font-size: 32px;
  margin-bottom: 10px;
}

.carousel-item-content p {
  font-size: 18px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 24px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.countdown-label {
  color: #909399;
  font-size: 13px;
}

.countdown-digit {
  background-color: #f56c6c;
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 700;
  min-width: 28px;
  text-align: center;
  display: inline-block;
}

.countdown-sep {
  color: #f56c6c;
  font-weight: 700;
  padding: 0 2px;
}

.flash-countdown {
  margin: 0 0 10px;
  padding: 10px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffecec 100%);
  border-radius: 8px;
  border: 1px solid #fde2e2;
}

.flash-countdown-label {
  display: block;
  font-size: 12px;
  color: #f56c6c;
  font-weight: 600;
  margin-bottom: 6px;
}

.countdown-digits {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 18px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.category-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-summary {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.flash-card {
  border: 1px solid #fde2e2;
}

.flash-image {
  background-color: #fef0f0 !important;
  color: #f56c6c !important;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.flash-price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}

.original-price {
  color: #909399;
  font-size: 12px;
  text-decoration: line-through;
  margin-left: 8px;
}

.progress-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.flash-btn {
  width: 100%;
  margin-top: 12px;
}

.product-col {
  margin-bottom: 20px;
}

.product-card {
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s;
  height: 100%;
}

.product-card:hover {
  transform: translateY(-5px);
}

.image-placeholder {
  height: 200px;
  background-color: #e4e7ed;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #909399;
  font-size: 16px;
  overflow: hidden;
}

.product-info {
  padding: 14px;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.product-name {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  margin: 0 0 10px;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  min-height: 40px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.product-price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}

.stock-text {
  color: #909399;
  font-size: 12px;
}

.action-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
