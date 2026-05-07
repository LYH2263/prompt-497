<template>
  <Layout title="系统概览">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <!-- Stats Cards -->
      <div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <h3 class="text-gray-400 text-xs font-semibold uppercase tracking-wider">设备总数</h3>
        <div class="mt-4 flex items-baseline">
            <span class="text-4xl font-extrabold text-gray-900">{{ stats.totalEquipment || 0 }}</span>
            <span class="ml-2 text-sm font-medium text-gray-500">台</span>
        </div>
        <div class="mt-4 h-1 w-full bg-gray-100 rounded-full overflow-hidden">
             <div class="h-full bg-blue-500 rounded-full" style="width: 70%"></div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <h3 class="text-gray-400 text-xs font-semibold uppercase tracking-wider">告警设备</h3>
        <div class="mt-4 flex items-baseline">
            <span class="text-4xl font-extrabold text-gray-900">{{ stats.warningEquipment || 0 }}</span>
             <span class="ml-2 text-sm font-medium text-red-500">⚠️ 需要关注</span>
        </div>
        <div class="mt-4 h-1 w-full bg-gray-100 rounded-full overflow-hidden">
             <div class="h-full bg-red-500 rounded-full" style="width: 25%"></div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <h3 class="text-gray-400 text-xs font-semibold uppercase tracking-wider">系统运行率</h3>
        <div class="mt-4 flex items-baseline">
            <span class="text-4xl font-extrabold text-gray-900">{{ stats.uptime || '100%' }}</span>
             <span class="ml-2 text-sm font-medium text-green-500">稳定</span>
        </div>
         <div class="mt-4 h-1 w-full bg-gray-100 rounded-full overflow-hidden">
             <div class="h-full bg-green-500 rounded-full" style="width: 98%"></div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <h3 class="text-gray-400 text-xs font-semibold uppercase tracking-wider">活跃用户</h3>
         <div class="mt-4 flex items-baseline">
            <span class="text-4xl font-extrabold text-gray-900">{{ stats.activeUsers || 0 }}</span>
             <span class="ml-2 text-sm font-medium text-gray-500">人</span>
        </div>
         <div class="mt-4 h-1 w-full bg-gray-100 rounded-full overflow-hidden">
             <div class="h-full bg-indigo-500 rounded-full" style="width: 40%"></div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import Layout from '../components/Layout.vue'

const stats = ref({})

const fetchStats = async () => {
    try {
        const response = await axios.get('/api/dashboard/stats')
        stats.value = response.data
    } catch (error) {
        console.error("Failed to fetch stats", error)
    }
}

onMounted(() => {
    fetchStats()
})
</script>
