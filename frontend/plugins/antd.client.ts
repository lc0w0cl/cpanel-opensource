import { DatePicker, ConfigProvider } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import updateLocale from 'dayjs/plugin/updateLocale'

// 配置 dayjs 插件和中文语言
dayjs.extend(updateLocale)
dayjs.locale('zh-cn')

// 自定义中文配置
dayjs.updateLocale('zh-cn', {
  weekStart: 1, // 周一作为一周的开始
})

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.use(DatePicker)
  nuxtApp.vueApp.use(ConfigProvider)

  // 提供全局配置
  nuxtApp.vueApp.provide('antdLocale', zhCN)
})
