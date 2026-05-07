import axios from 'axios'
import { createApp } from 'vue'
import './assets/base.css'
import App from './App.vue'
import router from './router'

axios.defaults.withCredentials = true

const app = createApp(App)

app.use(router)

app.mount('#app')
