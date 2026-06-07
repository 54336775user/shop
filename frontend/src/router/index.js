import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import AdminHomeView from '../views/AdminHomeView.vue'
import SearchView from '../views/SearchView.vue'
import CartView from '../views/CartView.vue'
import OrderListView from '../views/OrderListView.vue'
import PaymentView from '../views/PaymentView.vue'
import SeckillView from '../views/SeckillView.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/home', name: 'Home', component: HomeView },
  { path: '/search', name: 'Search', component: SearchView },
  { path: '/cart', name: 'Cart', component: CartView },
  { path: '/orders', name: 'Orders', component: OrderListView },
  { path: '/pay/:orderId', name: 'Payment', component: PaymentView },
  { path: '/seckill', name: 'Seckill', component: SeckillView },
  { path: '/admin/login', name: 'AdminLogin', component: AdminLoginView },
  { path: '/admin/home', name: 'AdminHome', component: AdminHomeView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
