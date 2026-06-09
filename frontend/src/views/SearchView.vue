<template>
  <div class="search-page">
    <header class="header">
      <el-button link type="primary" :icon="ArrowLeft" @click="goHome">返回首页</el-button>
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

    <main class="main-content" v-loading="loading">
      <div class="result-header">
        <h1 class="page-title">搜索结果</h1>
        <p class="result-summary">{{ resultSummaryText }}</p>
      </div>

      <el-empty v-if="!loading && products.length === 0" description="未找到相关商品" />

      <el-row v-else :gutter="20">
        <el-col
          :span="6"
          v-for="product in products"
          :key="product.id"
          class="product-col"
        >
          <el-card shadow="hover" class="product-card" :body-style="{ padding: '0px' }">
            <div class="image-placeholder clickable-image" @click="goToProductDetail(product.id)">
              <template v-if="product.image">
                <img class="cover-image" :src="product.image" :alt="product.name" />
              </template>
              <template v-else>图片占位</template>
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
                <el-button size="small" round :icon="ShoppingCart" @click.stop="handleAddToCart(product)">
                  加入购物车
                </el-button>
                <el-button type="danger" size="small" round :loading="buyingProductId === product.id" @click.stop="handleBuyNow(product)">
                  立即购买
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Search, ShoppingCart } from '@element-plus/icons-vue'
import { getPublicProductList } from '../api/product'
import { getCategoryList } from '../api/category'
import { useCartCount } from '../composables/useCartCount'
import { useDirectPurchase } from '../composables/useDirectPurchase'

const route = useRoute()
const router = useRouter()
const { cartCount, refreshCartCount: loadCartCount, clearCartCount } = useCartCount()
const { buyNow, addProductToCart } = useDirectPurchase()
const buyingProductId = ref(null)

const searchKeyword = ref('')
const loading = ref(false)
const products = ref([])
const categoryOptions = ref([])

const categoryMap = computed(() => {
  return new Map(categoryOptions.value.map(item => [Number(item.id), item.name]))
})

const queryKeyword = computed(() => String(route.query.keyword || '').trim())
const queryCategoryId = computed(() => {
  const id = Number(route.query.categoryId)
  return Number.isFinite(id) && id > 0 ? id : undefined
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

const resultSummaryText = computed(() => {
  const keyword = queryKeyword.value
  const categoryId = queryCategoryId.value
  const total = products.value.length

  if (keyword && categoryId) {
    return `关键词「${keyword}」+ 分类「${getCategoryName(categoryId)}」共 ${total} 件商品`
  }
  if (keyword) {
    return `关键词「${keyword}」共 ${total} 件商品`
  }
  if (categoryId) {
    return `分类「${getCategoryName(categoryId)}」共 ${total} 件商品`
  }
  return `共 ${total} 件商品`
})

const buildSearchParams = () => ({
  keyword: queryKeyword.value || undefined,
  categoryId: queryCategoryId.value,
  page: 1,
  size: 24
})

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryOptions.value = res.data.map(normalizeCategory)
    }
  } catch {
    // ignore
  }
}

const loadProducts = async () => {
  if (!queryKeyword.value && !queryCategoryId.value) {
    products.value = []
    return
  }

  loading.value = true
  try {
    const res = await getPublicProductList(buildSearchParams())
    if (res.code === 200 && Array.isArray(res.data)) {
      products.value = res.data.map(normalizeProduct)
      return
    }
    products.value = []
  } catch {
    products.value = []
  } finally {
    loading.value = false
  }
}

const syncFromRoute = () => {
  searchKeyword.value = queryKeyword.value
  loadProducts()
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.info('请输入搜索关键词')
    return
  }
  router.replace({
    path: '/search',
    query: {
      keyword,
      ...(queryCategoryId.value ? { categoryId: queryCategoryId.value } : {})
    }
  })
}

const goHome = () => {
  router.push('/home')
}

const goToProductDetail = (productId) => {
  router.push(`/product/${productId}`)
}

const handleAddToCart = async (product) => {
  const success = await addProductToCart(product.id, 1, product.name)
  if (success) {
    await loadCartCount()
  }
}

const handleBuyNow = async (product) => {
  buyingProductId.value = product.id
  try {
    await buyNow(product.id, 1)
  } finally {
    buyingProductId.value = null
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

const handleLogout = () => {
  localStorage.removeItem('token')
  clearCartCount()
  router.push('/login')
}

watch(
  () => [route.query.keyword, route.query.categoryId],
  () => {
    syncFromRoute()
  }
)

onMounted(async () => {
  if (!queryKeyword.value && !queryCategoryId.value) {
    ElMessage.info('请输入关键词进行搜索')
    router.replace('/home')
    return
  }
  await loadCategories()
  syncFromRoute()
  await loadCartCount()
})
</script>

<style scoped>
.search-page {
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
  gap: 16px;
}

.search-box {
  flex: 1;
  max-width: 560px;
}

.user-actions {
  display: flex;
  justify-content: flex-end;
  min-width: 80px;
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

.result-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 26px;
  color: #303133;
}

.result-summary {
  margin: 0;
  color: #909399;
  font-size: 14px;
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

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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
  margin: 0 0 10px;
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

.clickable-image {
  cursor: pointer;
}

.action-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
