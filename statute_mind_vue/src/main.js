import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initMonitor } from '@/monitor/metrics'
import './styles/main.css'

initMonitor()
createApp(App).use(router).mount('#app')
