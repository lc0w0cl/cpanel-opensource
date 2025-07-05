/**
 * 测试URL链接下载功能
 * 运行方法：node test-url-download.js
 */

const API_BASE_URL = 'http://localhost:8080/api';

// 测试B站URL解析
async function testBilibiliUrl() {
    console.log('=== 测试B站URL解析 ===');
    
    const testUrl = 'https://www.bilibili.com/video/BV1aiT7zTEWD/';
    
    const requestBody = {
        query: testUrl,
        searchType: 'url',
        platform: 'bilibili',
        page: 1,
        pageSize: 20
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/music/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        
        const result = await response.json();
        
        console.log('请求URL:', testUrl);
        console.log('响应状态:', response.status);
        console.log('响应结果:', JSON.stringify(result, null, 2));
        
        if (result.success && result.data && result.data.length > 0) {
            const video = result.data[0];
            console.log('✅ B站URL解析成功');
            console.log(`   标题: ${video.title}`);
            console.log(`   UP主: ${video.artist}`);
            console.log(`   时长: ${video.duration}`);
            console.log(`   平台: ${video.platform}`);
        } else {
            console.log('❌ B站URL解析失败');
        }
        
    } catch (error) {
        console.error('❌ 请求失败:', error.message);
    }
    
    console.log('');
}

// 测试YouTube URL解析
async function testYouTubeUrl() {
    console.log('=== 测试YouTube URL解析 ===');
    
    const testUrl = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';
    
    const requestBody = {
        query: testUrl,
        searchType: 'url',
        platform: 'youtube',
        page: 1,
        pageSize: 20
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/music/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        
        const result = await response.json();
        
        console.log('请求URL:', testUrl);
        console.log('响应状态:', response.status);
        console.log('响应结果:', JSON.stringify(result, null, 2));
        
        if (result.success && result.data && result.data.length > 0) {
            const video = result.data[0];
            console.log('✅ YouTube URL解析成功');
            console.log(`   标题: ${video.title}`);
            console.log(`   作者: ${video.artist}`);
            console.log(`   时长: ${video.duration}`);
            console.log(`   平台: ${video.platform}`);
        } else {
            console.log('❌ YouTube URL解析失败');
        }
        
    } catch (error) {
        console.error('❌ 请求失败:', error.message);
    }
    
    console.log('');
}

// 测试关键词搜索（确保原有功能不受影响）
async function testKeywordSearch() {
    console.log('=== 测试关键词搜索 ===');
    
    const requestBody = {
        query: '周杰伦',
        searchType: 'keyword',
        platform: 'bilibili',
        page: 1,
        pageSize: 5
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/music/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        
        const result = await response.json();
        
        console.log('搜索关键词:', requestBody.query);
        console.log('响应状态:', response.status);
        
        if (result.success && result.data && result.data.length > 0) {
            console.log(`✅ 关键词搜索成功，找到 ${result.data.length} 个结果`);
            result.data.forEach((video, index) => {
                console.log(`   ${index + 1}. ${video.title} - ${video.artist}`);
            });
        } else {
            console.log('❌ 关键词搜索失败');
            console.log('响应结果:', JSON.stringify(result, null, 2));
        }
        
    } catch (error) {
        console.error('❌ 请求失败:', error.message);
    }
    
    console.log('');
}

// 检查服务器是否运行
async function checkServer() {
    console.log('=== 检查服务器状态 ===');
    
    try {
        const response = await fetch(`${API_BASE_URL}/music/test-search?query=test`);
        const result = await response.json();
        
        if (response.status === 200) {
            console.log('✅ 服务器运行正常');
        } else {
            console.log('⚠️ 服务器响应异常:', response.status);
        }
        
    } catch (error) {
        console.error('❌ 无法连接到服务器:', error.message);
        console.log('请确保后端服务已启动 (mvn spring-boot:run)');
        return false;
    }
    
    console.log('');
    return true;
}

// 主测试函数
async function runTests() {
    console.log('开始测试URL链接下载功能...\n');
    
    // 检查服务器状态
    const serverOk = await checkServer();
    if (!serverOk) {
        return;
    }
    
    // 运行测试
    await testBilibiliUrl();
    await testYouTubeUrl();
    await testKeywordSearch();
    
    console.log('测试完成！');
}

// 运行测试
runTests().catch(console.error);
