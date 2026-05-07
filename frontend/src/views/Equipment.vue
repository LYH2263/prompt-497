<template>
  <Layout title="设备管理">
    <!-- Toast Container -->
    <Toast ref="toastRef" />

    <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
         <h3 class="text-lg font-bold text-gray-800">设备列表</h3>
         <button @click="showAddModal = true" class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold shadow-md shadow-blue-200 transition-all hover:scale-105 active:scale-95">
           + 新增设备
         </button>
      </div>

      <!-- Table -->
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-100">
          <thead class="bg-gray-50 text-gray-500">
            <tr>
              <th scope="col" class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">ID</th>
              <th scope="col" class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">设备名称</th>
              <th scope="col" class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">型号</th>
              <th scope="col" class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">位置</th>
              <th scope="col" class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">状态</th>
              <th scope="col" class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">上次维护</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-100">
            <tr v-for="item in equipmentList" :key="item.id" class="hover:bg-gray-50 transition-colors">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono">#{{ item.id }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ item.name }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ item.type }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ item.location }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <span :class="getStatusClass(item.status)" class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full border">
                  {{ item.status }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ item.lastMaintenance ? new Date(item.lastMaintenance).toLocaleDateString() : '尚未维护' }}</td>
            </tr>
             <tr v-if="equipmentList.length === 0">
                <td colspan="6" class="px-6 py-12 text-center text-gray-400 italic">暂无数据</td>
             </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showAddModal" class="fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm bg-black/30 transition-opacity">
        <div class="bg-white rounded-2xl w-full max-w-lg p-8 shadow-2xl transform transition-all scale-100">
            <h3 class="text-2xl font-bold mb-6 text-gray-800">新增设备</h3>
            <form @submit.prevent="createEquipment">
                <div class="space-y-5">
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">设备名称</label>
                        <input v-model="newItem.name" type="text" required class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none">
                    </div>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-sm font-semibold text-gray-700 mb-1">型号</label>
                            <input v-model="newItem.type" type="text" class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none">
                        </div>
                        <div>
                            <label class="block text-sm font-semibold text-gray-700 mb-1">位置</label>
                            <input v-model="newItem.location" type="text" class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none">
                        </div>
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">初始状态</label>
                        <select v-model="newItem.status" class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none appearance-none">
                            <option value="正常运行">正常运行</option>
                            <option value="故障报警">故障报警</option>
                            <option value="维护中">维护中</option>
                        </select>
                    </div>
                </div>
                <div class="mt-8 flex justify-end space-x-3">
                    <button type="button" @click="showAddModal = false" class="px-5 py-2.5 text-gray-600 font-medium hover:bg-gray-100 rounded-xl transition-colors">
                        取消
                    </button>
                    <button type="submit" class="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl shadow-lg shadow-blue-200 transition-all transform active:scale-95">
                        保存设备
                    </button>
                </div>
            </form>
        </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import Layout from '../components/Layout.vue'
import Toast from '../components/Toast.vue'

const equipmentList = ref([])
const showAddModal = ref(false)
const toastRef = ref(null)
const newItem = ref({
    name: '',
    type: '',
    location: '',
    status: '正常运行'
})

const fetchEquipment = async () => {
    try {
        const response = await axios.get('/api/equipment')
        equipmentList.value = response.data
    } catch (error) {
        console.error("Failed to fetch equipment", error)
        toastRef.value?.add({ title: '加载失败', message: '无法获取设备列表', type: 'error' })
    }
}

const createEquipment = async () => {
    try {
        await axios.post('/api/equipment', newItem.value)
        showAddModal.value = false
        newItem.value = { name: '', type: '', location: '', status: '正常运行' }
        fetchEquipment()
        toastRef.value?.add({ title: '成功', message: '设备已创建', type: 'success' })
    } catch (error) {
        toastRef.value?.add({ title: '失败', message: '创建设备失败', type: 'error' })
    }
}

const getStatusClass = (status) => {
    switch (status) {
        case '正常运行':
            return 'bg-green-100 text-green-700 border-green-200'
        case '故障报警':
            return 'bg-red-100 text-red-700 border-red-200'
        case '维护中':
            return 'bg-amber-100 text-amber-700 border-amber-200'
        default:
            return 'bg-gray-100 text-gray-700 border-gray-200'
    }
}

onMounted(() => {
    fetchEquipment()
})
</script>
