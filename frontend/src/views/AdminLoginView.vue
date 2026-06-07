<template>
  <div class="admin-login-page">
    <div class="form-box">
      <h1 class="logo">⚙️ 后台管理系统</h1>

      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 登录表单 -->
      <el-form
        v-show="activeTab === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入管理员账号"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            clearable
          />
        </el-form-item>
        <div class="options">
          <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
        </div>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          native-type="submit"
        >登 录</el-button>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-show="activeTab === 'register'"
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请设置管理员账号"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请设置密码"
            prefix-icon="Lock"
            size="large"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            clearable
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          native-type="submit"
        >注 册</el-button>
      </el-form>
    </div>

    <footer class="footer">© 2025 网店商城后台管理 All Rights Reserved</footer>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {adminLogin, adminRegister} from '../api/admin'

const router = useRouter()

const tabs = [
  { key: 'login', label: '管理员登录' },
  { key: 'register', label: '管理员注册' }
]

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
  remember: true
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const loginRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

function switchTab(key) {
  activeTab.value = key
  loginFormRef.value?.resetFields()
  registerFormRef.value?.resetFields()
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await adminLogin({
      username: loginForm.username,
      password: loginForm.password
    })
    if (res.code === 200) {
      ElMessage.success('登录成功！欢迎回来，' + loginForm.username)
      localStorage.setItem('admin_token', res.data?.token || '')
      router.push('/admin/home')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await adminRegister({
      username: registerForm.username,
      password: registerForm.password
    })
    if (res.code === 200) {
      ElMessage.success('注册成功！即将跳转到登录页...')
      setTimeout(() => switchTab('login'), 1000)
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
}

.form-box {
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
}

.logo {
  text-align: center;
  font-size: 26px;
  color: #2c3e50;
  margin-bottom: 30px;
  letter-spacing: 1px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.tab-btn {
  flex: 1;
  padding: 10px;
  border: none;
  background: #f0f2f5;
  color: #606266;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  background: #e4e7ed;
}

.tab-btn.active {
  background: #3498db;
  color: #fff;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
}

.options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  letter-spacing: 4px;
  border-radius: 8px !important;
  height: 44px;
  font-size: 16px !important;
  background-color: #3498db;
  border-color: #3498db;
}

.submit-btn:hover {
  background-color: #2980b9;
  border-color: #2980b9;
}

.footer {
  margin-top: 24px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  text-align: center;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 5px 12px;
}
</style>
