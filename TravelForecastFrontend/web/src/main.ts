import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'tailwindcss/tailwind.css'
import './assets/styles/main.css'
import './styles/dialog-form.css'
import App from './App.vue'
import router from './router'
import SvgIcon from './components/SvgIcon.vue'

// 创建Vue应用
const app = createApp(App)

// 使用插件
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.component('SvgIcon', SvgIcon)

// 挂载应用
app.mount('#app') 