<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo-area">
        <h2>后台管理系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        @select="handleMenuSelect"
      >
        <el-menu-item index="overview">
          <el-icon><DataLine /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="categories">
          <span>商品分类</span>
        </el-menu-item>
        <el-menu-item index="flashSale">
          <el-icon><Lightning /></el-icon>
          <span>秒杀管理</span>
        </el-menu-item>
        <el-menu-item index="groupBuy">
          <span>拼团管理</span>
        </el-menu-item>
        <el-menu-item index="orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="deadLetter">
          <el-icon><Bell /></el-icon>
          <span class="dead-letter-menu-label">
            异常消息
            <el-badge
              v-if="deadLetterPendingCount > 0"
              :value="deadLetterPendingCount"
              :max="99"
              class="menu-badge"
            />
          </span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="breadcrumb">{{ breadcrumbText }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              管理员 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <div v-if="activeMenu === 'overview'" class="panel">
          <h3 class="panel-title">近期收入汇总</h3>
          <div ref="chartRef" class="chart-container"></div>
        </div>

        <div v-if="activeMenu === 'products'" class="panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">商品列表</h3>
              <p class="panel-subtitle">支持新增、编辑、上下架与删除</p>
            </div>
            <div class="panel-actions">
              <el-input
                v-model="productSearchKeyword"
                placeholder="搜索商品名称 / 描述"
                clearable
                style="width: 260px"
              />
              <el-button :loading="productLoading" @click="loadProducts">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openAddDialog">上架新商品</el-button>
            </div>
          </div>

          <el-table
            :data="productData"
            border
            style="width: 100%"
            v-loading="productLoading"
            element-loading-text="商品数据加载中..."
            :empty-text="productLoading ? '正在加载商品数据...' : '暂无商品数据'"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="商品名称" min-width="160">
              <template #default="{ row }">
                <div class="product-name-cell">
                  <span class="product-name-text">{{ row.name }}</span>
                  <span class="product-desc" v-if="row.description">{{ row.description }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="120">
              <template #default="{ row }">
                {{ getCategoryName(row.categoryId) }}
              </template>
            </el-table-column>
            <el-table-column label="价格" width="120">
              <template #default="{ row }">
                ¥ {{ formatMoney(row.price) }}
              </template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="90" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
                  {{ Number(row.status) === 1 ? '上架' : '下架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="秒杀" width="100">
              <template #default="{ row }">
                <el-tag :type="Number(row.isFlashSale) === 1 ? 'danger' : 'warning'">
                  {{ Number(row.isFlashSale) === 1 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="260" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
                <el-button
                  size="small"
                  :type="Number(row.status) === 1 ? 'warning' : 'success'"
                  @click="toggleShelf(row)"
                >
                  {{ Number(row.status) === 1 ? '下架' : '上架' }}
                </el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="productCurrentPage"
              v-model:page-size="productPageSize"
              :page-sizes="[5, 10, 20]"
              :total="productTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
            />
          </div>
        </div>

        <div v-if="activeMenu === 'categories'" class="panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">商品分类</h3>
              <p class="panel-subtitle">支持新增、编辑与删除分类</p>
            </div>
            <div class="panel-actions">
              <el-button :loading="categoryLoading" @click="loadAdminCategories">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openAddCategoryDialog">新增分类</el-button>
            </div>
          </div>

          <el-table
            :data="categoryData"
            border
            style="width: 100%"
            v-loading="categoryLoading"
            element-loading-text="分类数据加载中..."
            :empty-text="categoryLoading ? '正在加载分类数据...' : '暂无分类数据'"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="分类名称" min-width="180" />
            <el-table-column prop="sort" label="排序" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
                  {{ Number(row.status) === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="180" />
            <el-table-column prop="updateTime" label="更新时间" min-width="180" />
            <el-table-column label="操作" min-width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditCategoryDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteCategory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="activeMenu === 'flashSale'" class="panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">秒杀活动列表</h3>
              <p class="panel-subtitle">直接从商品列表中筛选秒杀商品</p>
            </div>
            <div class="panel-actions">
              <el-tag :type="seckillBuyDegraded ? 'danger' : 'success'">
                抢购状态：{{ seckillBuyDegraded ? '已降级' : '正常' }}
              </el-tag>
              <el-button
                v-if="!seckillBuyDegraded"
                type="warning"
                :loading="seckillDegradeLoading"
                @click="handleEnableSeckillDegrade"
              >
                开启抢购降级
              </el-button>
              <el-button
                v-else
                type="success"
                :loading="seckillDegradeLoading"
                @click="handleDisableSeckillDegrade"
              >
                关闭抢购降级
              </el-button>
              <el-button
                :loading="seckillListCacheLoading"
                @click="handleRefreshSeckillListCache"
              >
                刷新前台列表缓存
              </el-button>
              <el-input
                v-model="flashSaleSearchKeyword"
                placeholder="搜索商品名称 / 分类"
                clearable
                style="width: 260px"
              />
              <el-button :loading="flashSaleLoading" @click="loadFlashSaleProducts">刷新</el-button>
              <el-button type="danger" :icon="Plus" @click="openAddFlashSaleDialog">添加秒杀商品</el-button>
            </div>
          </div>

          <el-alert
            v-if="seckillBuyDegraded"
            title="当前已开启抢购降级：用户无法发起新的秒杀抢购，但秒杀列表仍可浏览。"
            type="warning"
            show-icon
            :closable="false"
            class="seckill-degrade-alert"
          />

          <el-table
            :data="flashSaleData"
            border
            style="width: 100%"
            v-loading="flashSaleLoading"
            element-loading-text="秒杀数据加载中..."
            :empty-text="flashSaleLoading ? '正在加载秒杀数据...' : '暂无符合条件的秒杀商品'"
          >
            <el-table-column prop="id" label="活动ID" width="90" />
            <el-table-column prop="productName" label="商品名称" min-width="160" />
            <el-table-column prop="originalPrice" label="原价" width="120" />
            <el-table-column prop="flashPrice" label="秒杀价" width="120" />
            <el-table-column prop="stock" label="秒杀库存" width="100" />
            <el-table-column label="活动时间" min-width="200">
              <template #default="{ row }">
                <div
                  v-if="flashSaleCountdownById[row.id]?.type !== 'none'"
                  class="admin-flash-countdown"
                >
                  <span class="admin-countdown-label">{{ getAdminCountdownLabel(row) }}</span>
                  <div v-if="!flashSaleCountdownById[row.id].expired" class="countdown-digits">
                    <span class="countdown-digit">{{ flashSaleCountdownById[row.id].hours }}</span>
                    <span class="countdown-sep">:</span>
                    <span class="countdown-digit">{{ flashSaleCountdownById[row.id].minutes }}</span>
                    <span class="countdown-sep">:</span>
                    <span class="countdown-digit">{{ flashSaleCountdownById[row.id].seconds }}</span>
                  </div>
                </div>
                <span v-else class="countdown-empty">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getFlashSaleStatusType(getFlashSaleStatus(row.source))">
                  {{ getFlashSaleStatus(row.source) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" @click="openEditDialog(row.source)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="endFlashSale(row.source)">结束</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="flashSaleCurrentPage"
              v-model:page-size="flashSalePageSize"
              :page-sizes="[5, 10, 20]"
              :total="flashSaleTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
            />
          </div>
        </div>

        <div v-if="activeMenu === 'groupBuy'" class="panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">拼团活动列表</h3>
              <p class="panel-subtitle">支持新增、编辑、上架与下架拼团活动</p>
            </div>
            <div class="panel-actions">
              <el-input
                v-model="groupBuySearchKeyword"
                placeholder="搜索商品名称 / 分类"
                clearable
                style="width: 240px"
                @keyup.enter="loadGroupBuys"
              />
              <el-input
                v-model="groupBuyProductIdFilter"
                placeholder="商品ID"
                clearable
                style="width: 130px"
                @keyup.enter="loadGroupBuys"
              />
              <el-select
                v-model="groupBuyStatusFilter"
                placeholder="状态"
                clearable
                style="width: 140px"
                @change="loadGroupBuys"
              >
                <el-option
                  v-for="item in groupBuyStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button :loading="groupBuyLoading" @click="refreshGroupBuys">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openAddGroupBuyDialog">新增拼团活动</el-button>
            </div>
          </div>

          <el-alert
            title="拼团活动默认按商品维度配置，商品必须处于上架状态且不能是秒杀商品。"
            type="info"
            show-icon
            :closable="false"
            class="group-buy-alert"
          />

          <el-table
            :data="groupBuyData"
            border
            style="width: 100%"
            v-loading="groupBuyLoading"
            element-loading-text="拼团活动加载中..."
            :empty-text="groupBuyLoading ? '正在加载拼团活动...' : '暂无拼团活动'"
          >
            <el-table-column prop="id" label="活动ID" width="90" />
            <el-table-column label="商品信息" min-width="220">
              <template #default="{ row }">
                <div class="group-buy-product-cell">
                  <div class="group-buy-product-name">{{ row.productName || `商品 #${row.productId}` }}</div>
                  <div class="group-buy-product-subtitle">
                    {{ row.categoryName || '-' }} · 商品ID {{ row.productId }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="价格" min-width="180">
              <template #default="{ row }">
                <div class="group-buy-price-cell">
                  <span class="group-buy-price-current">拼团价 ¥{{ formatMoney(row.groupPrice) }}</span>
                  <span class="group-buy-price-original">原价 ¥{{ formatMoney(row.productPrice) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="requiredSize" label="成团人数" width="100" />
            <el-table-column prop="durationHours" label="时长(小时)" width="110" />
            <el-table-column prop="stock" label="库存" width="90" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
                  {{ row.statusText || (Number(row.status) === 1 ? '进行中' : '已下架') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="活动时间" min-width="200">
              <template #default="{ row }">
                <div class="group-buy-meta">
                  <div>开始：{{ formatDateTime(row.startTime) }}</div>
                  <div>结束：{{ formatDateTime(row.endTime) }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="190" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditGroupBuyDialog(row)">编辑</el-button>
                <el-button
                  size="small"
                  :type="Number(row.status) === 1 ? 'warning' : 'success'"
                  @click="toggleGroupBuyStatus(row)"
                >
                  {{ Number(row.status) === 1 ? '下架' : '上架' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="groupBuyCurrentPage"
              v-model:page-size="groupBuyPageSize"
              :page-sizes="[5, 10, 20]"
              :total="groupBuyTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
            />
          </div>
        </div>

        <div v-if="activeMenu === 'orders'" class="panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">订单管理</h3>
              <p class="panel-subtitle">支持搜索、查看详情、修改状态与取消订单</p>
            </div>
            <div class="panel-actions">
              <el-input
                v-model="orderSearchKeyword"
                placeholder="搜索订单号"
                clearable
                style="width: 240px"
                @keyup.enter="loadOrders"
              />
              <el-select
                v-model="orderStatusFilter"
                placeholder="订单状态"
                clearable
                style="width: 150px"
                @change="loadOrders"
              >
                <el-option
                  v-for="item in orderStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button :loading="orderLoading" @click="refreshOrders">刷新</el-button>
            </div>
          </div>

          <el-table
            :data="orderData"
            border
            style="width: 100%"
            v-loading="orderLoading"
            element-loading-text="订单数据加载中..."
            :empty-text="orderLoading ? '正在加载订单数据...' : '暂无订单数据'"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="orderNo" label="订单号" min-width="200" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getOrderTypeTagType(row.orderType)" effect="plain">
                  {{ getOrderTypeText(row.orderType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column label="订单金额" width="120">
              <template #default="{ row }">¥ {{ formatMoney(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusTagType(row.status)">
                  {{ getOrderStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="商品数量" width="100">
              <template #default="{ row }">{{ row.items?.length || 0 }} 件</template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" min-width="180" />
            <el-table-column label="操作" min-width="280" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openOrderDetail(row)">详情</el-button>
                <el-button
                  v-if="Number(row.orderType) === 1 && Number(row.status) === 1"
                  size="small"
                  type="success"
                  plain
                  @click="handleChangeOrderStatus(row, 2)"
                >
                  发货
                </el-button>
                <el-button
                  v-if="Number(row.orderType) === 1 && Number(row.status) === 2"
                  size="small"
                  type="warning"
                  plain
                  @click="handleChangeOrderStatus(row, 3)"
                >
                  完成
                </el-button>
                <el-button
                  v-if="Number(row.orderType) === 1 && Number(row.status) !== 4"
                  size="small"
                  type="danger"
                  plain
                  @click="handleCancelOrder(row)"
                >
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="orderCurrentPage"
              v-model:page-size="orderPageSize"
              :page-sizes="[5, 10, 20]"
              :total="orderTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
            />
          </div>
        </div>

        <div v-if="activeMenu === 'deadLetter'" class="panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">异常消息</h3>
              <p class="panel-subtitle">秒杀消费失败进入死信队列后，在此人工核对与处理</p>
            </div>
            <div class="panel-actions">
              <el-select
                v-model="deadLetterStatusFilter"
                placeholder="处理状态"
                style="width: 150px"
                @change="loadDeadLetters"
              >
                <el-option
                  v-for="item in deadLetterStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button :loading="deadLetterLoading" @click="refreshDeadLetters">刷新</el-button>
            </div>
          </div>

          <el-alert
            v-if="deadLetterPendingCount > 0"
            :title="`当前有 ${deadLetterPendingCount} 条待处理异常消息`"
            type="warning"
            show-icon
            :closable="false"
            class="dead-letter-alert"
          />

          <el-table
            :data="deadLetterData"
            border
            style="width: 100%"
            v-loading="deadLetterLoading"
            element-loading-text="异常消息加载中..."
            :empty-text="deadLetterLoading ? '正在加载异常消息...' : '暂无异常消息'"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="requestId" label="RequestId" min-width="180" show-overflow-tooltip />
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="productId" label="商品ID" width="100" />
            <el-table-column prop="retryCount" label="重试次数" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getDeadLetterStatusTagType(row.status)">
                  {{ row.statusText || getDeadLetterStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failReason" label="失败原因" min-width="220" show-overflow-tooltip />
            <el-table-column label="进入时间" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openDeadLetterDetail(row.id)">详情</el-button>
                <el-button
                  v-if="Number(row.status) !== 2"
                  size="small"
                  type="primary"
                  plain
                  @click="handleMarkDeadLetterDone(row)"
                >
                  标记已处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="deadLetterCurrentPage"
              v-model:page-size="deadLetterPageSize"
              :page-sizes="[5, 10, 20]"
              :total="deadLetterTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
            />
          </div>
        </div>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog
    v-model="productDialogVisible"
    :title="productDialogTitle"
    width="760px"
    destroy-on-close
    @closed="resetProductForm"
  >
    <el-form
      ref="productFormRef"
      :model="productForm"
      :rules="productRules"
      label-width="110px"
      class="product-form"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="productForm.name" placeholder="请输入商品名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="productForm.categoryId" placeholder="请选择分类" filterable style="width: 100%">
              <el-option
                v-for="item in categoryOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品价格" prop="price">
            <el-input-number
              v-model="productForm.price"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入商品价格"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品库存" prop="stock">
            <el-input-number
              v-model="productForm.stock"
              :min="1"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入库存"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品状态" prop="status">
            <el-radio-group v-model="productForm.status">
              <el-radio :label="1">上架</el-radio>
              <el-radio :label="0">下架</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否秒杀" prop="isFlashSale">
            <el-radio-group v-model="productForm.isFlashSale">
              <el-radio :label="1">是</el-radio>
              <el-radio :label="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="商品封面" prop="image">
            <el-input v-model="productForm.image" placeholder="请输入封面图片地址" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="商品描述" prop="description">
            <el-input
              v-model="productForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入商品描述"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="Number(productForm.isFlashSale) === 1">
          <el-form-item label="秒杀价格" prop="flashPrice">
            <el-input-number
              v-model="productForm.flashPrice"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入秒杀价格"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="Number(productForm.isFlashSale) === 1">
          <el-form-item label="秒杀库存" prop="flashStock">
            <el-input-number
              v-model="productForm.flashStock"
              :min="0"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入秒杀库存"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="Number(productForm.isFlashSale) === 1">
          <el-form-item label="开始时间" prop="flashStartTime">
            <el-date-picker
              v-model="productForm.flashStartTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="请选择开始时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="Number(productForm.isFlashSale) === 1">
          <el-form-item label="结束时间" prop="flashEndTime">
            <el-date-picker
              v-model="productForm.flashEndTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="请选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="productDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSaveProduct">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="categoryDialogVisible"
    :title="categoryDialogTitle"
    width="520px"
    destroy-on-close
    @closed="resetCategoryForm"
  >
    <el-form
      ref="categoryFormRef"
      :model="categoryForm"
      :rules="categoryRules"
      label-width="100px"
    >
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number
          v-model="categoryForm.sort"
          :min="0"
          :step="1"
          controls-position="right"
          style="width: 100%"
          placeholder="请输入排序值"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="categoryForm.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="categoryDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="categorySaving" @click="handleSaveCategory">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="groupBuyDialogVisible"
    :title="groupBuyDialogTitle"
    width="760px"
    destroy-on-close
    @closed="resetGroupBuyForm"
  >
    <el-form
      ref="groupBuyFormRef"
      :model="groupBuyForm"
      :rules="groupBuyRules"
      label-width="110px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="商品ID" prop="productId">
            <el-input-number
              v-model="groupBuyForm.productId"
              :min="1"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入商品ID"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="拼团价" prop="groupPrice">
            <el-input-number
              v-model="groupBuyForm.groupPrice"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入拼团价"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成团人数" prop="requiredSize">
            <el-input-number
              v-model="groupBuyForm.requiredSize"
              :min="2"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入成团人数"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="活动时长(小时)" prop="durationHours">
            <el-input-number
              v-model="groupBuyForm.durationHours"
              :min="1"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入活动时长"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="活动库存" prop="stock">
            <el-input-number
              v-model="groupBuyForm.stock"
              :min="1"
              :step="1"
              controls-position="right"
              style="width: 100%"
              placeholder="请输入活动库存"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="活动状态" prop="status">
            <el-radio-group v-model="groupBuyForm.status">
              <el-radio :label="1">进行中</el-radio>
              <el-radio :label="0">已下架</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
              v-model="groupBuyForm.startTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="可选"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
              v-model="groupBuyForm.endTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="可选"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="groupBuyDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="groupBuySaving" @click="handleSaveGroupBuy">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="deadLetterDetailVisible"
    title="异常消息详情"
    width="860px"
    destroy-on-close
  >
    <template v-if="deadLetterDetailData">
      <div v-loading="deadLetterDetailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ deadLetterDetailData.id }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ deadLetterDetailData.bizType }}</el-descriptions-item>
          <el-descriptions-item label="RequestId">{{ deadLetterDetailData.requestId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ deadLetterDetailData.userId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="商品ID">{{ deadLetterDetailData.productId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ deadLetterDetailData.retryCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getDeadLetterStatusTagType(deadLetterDetailData.status)">
              {{ deadLetterDetailData.statusText || getDeadLetterStatusText(deadLetterDetailData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="进入时间">{{ formatDateTime(deadLetterDetailData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="失败原因" :span="2">
            {{ deadLetterDetailData.failReason || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理备注" :span="2">
            {{ deadLetterDetailData.handleRemark || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间" :span="2">
            {{ formatDateTime(deadLetterDetailData.handleTime) || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="dead-letter-message-block">
          <div class="dead-letter-message-title">原始消息体</div>
          <pre class="dead-letter-message-body">{{ formatDeadLetterBody(deadLetterDetailData.messageBody) }}</pre>
        </div>
      </div>
    </template>
    <template #footer>
      <el-button @click="deadLetterDetailVisible = false">关闭</el-button>
      <el-button
        v-if="deadLetterDetailData && Number(deadLetterDetailData.status) !== 2"
        type="primary"
        @click="handleMarkDeadLetterDone(deadLetterDetailData)"
      >
        标记已处理
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="orderDetailVisible"
    title="订单详情"
    width="820px"
    destroy-on-close
  >
    <template v-if="orderDetailData">
      <div v-loading="orderDetailLoading">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ orderDetailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ orderDetailData.userId }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="getOrderTypeTagType(orderDetailData.orderType)" effect="plain">
            {{ getOrderTypeText(orderDetailData.orderType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getOrderStatusTagType(orderDetailData.status)">
            {{ getOrderStatusText(orderDetailData.status, orderDetailData.orderType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单金额">¥ {{ formatMoney(orderDetailData.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDateTime(orderDetailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(orderDetailData.updateTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-table
        :data="orderDetailData.items || []"
        border
        style="width: 100%; margin-top: 16px"
      >
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="order-item-cell">
              <div class="order-item-thumb">
                <template v-if="row.productImage">
                  <img :src="row.productImage" :alt="row.productName" />
                </template>
                <template v-else>暂无图片</template>
              </div>
              <div class="order-item-name">{{ row.productName }}</div>
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
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Bell, DataLine, Document, Goods, Lightning, Plus } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { addProduct, deleteProduct, getProductList, offShelfProduct, onShelfProduct, updateProduct } from '../api/product'
import { cancelAdminOrder, getAdminOrderDetail, getAdminOrderList, updateAdminOrderStatus } from '../api/admin'
import {
  getDeadLetterDetail,
  getDeadLetterList,
  getDeadLetterPendingCount,
  markDeadLetterDone
} from '../api/deadLetter'
import {
  addCategory,
  deleteCategory,
  getAdminCategoryList,
  getCategoryList,
  updateCategory
} from '../api/category'
import {
  disableSeckillBuyDegrade,
  enableSeckillBuyDegrade,
  getSeckillBuyDegradeStatus,
  refreshSeckillListCache
} from '../api/adminSeckill'
import {
  addGroupBuyActivity,
  disableGroupBuyActivity,
  enableGroupBuyActivity,
  getAdminGroupBuyList,
  updateGroupBuyActivity
} from '../api/adminGroupBuy'

const router = useRouter()
const activeMenu = ref('overview')
const chartRef = ref(null)
let myChart = null
const resizeHandler = () => {
  if (myChart) {
    myChart.resize()
  }
}

const breadcrumbText = computed(() => {
  const map = {
    overview: '数据概览',
    products: '商品管理',
    categories: '商品分类',
    flashSale: '秒杀管理',
    orders: '订单管理',
    deadLetter: '异常消息'
  }
  return map[activeMenu.value]
})

const handleMenuSelect = (index) => {
  activeMenu.value = index
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('admin_token')
    router.push('/admin/login')
  }
}

const categoryOptions = ref([])
const categoryData = ref([])
const categoryLoading = ref(false)
const categorySaving = ref(false)
const categoryDialogVisible = ref(false)
const categoryFormRef = ref(null)
const productData = ref([])
const productSearchKeyword = ref('')
const productCurrentPage = ref(1)
const productPageSize = ref(10)
const flashSaleSearchKeyword = ref('')
const flashSaleCurrentPage = ref(1)
const flashSalePageSize = ref(5)
const productLoading = ref(false)
const flashSaleLoading = ref(false)
const seckillBuyDegraded = ref(false)
const seckillDegradeLoading = ref(false)
const seckillListCacheLoading = ref(false)
const nowTick = ref(Date.now())
let countdownTimer = null
const orderSearchKeyword = ref('')
const orderStatusFilter = ref('')
const orderCurrentPage = ref(1)
const orderPageSize = ref(10)
const orderLoading = ref(false)
const orderData = ref([])
const orderTotal = ref(0)
const orderDetailVisible = ref(false)
const orderDetailLoading = ref(false)
const orderDetailData = ref(null)
const deadLetterStatusFilter = ref(0)
const deadLetterCurrentPage = ref(1)
const deadLetterPageSize = ref(10)
const deadLetterLoading = ref(false)
const deadLetterData = ref([])
const deadLetterTotal = ref(0)
const deadLetterPendingCount = ref(0)
const deadLetterDetailVisible = ref(false)
const deadLetterDetailLoading = ref(false)
const deadLetterDetailData = ref(null)
let deadLetterPollTimer = null
const saving = ref(false)
const productDialogVisible = ref(false)
const productFormRef = ref(null)

const createEmptyProductForm = () => ({
  id: null,
  name: '',
  categoryId: null,
  price: null,
  stock: 0,
  image: '',
  description: '',
  status: 1,
  isFlashSale: 0,
  flashPrice: null,
  flashStock: null,
  flashStartTime: null,
  flashEndTime: null
})

const productForm = reactive(createEmptyProductForm())

const createEmptyCategoryForm = () => ({
  id: null,
  name: '',
  sort: 0,
  status: 1
})

const categoryForm = reactive(createEmptyCategoryForm())

const categoryMap = computed(() => {
  return new Map(categoryOptions.value.map(item => [Number(item.id), item.name]))
})

const productTotal = ref(0)
const flashSaleData = ref([])
const flashSaleTotal = ref(0)
const groupBuyData = ref([])
const groupBuyTotal = ref(0)
const groupBuySearchKeyword = ref('')
const groupBuyProductIdFilter = ref('')
const groupBuyStatusFilter = ref('')
const groupBuyCurrentPage = ref(1)
const groupBuyPageSize = ref(10)
const groupBuyLoading = ref(false)
const groupBuySaving = ref(false)
const groupBuyDialogVisible = ref(false)
const groupBuyFormRef = ref(null)
const groupBuyStatusOptions = [
  { label: '进行中', value: 1 },
  { label: '已下架', value: 0 }
]

const productDialogTitle = computed(() => {
  if (productForm.id) {
    return Number(productForm.isFlashSale) === 1 ? '编辑秒杀商品' : '编辑商品'
  }
  return Number(productForm.isFlashSale) === 1 ? '添加秒杀商品' : '上架新商品'
})

const categoryDialogTitle = computed(() => {
  return categoryForm.id ? '编辑分类' : '新增分类'
})

const createEmptyGroupBuyForm = () => ({
  id: null,
  productId: null,
  groupPrice: null,
  requiredSize: 2,
  durationHours: 24,
  stock: 0,
  status: 1,
  startTime: null,
  endTime: null
})

const groupBuyForm = reactive(createEmptyGroupBuyForm())

const groupBuyDialogTitle = computed(() => {
  return groupBuyForm.id ? '编辑拼团活动' : '新增拼团活动'
})

const deadLetterStatusOptions = [
  { label: '待处理', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已处理', value: 2 }
]

const orderStatusOptions = [
  { label: '待支付', value: 0 },
  { label: '待发货', value: 1 },
  { label: '已发货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
  { label: '未成团已退款', value: 5 }
]

watch(productCurrentPage, () => {
  loadProducts()
})

watch(productSearchKeyword, (newValue, oldValue) => {
  const shouldReload = productCurrentPage.value === 1
  productCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadProducts()
  }
})

watch(productPageSize, (newValue, oldValue) => {
  const shouldReload = productCurrentPage.value === 1
  productCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadProducts()
  }
})

const productRules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '商品名称长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'change' }],
  stock: [{ required: true, message: '请输入商品库存', trigger: 'change' }],
  flashPrice: [
    {
      validator: (_, value, callback) => {
        if (Number(productForm.isFlashSale) === 1 && (value === null || value === undefined || value === '')) {
          callback(new Error('请填写秒杀价格'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  flashStock: [
    {
      validator: (_, value, callback) => {
        if (Number(productForm.isFlashSale) === 1 && (value === null || value === undefined || value === '')) {
          callback(new Error('请填写秒杀库存'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  flashStartTime: [
    {
      validator: (_, value, callback) => {
        if (Number(productForm.isFlashSale) === 1 && !value) {
          callback(new Error('请选择秒杀开始时间'))
          return
        }
        callback()
      },
      trigger: ['change']
    }
  ],
  flashEndTime: [
    {
      validator: (_, value, callback) => {
        if (Number(productForm.isFlashSale) === 1 && !value) {
          callback(new Error('请选择秒杀结束时间'))
          return
        }
        callback()
      },
      trigger: ['change']
    }
  ]
}

const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '分类名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  sort: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const groupBuyRules = {
  productId: [{ required: true, message: '请输入商品ID', trigger: 'change' }],
  groupPrice: [
    {
      validator: (_, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入拼团价'))
          return
        }
        if (Number(value) <= 0) {
          callback(new Error('拼团价必须大于 0'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  requiredSize: [
    {
      validator: (_, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入成团人数'))
          return
        }
        if (Number(value) < 2) {
          callback(new Error('成团人数至少为 2'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  durationHours: [
    {
      validator: (_, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入活动时长'))
          return
        }
        if (Number(value) <= 0) {
          callback(new Error('活动时长必须大于 0'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  stock: [
    {
      validator: (_, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入活动库存'))
          return
        }
        if (Number(value) <= 0) {
          callback(new Error('活动库存必须大于 0'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  endTime: [
    {
      validator: (_, value, callback) => {
        if (!value || !groupBuyForm.startTime) {
          callback()
          return
        }
        const start = parseDateTime(groupBuyForm.startTime)
        const end = parseDateTime(value)
        if (start && end && end <= start) {
          callback(new Error('结束时间必须晚于开始时间'))
          return
        }
        callback()
      },
      trigger: ['change']
    }
  ]
}

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
    status: 0,
    isFlashSale: 0,
    flashPrice: null,
    flashStock: null
  }
]

// 模拟秒杀数据，保持首页中的演示板块可直接展示
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

const getCategoryName = (categoryId) => {
  if (categoryId === null || categoryId === undefined) {
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

const getOrderTypeText = (orderType) => {
  switch (Number(orderType)) {
    case 2:
      return '秒杀'
    case 3:
      return '拼团'
    default:
      return '普通'
  }
}

const getOrderTypeTagType = (orderType) => {
  switch (Number(orderType)) {
    case 2:
      return 'danger'
    case 3:
      return 'warning'
    default:
      return 'info'
  }
}

const getOrderStatusText = (status, orderType = 1) => {
  const type = Number(orderType)
  const value = Number(status)
  if (type === 3) {
    switch (value) {
      case 0:
        return '待支付'
      case 1:
        return '已支付待成团'
      case 2:
        return '已成团待发货'
      case 3:
        return '已完成'
      case 4:
        return '已取消'
      case 5:
        return '未成团已退款'
      default:
        return '未知'
    }
  }
  if (type === 2) {
    switch (value) {
      case 0:
        return '待支付'
      case 1:
        return '待发货'
      case 2:
        return '已取消'
      case 3:
        return '已完成'
      default:
        return '未知'
    }
  }
  switch (value) {
    case 0:
      return '待支付'
    case 1:
      return '待发货'
    case 2:
      return '已发货'
    case 3:
      return '已完成'
    case 4:
      return '已取消'
    default:
      return '未知'
  }
}

const getOrderStatusTagType = (status) => {
  switch (Number(status)) {
    case 0:
      return 'warning'
    case 1:
      return 'danger'
    case 2:
      return 'success'
    case 3:
      return 'info'
    case 4:
      return 'info'
    default:
      return 'info'
  }
}

const getDeadLetterStatusText = (status) => {
  switch (Number(status)) {
    case 0:
      return '待处理'
    case 1:
      return '处理中'
    case 2:
      return '已处理'
    default:
      return '未知'
  }
}

const getDeadLetterStatusTagType = (status) => {
  switch (Number(status)) {
    case 0:
      return 'danger'
    case 1:
      return 'warning'
    case 2:
      return 'success'
    default:
      return 'info'
  }
}

const formatDeadLetterBody = (value) => {
  if (!value) {
    return '-'
  }
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
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

const flashSaleCountdownById = computed(() => {
  nowTick.value
  return Object.fromEntries(
    flashSaleData.value.map(row => [
      row.id,
      getCountdownParts(row.source?.flashEndTime, row.source?.flashStartTime)
    ])
  )
})

const getAdminCountdownLabel = (row) => {
  const parts = flashSaleCountdownById.value[row.id] ?? { type: 'none', expired: false }
  if (parts.type === 'none') {
    return ''
  }
  if (parts.expired) {
    return '活动已结束'
  }
  return parts.type === 'start' ? '距开始' : '距结束'
}

const getFlashSaleStatusType = (status) => {
  if (status === '进行中') {
    return 'danger'
  }
  if (status === '已结束') {
    return 'info'
  }
  return 'warning'
}

const getFlashSaleStatus = (item) => {
  nowTick.value
  const start = parseDateTime(item.flashStartTime)
  const end = parseDateTime(item.flashEndTime)
  const now = new Date()
  if (start && now < start) {
    return '未开始'
  }
  if (end && now > end) {
    return '已结束'
  }
  if (Number(item.flashStock ?? 0) <= 0 || Number(item.status) !== 1) {
    return '已结束'
  }
  return '进行中'
}

const buildFlashSaleDisplay = (item) => ({
  id: item.id,
  productName: item.name,
  originalPrice: formatMoney(item.price),
  flashPrice: formatMoney(item.flashPrice),
  stock: item.flashStock ?? item.stock ?? 0,
  source: item
})

watch(flashSaleCurrentPage, () => {
  loadFlashSaleProducts()
})

watch(flashSaleSearchKeyword, (newValue, oldValue) => {
  const shouldReload = flashSaleCurrentPage.value === 1
  flashSaleCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadFlashSaleProducts()
  }
})

watch(flashSalePageSize, (newValue, oldValue) => {
  const shouldReload = flashSaleCurrentPage.value === 1
  flashSaleCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadFlashSaleProducts()
  }
})

watch(groupBuyCurrentPage, () => {
  loadGroupBuys()
})

watch(groupBuySearchKeyword, (newValue, oldValue) => {
  const shouldReload = groupBuyCurrentPage.value === 1
  groupBuyCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadGroupBuys()
  }
})

watch(groupBuyProductIdFilter, (newValue, oldValue) => {
  const shouldReload = groupBuyCurrentPage.value === 1
  groupBuyCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadGroupBuys()
  }
})

watch(groupBuyStatusFilter, (newValue, oldValue) => {
  const shouldReload = groupBuyCurrentPage.value === 1
  groupBuyCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadGroupBuys()
  }
})

watch(groupBuyPageSize, (newValue, oldValue) => {
  const shouldReload = groupBuyCurrentPage.value === 1
  groupBuyCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadGroupBuys()
  }
})

watch(orderCurrentPage, () => {
  loadOrders()
})

watch(orderSearchKeyword, (newValue, oldValue) => {
  const shouldReload = orderCurrentPage.value === 1
  orderCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadOrders()
  }
})

watch(orderStatusFilter, (newValue, oldValue) => {
  const shouldReload = orderCurrentPage.value === 1
  orderCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadOrders()
  }
})

watch(orderPageSize, (newValue, oldValue) => {
  const shouldReload = orderCurrentPage.value === 1
  orderCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadOrders()
  }
})

watch(deadLetterCurrentPage, () => {
  loadDeadLetters()
})

watch(deadLetterStatusFilter, (newValue, oldValue) => {
  const shouldReload = deadLetterCurrentPage.value === 1
  deadLetterCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadDeadLetters()
  }
})

watch(deadLetterPageSize, (newValue, oldValue) => {
  const shouldReload = deadLetterCurrentPage.value === 1
  deadLetterCurrentPage.value = 1
  if (shouldReload && newValue !== oldValue) {
    loadDeadLetters()
  }
})

const normalizeProduct = (item) => ({
  id: item.id,
  name: item.name || '',
  categoryId: item.categoryId ?? item.category_id ?? null,
  price: item.price ?? null,
  stock: item.stock ?? 0,
  image: item.image || '',
  description: item.description || '',
  status: Number(item.status ?? 1),
  isFlashSale: Number(item.isFlashSale ?? item.is_flash_sale ?? 0),
  flashPrice: item.flashPrice ?? item.flash_price ?? null,
  flashStock: item.flashStock ?? item.flash_stock ?? null,
  flashStartTime: item.flashStartTime ?? item.flash_start_time ?? null,
  flashEndTime: item.flashEndTime ?? item.flash_end_time ?? null,
  createTime: item.createTime ?? item.create_time ?? null,
  updateTime: item.updateTime ?? item.update_time ?? null
})

const normalizeCategory = (item) => ({
  id: item.id,
  name: item.name || '',
  sort: item.sort ?? 0,
  status: item.status ?? 1
})

const normalizeGroupBuy = (item) => ({
  id: item.id,
  productId: item.productId ?? item.product_id ?? null,
  productName: item.productName || item.product_name || '',
  categoryName: item.categoryName || item.category_name || '',
  productPrice: item.productPrice ?? item.product_price ?? null,
  groupPrice: item.groupPrice ?? item.group_price ?? null,
  requiredSize: item.requiredSize ?? item.required_size ?? 2,
  durationHours: item.durationHours ?? item.duration_hours ?? 24,
  stock: item.stock ?? 0,
  status: Number(item.status ?? 1),
  statusText: item.statusText || item.status_text || (Number(item.status ?? 1) === 1 ? '进行中' : '已下架'),
  startTime: item.startTime ?? item.start_time ?? null,
  endTime: item.endTime ?? item.end_time ?? null,
  createTime: item.createTime ?? item.create_time ?? null,
  updateTime: item.updateTime ?? item.update_time ?? null
})

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryOptions.value = res.data.map(normalizeCategory)
      return
    }
    categoryOptions.value = fallbackCategories
  } catch {
    categoryOptions.value = fallbackCategories
  }
}

const loadAdminCategories = async () => {
  categoryLoading.value = true
  try {
    const res = await getAdminCategoryList()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryData.value = res.data.map(normalizeCategory)
      return
    }
    categoryData.value = fallbackCategories.map(item => ({ ...item }))
  } catch {
    categoryData.value = fallbackCategories.map(item => ({ ...item }))
  } finally {
    categoryLoading.value = false
  }
}

const loadProducts = async () => {
  productLoading.value = true
  try {
    const res = await getProductList({
      keyword: productSearchKeyword.value.trim() || undefined,
      page: productCurrentPage.value,
      size: productPageSize.value
    })
    if (res.code === 200 && res.data) {
      const list = Array.isArray(res.data.list) ? res.data.list : []
      productData.value = list.map(normalizeProduct)
      productTotal.value = Number(res.data.total) || 0
      return
    }
    productData.value = fallbackProducts.map(item => ({ ...item }))
    productTotal.value = fallbackProducts.length
  } catch {
    productData.value = fallbackProducts.map(item => ({ ...item }))
    productTotal.value = fallbackProducts.length
  } finally {
    productLoading.value = false
  }
}

const loadGroupBuys = async () => {
  groupBuyLoading.value = true
  try {
    const productId =
      groupBuyProductIdFilter.value === '' ||
      groupBuyProductIdFilter.value === null ||
      groupBuyProductIdFilter.value === undefined
        ? undefined
        : Number(groupBuyProductIdFilter.value)
    const status =
      groupBuyStatusFilter.value === '' ||
      groupBuyStatusFilter.value === null ||
      groupBuyStatusFilter.value === undefined
        ? undefined
        : Number(groupBuyStatusFilter.value)
    const res = await getAdminGroupBuyList({
      keyword: groupBuySearchKeyword.value.trim() || undefined,
      productId,
      status,
      page: groupBuyCurrentPage.value,
      size: groupBuyPageSize.value
    })
    if (res.code === 200 && res.data) {
      const list = Array.isArray(res.data.list) ? res.data.list : []
      groupBuyData.value = list.map(normalizeGroupBuy)
      groupBuyTotal.value = Number(res.data.total) || 0
      return
    }
    groupBuyData.value = []
    groupBuyTotal.value = 0
  } catch {
    groupBuyData.value = []
    groupBuyTotal.value = 0
  } finally {
    groupBuyLoading.value = false
  }
}

const loadSeckillBuyDegradeStatus = async () => {
  try {
    const res = await getSeckillBuyDegradeStatus()
    if (res.code === 200 && res.data) {
      seckillBuyDegraded.value = Boolean(res.data.degraded)
    }
  } catch {
    // handled by interceptor
  }
}

const handleEnableSeckillDegrade = async () => {
  try {
    await ElMessageBox.confirm(
      '开启后，用户将无法发起新的秒杀抢购。仅影响写操作，秒杀列表仍可浏览。确定开启吗？',
      '开启抢购降级',
      {
        type: 'warning',
        confirmButtonText: '确定开启',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  seckillDegradeLoading.value = true
  try {
    const res = await enableSeckillBuyDegrade()
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    ElMessage.success('已开启抢购降级')
    await loadSeckillBuyDegradeStatus()
  } catch {
    // handled by interceptor
  } finally {
    seckillDegradeLoading.value = false
  }
}

const handleDisableSeckillDegrade = async () => {
  seckillDegradeLoading.value = true
  try {
    const res = await disableSeckillBuyDegrade()
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    ElMessage.success('已关闭抢购降级')
    await loadSeckillBuyDegradeStatus()
  } catch {
    // handled by interceptor
  } finally {
    seckillDegradeLoading.value = false
  }
}

const handleRefreshSeckillListCache = async () => {
  seckillListCacheLoading.value = true
  try {
    const res = await refreshSeckillListCache()
    if (res.code !== 200) {
      throw new Error(res.message || '刷新失败')
    }
    ElMessage.success('前台秒杀列表缓存已刷新')
  } catch {
    // handled by interceptor
  } finally {
    seckillListCacheLoading.value = false
  }
}

const loadFlashSaleProducts = async () => {
  flashSaleLoading.value = true
  try {
    const res = await getProductList({
      keyword: flashSaleSearchKeyword.value.trim() || undefined,
      flashSale: true,
      page: flashSaleCurrentPage.value,
      size: flashSalePageSize.value
    })
    if (res.code === 200 && res.data) {
      const list = Array.isArray(res.data.list) ? res.data.list : []
      flashSaleData.value = list.map(buildFlashSaleDisplay)
      flashSaleTotal.value = Number(res.data.total) || 0
      return
    }
    const fallbackList = fallbackProducts
      .filter(item => Number(item.isFlashSale) === 1)
      .map(buildFlashSaleDisplay)
    flashSaleData.value = fallbackList
    flashSaleTotal.value = fallbackList.length
  } catch {
    const fallbackList = fallbackProducts
      .filter(item => Number(item.isFlashSale) === 1)
      .map(buildFlashSaleDisplay)
    flashSaleData.value = fallbackList
    flashSaleTotal.value = fallbackList.length
  } finally {
    flashSaleLoading.value = false
  }
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

const normalizeOrder = (item) => ({
  id: Number(item.id),
  orderNo: item.orderNo || item.order_no || '',
  userId: item.userId ?? item.user_id ?? null,
  totalAmount: item.totalAmount ?? item.total_amount ?? 0,
  status: Number(item.status ?? 0),
  orderType: Number(item.orderType ?? item.order_type ?? 1),
  statusText: item.statusText || item.status_text || getOrderStatusText(item.status, item.orderType ?? item.order_type ?? 1),
  createTime: item.createTime ?? item.create_time ?? null,
  updateTime: item.updateTime ?? item.update_time ?? null,
  items: Array.isArray(item.items) ? item.items.map(normalizeOrderItem) : []
})

const normalizeOrderDetail = (item) => ({
  id: Number(item.id),
  orderNo: item.orderNo || item.order_no || '',
  userId: item.userId ?? item.user_id ?? null,
  totalAmount: item.totalAmount ?? item.total_amount ?? 0,
  status: Number(item.status ?? 0),
  orderType: Number(item.orderType ?? item.order_type ?? 1),
  statusText: item.statusText || item.status_text || getOrderStatusText(item.status, item.orderType ?? item.order_type ?? 1),
  createTime: item.createTime ?? item.create_time ?? null,
  updateTime: item.updateTime ?? item.update_time ?? null,
  items: Array.isArray(item.items) ? item.items.map(normalizeOrderItem) : []
})

const loadOrders = async () => {
  orderLoading.value = true
  try {
    const res = await getAdminOrderList({
      keyword: orderSearchKeyword.value.trim() || undefined,
      status:
        orderStatusFilter.value === '' || orderStatusFilter.value === null || orderStatusFilter.value === undefined
          ? undefined
          : Number(orderStatusFilter.value),
      page: orderCurrentPage.value,
      size: orderPageSize.value
    })
    if (res.code === 200 && res.data) {
      const list = Array.isArray(res.data.list) ? res.data.list : []
      orderData.value = list.map(normalizeOrder)
      orderTotal.value = Number(res.data.total) || 0
      return
    }
    orderData.value = []
    orderTotal.value = 0
  } catch {
    orderData.value = []
    orderTotal.value = 0
  } finally {
    orderLoading.value = false
  }
}

const refreshOrders = async () => {
  await loadOrders()
}

const normalizeDeadLetter = (item) => ({
  id: Number(item.id),
  bizType: item.bizType || item.biz_type || '',
  requestId: item.requestId || item.request_id || '',
  userId: item.userId ?? item.user_id ?? null,
  productId: item.productId ?? item.product_id ?? null,
  messageBody: item.messageBody || item.message_body || '',
  failReason: item.failReason || item.fail_reason || '',
  retryCount: Number(item.retryCount ?? item.retry_count ?? 0),
  status: Number(item.status ?? 0),
  statusText: item.statusText || item.status_text || getDeadLetterStatusText(item.status),
  handlerAdminId: item.handlerAdminId ?? item.handler_admin_id ?? null,
  handleRemark: item.handleRemark || item.handle_remark || '',
  createTime: item.createTime ?? item.create_time ?? null,
  handleTime: item.handleTime ?? item.handle_time ?? null
})

const loadDeadLetterPendingCount = async () => {
  try {
    const res = await getDeadLetterPendingCount()
    if (res.code === 200 && res.data) {
      deadLetterPendingCount.value = Number(res.data.count) || 0
    }
  } catch {
    // handled by interceptor
  }
}

const loadDeadLetters = async () => {
  deadLetterLoading.value = true
  try {
    const res = await getDeadLetterList({
      status: deadLetterStatusFilter.value,
      page: deadLetterCurrentPage.value,
      size: deadLetterPageSize.value
    })
    if (res.code === 200 && res.data) {
      const list = Array.isArray(res.data.list) ? res.data.list : []
      deadLetterData.value = list.map(normalizeDeadLetter)
      deadLetterTotal.value = Number(res.data.total) || 0
      return
    }
    deadLetterData.value = []
    deadLetterTotal.value = 0
  } catch {
    deadLetterData.value = []
    deadLetterTotal.value = 0
  } finally {
    deadLetterLoading.value = false
  }
}

const refreshDeadLetters = async () => {
  await Promise.all([loadDeadLetterPendingCount(), loadDeadLetters()])
}

const openDeadLetterDetail = async (id) => {
  deadLetterDetailLoading.value = true
  try {
    const res = await getDeadLetterDetail(id)
    if (res.code === 200 && res.data) {
      deadLetterDetailData.value = normalizeDeadLetter(res.data)
      deadLetterDetailVisible.value = true
    }
  } catch {
    // handled by interceptor
  } finally {
    deadLetterDetailLoading.value = false
  }
}

const handleMarkDeadLetterDone = async (row) => {
  let remark = '已人工处理'
  try {
    const { value } = await ElMessageBox.prompt('请输入处理备注（可选）', '标记已处理', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: '已人工处理',
      inputPlaceholder: '例如：已核对并补单'
    })
    remark = value?.trim() || '已人工处理'
  } catch {
    return
  }

  try {
    const res = await markDeadLetterDone(row.id, remark)
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    ElMessage.success('已标记处理完成')
    await refreshDeadLetters()
    if (deadLetterDetailVisible.value && deadLetterDetailData.value?.id === row.id) {
      await openDeadLetterDetail(row.id)
    }
  } catch {
    // handled by interceptor
  }
}

const openOrderDetail = async (order) => {
  orderDetailLoading.value = true
  try {
    const orderId = typeof order === 'object' ? order.id : order
    const orderType = typeof order === 'object' ? order.orderType : undefined
    const res = await getAdminOrderDetail(orderId, orderType)
    if (res.code === 200 && res.data) {
      orderDetailData.value = normalizeOrderDetail(res.data)
      orderDetailVisible.value = true
      return
    }
  } catch {
    // handled by interceptor
  } finally {
    orderDetailLoading.value = false
  }
}

const handleChangeOrderStatus = async (row, targetStatus) => {
  const statusText = getOrderStatusText(targetStatus)
  try {
    await ElMessageBox.confirm(`确定将订单「${row.orderNo}」修改为【${statusText}】吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await updateAdminOrderStatus(row.id, targetStatus)
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    ElMessage.success('订单状态已更新')
    await loadOrders()
    if (orderDetailVisible.value && orderDetailData.value?.id === row.id) {
      await openOrderDetail(row)
    }
  } catch {
    row.status = targetStatus
    row.statusText = statusText
    ElMessage.warning('接口暂不可用，已先同步当前页面状态')
  }
}

const handleCancelOrder = async (row) => {
  try {
    await ElMessageBox.confirm(`确定取消订单「${row.orderNo}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '取消订单',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await cancelAdminOrder(row.id)
    if (res.code !== 200) {
      throw new Error(res.message || '取消失败')
    }
    ElMessage.success('订单已取消')
    await loadOrders()
    if (orderDetailVisible.value && orderDetailData.value?.id === row.id) {
      await openOrderDetail(row)
    }
  } catch {
    row.status = 4
    row.statusText = getOrderStatusText(4)
    ElMessage.warning('接口暂不可用，已先同步当前页面状态')
  }
}

const resetCategoryForm = () => {
  Object.assign(categoryForm, createEmptyCategoryForm())
  if (categoryFormRef.value) {
    categoryFormRef.value.clearValidate()
  }
}

const openAddCategoryDialog = () => {
  resetCategoryForm()
  categoryDialogVisible.value = true
}

const openEditCategoryDialog = (row) => {
  resetCategoryForm()
  Object.assign(categoryForm, normalizeCategory(row))
  categoryDialogVisible.value = true
}

const handleDeleteCategory = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？删除后不可恢复。', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await deleteCategory(row.id)
    if (res.code !== 200) {
      throw new Error(res.message || '删除失败')
    }
    ElMessage.success('分类删除成功')
    await Promise.all([loadAdminCategories(), loadCategories()])
  } catch {
    categoryData.value = categoryData.value.filter(item => Number(item.id) !== Number(row.id))
    ElMessage.warning('接口暂不可用，已从当前页面移除')
  }
}

const handleSaveCategory = async () => {
  if (!categoryFormRef.value) {
    return
  }

  const valid = await categoryFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  const payload = {
    ...categoryForm,
    name: categoryForm.name.trim(),
    sort: Number(categoryForm.sort) || 0,
    status: Number(categoryForm.status) || 0
  }

  categorySaving.value = true
  try {
    const res = payload.id ? await updateCategory(payload) : await addCategory(payload)
    if (res.code !== 200) {
      throw new Error(res.message || '保存失败')
    }
    ElMessage.success(payload.id ? '分类已更新' : '分类已新增')
    categoryDialogVisible.value = false
    await Promise.all([loadAdminCategories(), loadCategories()])
  } catch {
    const normalized = normalizeCategory({
      ...payload,
      id: payload.id ?? Date.now()
    })
    const nextList = [...categoryData.value]
    const index = nextList.findIndex(item => Number(item.id) === Number(normalized.id))
    if (index >= 0) {
      nextList[index] = { ...nextList[index], ...normalized }
    } else {
      nextList.unshift(normalized)
    }
    categoryData.value = nextList
    ElMessage.warning('接口暂不可用，已先更新当前页面数据')
    categoryDialogVisible.value = false
  } finally {
    categorySaving.value = false
  }
}

const resetGroupBuyForm = () => {
  Object.assign(groupBuyForm, createEmptyGroupBuyForm())
  if (groupBuyFormRef.value) {
    groupBuyFormRef.value.clearValidate()
  }
}

const openAddGroupBuyDialog = () => {
  resetGroupBuyForm()
  groupBuyDialogVisible.value = true
}

const openEditGroupBuyDialog = (row) => {
  resetGroupBuyForm()
  Object.assign(groupBuyForm, normalizeGroupBuy(row))
  groupBuyDialogVisible.value = true
}

const refreshGroupBuys = async () => {
  await loadGroupBuys()
}

const toggleGroupBuyStatus = async (row) => {
  const shouldEnable = Number(row.status) !== 1
  const actionText = shouldEnable ? '上架' : '下架'
  const confirmText = shouldEnable
    ? `确定上架拼团活动「${row.productName || row.productId}」吗？`
    : `确定下架拼团活动「${row.productName || row.productId}」吗？`

  try {
    await ElMessageBox.confirm(confirmText, '提示', {
      type: 'warning',
      confirmButtonText: actionText,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = shouldEnable ? await enableGroupBuyActivity(row.id) : await disableGroupBuyActivity(row.id)
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    ElMessage.success(`${actionText}成功`)
    await loadGroupBuys()
  } catch {
    row.status = shouldEnable ? 1 : 0
    row.statusText = shouldEnable ? '进行中' : '已下架'
    ElMessage.warning('接口暂不可用，已先同步当前页面状态')
  }
}

const handleSaveGroupBuy = async () => {
  if (!groupBuyFormRef.value) {
    return
  }

  const valid = await groupBuyFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  const payload = {
    ...groupBuyForm,
    productId: Number(groupBuyForm.productId),
    groupPrice: Number(groupBuyForm.groupPrice),
    requiredSize: Number(groupBuyForm.requiredSize),
    durationHours: Number(groupBuyForm.durationHours),
    stock: Number(groupBuyForm.stock),
    status: Number(groupBuyForm.status) || 0,
    startTime: groupBuyForm.startTime || null,
    endTime: groupBuyForm.endTime || null
  }

  if (payload.endTime && payload.startTime) {
    const start = parseDateTime(payload.startTime)
    const end = parseDateTime(payload.endTime)
    if (start && end && end <= start) {
      ElMessage.warning('结束时间必须晚于开始时间')
      return
    }
  }

  groupBuySaving.value = true
  try {
    const res = payload.id ? await updateGroupBuyActivity(payload) : await addGroupBuyActivity(payload)
    if (res.code !== 200) {
      throw new Error(res.message || '保存失败')
    }
    ElMessage.success(payload.id ? '拼团活动已更新' : '拼团活动已新增')
    groupBuyDialogVisible.value = false
    await loadGroupBuys()
  } catch {
    const normalized = normalizeGroupBuy(payload)
    if (payload.id) {
      groupBuyData.value = groupBuyData.value.map(item =>
        Number(item.id) === Number(payload.id) ? normalized : item
      )
    } else {
      groupBuyData.value = [normalized, ...groupBuyData.value]
      groupBuyTotal.value += 1
    }
    ElMessage.warning('接口暂不可用，已先更新当前页面数据')
    groupBuyDialogVisible.value = false
  } finally {
    groupBuySaving.value = false
  }
}

const openAddDialog = () => {
  resetProductForm()
  productDialogVisible.value = true
}

const openAddFlashSaleDialog = () => {
  resetProductForm()
  productForm.isFlashSale = 1
  productForm.status = 1
  productForm.stock = 1
  productDialogVisible.value = true
}

const openEditDialog = (row) => {
  resetProductForm()
  Object.assign(productForm, normalizeProduct(row))
  productDialogVisible.value = true
}

const endFlashSale = async (row) => {
  try {
    await ElMessageBox.confirm(`确定结束「${row.name}」的秒杀吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const payload = {
    ...normalizeProduct(row),
    isFlashSale: 0,
    flashPrice: null,
    flashStock: null,
    flashStartTime: null,
    flashEndTime: null
  }

  try {
    const res = await updateProduct(payload)
    if (res.code !== 200) {
      throw new Error(res.message || '结束失败')
    }
    ElMessage.success('秒杀已结束')
    await Promise.all([loadProducts(), loadFlashSaleProducts()])
  } catch {
    await Promise.all([loadProducts(), loadFlashSaleProducts()])
    ElMessage.warning('接口暂不可用，已先更新当前页面状态')
  }
}

const resetProductForm = () => {
  Object.assign(productForm, createEmptyProductForm())
  if (productFormRef.value) {
    productFormRef.value.clearValidate()
  }
}

const upsertLocalProduct = (payload) => {
  const nextList = [...productData.value]
  const normalized = normalizeProduct({
    ...payload,
    id: payload.id ?? Date.now(),
    createTime: payload.createTime ?? new Date().toISOString(),
    updateTime: new Date().toISOString()
  })
  const index = nextList.findIndex(item => Number(item.id) === Number(normalized.id))
  if (index >= 0) {
    nextList[index] = { ...nextList[index], ...normalized }
  } else {
    nextList.unshift(normalized)
  }
  productData.value = nextList
}

const removeLocalProduct = (id) => {
  productData.value = productData.value.filter(item => Number(item.id) !== Number(id))
}

const toggleShelf = async (row) => {
  const shouldOnShelf = Number(row.status) !== 1
  try {
    await ElMessageBox.confirm(
      `确定${shouldOnShelf ? '上架' : '下架'}该商品吗？`,
      '提示',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    const res = shouldOnShelf ? await onShelfProduct(row.id) : await offShelfProduct(row.id)
    if (res.code !== 200) {
      throw new Error(res.message || '操作失败')
    }
    ElMessage.success(shouldOnShelf ? '已上架' : '已下架')
    await Promise.all([loadProducts(), loadFlashSaleProducts()])
  } catch {
    row.status = shouldOnShelf ? 1 : 0
    ElMessage.warning('接口暂不可用，已同步到当前页面状态')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该商品吗？删除后不可恢复。', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    const res = await deleteProduct(row.id)
    if (res.code !== 200) {
      throw new Error(res.message || '删除失败')
    }
    ElMessage.success('删除成功')
    await Promise.all([loadProducts(), loadFlashSaleProducts()])
  } catch {
    removeLocalProduct(row.id)
    ElMessage.warning('接口暂不可用，已从当前页面移除')
  }
}

const handleSaveProduct = async () => {
  if (!productFormRef.value) {
    return
  }

  const valid = await productFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  const payload = {
    ...productForm,
    name: productForm.name.trim(),
    stock: Number(productForm.stock) || 0,
    status: Number(productForm.status) || 0,
    isFlashSale: Number(productForm.isFlashSale) || 0
  }

  if (payload.isFlashSale === 0) {
    payload.flashPrice = null
    payload.flashStock = null
    payload.flashStartTime = null
    payload.flashEndTime = null
  }

  saving.value = true
  try {
    const res = payload.id ? await updateProduct(payload) : await addProduct(payload)
    if (res.code !== 200) {
      throw new Error(res.message || '保存失败')
    }
    ElMessage.success(payload.id ? '商品已更新' : '商品已上架')
    productDialogVisible.value = false
    await Promise.all([loadProducts(), loadFlashSaleProducts()])
  } catch {
    upsertLocalProduct(payload)
    ElMessage.warning('接口暂不可用，已先更新当前页面数据')
    productDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) {
    return
  }

  if (myChart) {
    myChart.dispose()
  }

  myChart = echarts.init(chartRef.value)
  const option = {
    title: {
      text: '近7天收入趋势 (元)'
    },
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['5-14', '5-15', '5-16', '5-17', '5-18', '5-19', '5-20']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        data: [8200, 9320, 9010, 9340, 12900, 13300, 13200],
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.5)' },
            { offset: 1, color: 'rgba(64,158,255,0.1)' }
          ])
        },
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  }
  myChart.setOption(option)
}

watch(activeMenu, (newVal) => {
  if (newVal === 'overview') {
    nextTick(() => {
      initChart()
    })
  }
  if (newVal === 'orders') {
    loadOrders()
  }
  if (newVal === 'flashSale') {
    loadFlashSaleProducts()
    loadSeckillBuyDegradeStatus()
  }
  if (newVal === 'groupBuy') {
    loadGroupBuys()
  }
  if (newVal === 'deadLetter') {
    refreshDeadLetters()
  }
})

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

const startDeadLetterPolling = () => {
  if (deadLetterPollTimer) {
    return
  }
  deadLetterPollTimer = window.setInterval(() => {
    loadDeadLetterPendingCount()
    if (activeMenu.value === 'deadLetter') {
      loadDeadLetters()
    }
  }, 30000)
}

const stopDeadLetterPolling = () => {
  if (deadLetterPollTimer) {
    clearInterval(deadLetterPollTimer)
    deadLetterPollTimer = null
  }
}

onMounted(async () => {
  startCountdownTimer()
  startDeadLetterPolling()
  await Promise.all([
    loadCategories(),
    loadAdminCategories(),
    loadProducts(),
    loadFlashSaleProducts(),
    loadGroupBuys(),
    loadOrders(),
    loadDeadLetterPendingCount()
  ])
  if (activeMenu.value === 'overview') {
    initChart()
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  stopCountdownTimer()
  stopDeadLetterPolling()
  window.removeEventListener('resize', resizeHandler)
  if (myChart) {
    myChart.dispose()
    myChart = null
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.aside {
  background-color: #304156;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #1f2d3d;
}

.logo-area h2 {
  margin: 0;
  font-size: 18px;
  color: #fff;
}

.el-menu-vertical {
  border-right: none;
  flex: 1;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.breadcrumb {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-dropdown {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #606266;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}

.panel {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, .1);
  min-height: calc(100vh - 120px);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.panel-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.panel-subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.chart-container {
  width: 100%;
  height: 400px;
  margin-top: 20px;
}

.product-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name-text {
  font-weight: 600;
  color: #303133;
}

.product-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.product-form {
  padding-right: 12px;
}

.dead-letter-menu-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.menu-badge {
  margin-top: 2px;
}

.dead-letter-alert {
  margin-bottom: 16px;
}

.seckill-degrade-alert {
  margin-bottom: 16px;
}

.dead-letter-message-block {
  margin-top: 16px;
}

.dead-letter-message-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: #303133;
}

.dead-letter-message-body {
  margin: 0;
  padding: 12px;
  max-height: 280px;
  overflow: auto;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.admin-flash-countdown {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.admin-countdown-label {
  font-size: 12px;
  color: #f56c6c;
  font-weight: 600;
}

.countdown-digits {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.countdown-digit {
  background-color: #f56c6c;
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  font-size: 13px;
  min-width: 24px;
  text-align: center;
}

.countdown-sep {
  color: #f56c6c;
  font-weight: 700;
}

.countdown-empty {
  color: #909399;
}

.order-item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-item-thumb {
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

.order-item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.order-item-name {
  color: #303133;
  font-weight: 500;
}
</style>
