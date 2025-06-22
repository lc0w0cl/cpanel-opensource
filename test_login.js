// 简单的登录测试脚本
const API_BASE_URL = 'http://localhost:8080/api'

async function testLogin() {
  try {
    console.log('测试登录API...')
    
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        password: 'admin'
      })
    })
    
    console.log('响应状态:', response.status)
    console.log('响应头:', Object.fromEntries(response.headers.entries()))
    
    const result = await response.json()
    console.log('响应数据:', result)
    
    if (result.success) {
      console.log('✅ 登录成功!')
      console.log('Access Token:', result.data.accessToken.substring(0, 50) + '...')
      console.log('Refresh Token:', result.data.refreshToken.substring(0, 50) + '...')
    } else {
      console.log('❌ 登录失败:', result.message)
    }
    
  } catch (error) {
    console.error('❌ 请求失败:', error.message)
  }
}

// 运行测试
testLogin()
