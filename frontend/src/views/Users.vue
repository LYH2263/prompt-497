<template>
  <Layout title="权限管理">
    <Toast ref="toastRef" />

    <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
           <h3 class="text-lg font-bold text-gray-800">用户权限管理</h3>
           <button @click="showModal = true" class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold shadow-md shadow-blue-200 transition-all hover:scale-105 active:scale-95">
               + 新增用户
           </button>
        </div>

         <div class="overflow-x-auto">
           <table class="min-w-full divide-y divide-gray-100">
              <thead class="bg-gray-50 text-gray-500">
                <tr>
                    <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">ID</th>
                    <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">用户名</th>
                    <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">角色</th>
                    <th class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider">操作</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-100">
                  <tr v-for="u in list" :key="u.id" class="hover:bg-gray-50 transition-colors">
                      <td class="px-6 py-4 text-sm text-gray-500 font-mono">#{{ u.id }}</td>
                      <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ u.username }}</td>
                      <td class="px-6 py-4 text-sm text-gray-500">
                          <span :class="u.role === 'ADMIN' ? 'bg-purple-100 text-purple-700 border-purple-200' : 'bg-gray-100 text-gray-700 border-gray-200'" class="px-2 py-0.5 rounded text-xs font-semibold border">
                              {{ u.role }}
                          </span>
                      </td>
                      <td class="px-6 py-4 text-sm">
                          <button @click="deleteUser(u.id)" class="text-red-500 hover:text-red-700 font-medium transition-colors">删除</button>
                      </td>
                  </tr>
              </tbody>
           </table>
         </div>
    </div>

     <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm bg-black/30 transition-opacity">
        <div class="bg-white rounded-2xl w-full max-w-lg p-8 shadow-2xl">
            <h3 class="text-2xl font-bold mb-6 text-gray-800">新增用户</h3>
            <form @submit.prevent="createUser">
                <div class="space-y-5">
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">用户名</label>
                        <input v-model="form.username" type="text" required class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none">
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">密码</label>
                        <input v-model="form.password" type="password" required class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none">
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1">角色</label>
                         <select v-model="form.role" class="block w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors outline-none appearance-none">
                             <option value="USER">普通用户</option>
                            <option value="ADMIN">管理员</option>
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
const form = ref({ username: '', password: '', role: 'USER' })

const fetchList = async () => {
    try {
        const res = await axios.get('/api/users')
        list.value = res.data
    } catch(e) { console.error(e) }
}

const createUser = async () => {
    try {
        await axios.post('/api/users', form.value)
        showModal.value = false
        form.value = { username: '', password: '', role: 'USER' }
        fetchList()
        toastRef.value?.add({ title: '成功', message: '用户已创建', type: 'success' })
    } catch(e) {
        toastRef.value?.add({ title: '失败', message: '创建失败', type: 'error' })
    }
}

const deleteUser = async (id) => {
    if (!confirm('确定删除?')) return
    try {
        await axios.delete(`/api/users/${id}`)
        fetchList()
        toastRef.value?.add({ title: '成功', message: '用户已删除', type: 'success' })
    } catch(e) {
        toastRef.value?.add({ title: '失败', message: '删除失败', type: 'error' })
    }
}

onMounted(fetchList)
</script>
