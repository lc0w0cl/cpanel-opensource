/**
 * 壁纸预加载插件
 * 在客户端启动时预加载壁纸配置，减少页面跳跃
 */

export default defineNuxtPlugin(async () => {
  // 只在客户端运行
  if (process.client) {
    const { initializeWallpaper } = useWallpaper()
    
    try {
      // 预加载壁纸配置
      await initializeWallpaper()
      console.log('壁纸配置预加载完成')
    } catch (error) {
      console.warn('壁纸配置预加载失败:', error)
    }
  }
})
