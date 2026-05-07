<template>
  <Layout title="报表导出">
    <Toast ref="toastRef" />

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-start hover:shadow-md transition-shadow">
            <div class="p-3 bg-green-100 rounded-xl mb-4">
               <span class="text-2xl">📊</span>
            </div>
            <h3 class="text-xl font-bold text-gray-800 mb-2">设备清单报表</h3>
            <p class="text-gray-500 mb-6 leading-relaxed">导出系统中所有设备的详细信息，包括当前状态、位置、型号及最近维护时间。</p>
            <button @click="exportEquipment" class="px-6 py-2.5 bg-green-600 text-white font-semibold rounded-xl hover:bg-green-700 transition-colors shadow-lg shadow-green-200 w-full md:w-auto text-center">
                下载 CSV
            </button>
        </div>

        <!-- Placeholder for future reports -->
        <div class="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-start hover:shadow-md transition-shadow opacity-60">
            <div class="p-3 bg-gray-100 rounded-xl mb-4">
               <span class="text-2xl">📉</span>
            </div>
            <h3 class="text-xl font-bold text-gray-800 mb-2">维护统计 (即将推出)</h3>
            <p class="text-gray-500 mb-6 leading-relaxed">按月度或季度导出维护记录统计分析报表。</p>
            <button disabled class="px-6 py-2.5 bg-gray-200 text-gray-400 font-semibold rounded-xl cursor-not-allowed w-full md:w-auto text-center">
                暂不可用
            </button>
        </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import Layout from '../components/Layout.vue'
import Toast from '../components/Toast.vue'

const toastRef = ref(null)

const exportEquipment = async () => {
    try {
        const response = await axios.get('/api/reports/export', { responseType: 'blob' })
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'equipment_report.csv')
        document.body.appendChild(link)
        link.click()
        toastRef.value?.add({ title: '成功', message: '报表已开始下载', type: 'success' })
    } catch (e) {
        console.error(e)
        toastRef.value?.add({ title: '失败', message: '导出失败', type: 'error' })
    }
}
</script>
