<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 relative overflow-hidden">
    <!-- 背景装饰 -->
    <div class="absolute inset-0 overflow-hidden">
      <div class="absolute -top-40 -right-40 w-80 h-80 bg-blue-500/20 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-40 -left-40 w-80 h-80 bg-indigo-500/20 rounded-full blur-3xl"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="relative z-10 w-full max-w-md mx-4">
      <div class="backdrop-blur-xl bg-white/10 rounded-3xl shadow-2xl border border-white/20 p-8">
        <!-- Logo区域 -->
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-2xl shadow-lg mb-4">
            <span class="text-3xl">⚙️</span>
          </div>
          <h1 class="text-3xl font-bold bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">MEMS</h1>
          <p class="text-gray-400 mt-2 text-sm">矿井提升机设备管理系统</p>
        </div>

        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="space-y-6">
          <div class="space-y-4">
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <span class="text-gray-400">👤</span>
              </div>
              <input
                v-model="username"
                type="text"
                class="w-full pl-12 pr-4 py-3.5 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/20 transition-all duration-200"
                placeholder="用户名"
                required
              />
            </div>

            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <span class="text-gray-400">🔒</span>
              </div>
              <input
                v-model="password"
                type="password"
                class="w-full pl-12 pr-4 py-3.5 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/20 transition-all duration-200"
                placeholder="密码"
                required
              />
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="error" class="bg-red-500/10 border border-red-500/30 rounded-xl px-4 py-3 text-red-400 text-sm text-center">
            {{ error }}
          </div>

          <!-- 登录按钮 -->
          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3.5 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold rounded-xl shadow-lg shadow-blue-500/25 transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
          >
            <span v-if="loading" class="animate-spin">⏳</span>
            <span>{{ loading ? '登录中...' : '登 录' }}</span>
          </button>
        </form>

        <!-- 底部信息 -->
        <div class="mt-8 pt-6 border-t border-white/10 text-center">
          <p class="text-gray-500 text-xs">
            测试账号: admin / 123456
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const username = ref('admin')
const password = ref('123456')
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await axios.post('/api/auth/login', {
      username: username.value,
      password: password.value
    })

    if (response.data.success) {
      // 存储登录状态
      localStorage.setItem('isLoggedIn', 'true')
      localStorage.setItem('username', response.data.username)
      localStorage.setItem('role', response.data.role)
      router.push('/dashboard')
    } else {
      error.value = response.data.message || '登录失败'
    }
  } catch (err) {
    console.error(err)
    if (err.response && err.response.data && err.response.data.message) {
      error.value = err.response.data.message
    } else {
      error.value = '登录失败，请检查网络或后端服务'
    }
  } finally {
    loading.value = false
  }
}
</script>
