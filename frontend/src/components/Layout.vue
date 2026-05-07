<template>
  <div class="min-h-screen bg-gray-50 flex font-sans">
    <!-- Sidebar -->
    <aside class="w-64 bg-slate-900 text-white flex-shrink-0 hidden md:flex flex-col shadow-xl">
      <div class="p-6 border-b border-slate-800">
        <h1 class="text-2xl font-bold bg-gradient-to-r from-blue-400 to-cyan-300 bg-clip-text text-transparent tracking-tight">MEMS 平台</h1>
      </div>
      <nav class="mt-6 flex-1 px-4 space-y-3">
        <router-link to="/dashboard" custom v-slot="{ href, navigate, isActive }">
          <a :href="href" @click="navigate" :class="[isActive ? 'bg-blue-600 shadow-lg text-white' : 'text-slate-400 hover:bg-slate-800 hover:text-white', 'flex items-center px-4 py-3 rounded-xl transition-all duration-300 group font-medium']">
             <span class="mr-3">📊</span> 系统概览
          </a>
        </router-link>
        <router-link to="/equipment" custom v-slot="{ href, navigate, isActive }">
          <a :href="href" @click="navigate" :class="[isActive ? 'bg-blue-600 shadow-lg text-white' : 'text-slate-400 hover:bg-slate-800 hover:text-white', 'flex items-center px-4 py-3 rounded-xl transition-all duration-300 group font-medium']">
             <span class="mr-3">🔧</span> 设备管理
          </a>
        </router-link>
        <router-link to="/maintenance" custom v-slot="{ href, navigate, isActive }">
          <a :href="href" @click="navigate" :class="[isActive ? 'bg-blue-600 shadow-lg text-white' : 'text-slate-400 hover:bg-slate-800 hover:text-white', 'flex items-center px-4 py-3 rounded-xl transition-all duration-300 group font-medium']">
             <span class="mr-3">🛡️</span> 维护记录
          </a>
        </router-link>
        <router-link to="/reports" custom v-slot="{ href, navigate, isActive }">
          <a :href="href" @click="navigate" :class="[isActive ? 'bg-blue-600 shadow-lg text-white' : 'text-slate-400 hover:bg-slate-800 hover:text-white', 'flex items-center px-4 py-3 rounded-xl transition-all duration-300 group font-medium']">
             <span class="mr-3">📥</span> 报表导出
          </a>
        </router-link>
        <router-link to="/users" custom v-slot="{ href, navigate, isActive }">
          <a :href="href" @click="navigate" :class="[isActive ? 'bg-blue-600 shadow-lg text-white' : 'text-slate-400 hover:bg-slate-800 hover:text-white', 'flex items-center px-4 py-3 rounded-xl transition-all duration-300 group font-medium']">
             <span class="mr-3">👥</span> 权限管理
          </a>
        </router-link>
      </nav>
      <div class="p-4 border-t border-slate-800">
         <div class="flex items-center space-x-3 text-sm text-slate-400">
            <div class="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center">A</div>
            <div>
               <p class="text-white font-medium">管理员</p>
               <p class="text-xs">在线</p>
            </div>
         </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col overflow-hidden relative">
      <!-- Header -->
      <header class="bg-white/80 backdrop-blur-md sticky top-0 z-30 shadow-sm border-b border-gray-100">
        <div class="px-8 py-4 flex justify-between items-center">
          <h2 class="text-2xl font-bold text-gray-800 tracking-tight">{{ title }}</h2>
          <div class="flex items-center space-x-4">
             <button @click="handleLogout" class="px-5 py-2 text-sm font-medium text-red-500 hover:bg-red-50 hover:text-red-600 rounded-lg transition-colors border border-transparent hover:border-red-100">
                退出登录
             </button>
          </div>
        </div>
      </header>

      <!-- Content -->
      <main class="flex-1 p-8 overflow-y-auto">
         <slot></slot>
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  title: {
    type: String,
    default: 'MEMS'
  }
})

const router = useRouter()
const handleLogout = () => {
    // Clear local storage if any (optional)
    router.push('/login')
}
</script>
