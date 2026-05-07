<template>
  <Layout title="维护记录">
    <Toast ref="toastRef" />

    <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
       <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
         <h3 class="text-lg font-bold text-gray-800">维护日志</h3>
         <button @click="showModal = true" class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold shadow-md shadow-blue-200 transition-all hover:scale-105 active:scale-95">
           + 新增记录
         </button>
       </div>

       <div class="overflow-x-auto">
         <table class="min-w-full divide-y divide-gray-100">
            <thead class="bg-gray-50 text-gray-500">
              <tr>
                  <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">ID</th>
                  <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">技术员</th>
                  <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">描述</th>
                  <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">状态</th>
                  <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">日期</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-100">
                <tr v-for="item in list" :key="item.id" class="hover:bg-gray-50 transition-colors">
                    <td class="px-6 py-4 text-sm text-gray-500 font-mono">#{{ item.id }}</td>
                    <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ item.technician }}</td>
                    <td class="px-6 py-4 text-sm text-gray-500">{{ item.description }}</td>
                    <td class="px-6 py-4 text-sm">
                      <span :class="getStatusClass(item.status)" class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full border">
                        {{ item.status }}
                      </span>
                    </td>
                    <td class="px-6 py-4 text-sm text-gray-500">{{ new Date(item.maintenanceDate).toLocaleDateString() }}</td>
                </tr>
                <tr v-if="list.length === 0"><td colspan="5" class="px-6 py-12 text-center text-gray-400 italic">暂无记录</td></tr>
            </tbody>
         </table>
       </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm bg-black/30 transition-opacity">
        <div class="bg-white rounded-2xl w-full max-w-lg p-8 shadow-2xl">
            <h3 class="text-2xl font-bold mb-6 text-gray-800">新增维护记录</h3>
            <form @submit.prevent="createRecord">
                <div class="space-y-5">
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">技术员</label>
                        <input v-model="form.technician" type="text" required class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none">
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">描述</label>
                        <textarea v-model="form.description" required rows="3" class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none"></textarea>
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">状态</label>
                         <select v-model="form.status" class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none appearance-none">
                            <option value="进行中">进行中</option>
                            <option value="已完成">已完成</option>
                        </select>
                    </div>
                </div>
                <div class="mt-8 flex justify-end space-x-3">
                    <button type="button" @click="showModal = false" class="px-5 py-2.5 text-gray-600 font-medium hover:bg-gray-100 rounded-xl transition-colors">取消</button>
                    <button type="submit" class="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl shadow-lg shadow-blue-200 transition-all transform active:scale-95">保存</button>
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

const list = ref([])
const showModal = ref(false)
const toastRef = ref(null)
const form = ref({
    technician: '',
    description: '',
    status: '已完成',
    equipmentId: 1
})

const fetchList = async () => {
    try {
        const res = await axios.get('/api/maintenance')
        list.value = res.data
    } catch(e) {
       console.error(e)
       toastRef.value?.add({ title: '错误', message: '获取记录失败', type: 'error' })
    }
}

const createRecord = async () => {
    try {
        await axios.post('/api/maintenance', form.value)
        showModal.value = false
        fetchList()
        toastRef.value?.add({ title: '成功', message: '记录已添加', type: 'success' })
    } catch(e) {
        toastRef.value?.add({ title: '失败', message: '添加记录失败', type: 'error' })
    }
}

const getStatusClass = (status) => {
    return status === '已完成'
       ? 'bg-green-100 text-green-700 border-green-200'
       : 'bg-blue-100 text-blue-700 border-blue-200'
}

onMounted(fetchList)
</script>
