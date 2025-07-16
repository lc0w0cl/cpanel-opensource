// 检查后端服务状态的脚本
const http = require('http');

const checkBackendStatus = () => {
  console.log('正在检查后端服务状态...');
  
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: '/api/system/info',
    method: 'GET',
    timeout: 5000
  };

  const req = http.request(options, (res) => {
    console.log(`✅ 后端服务正在运行`);
    console.log(`   状态码: ${res.statusCode}`);
    console.log(`   状态文本: ${res.statusMessage}`);
    
    let data = '';
    res.on('data', (chunk) => {
      data += chunk;
    });
    
    res.on('end', () => {
      try {
        const jsonData = JSON.parse(data);
        console.log(`   响应类型: JSON`);
        console.log(`   响应成功: ${jsonData.success || false}`);
      } catch (e) {
        console.log(`   响应类型: ${data.includes('<!DOCTYPE') ? 'HTML' : 'TEXT'}`);
        console.log(`   响应内容: ${data.substring(0, 100)}...`);
      }
    });
  });

  req.on('error', (err) => {
    console.log(`❌ 后端服务连接失败:`);
    console.log(`   错误: ${err.message}`);
    console.log(`   建议: 请启动后端服务 (mvn spring-boot:run)`);
  });

  req.on('timeout', () => {
    console.log(`⏰ 后端服务响应超时`);
    console.log(`   建议: 检查服务是否正常运行`);
    req.destroy();
  });

  req.end();
};

// 检查2FA API
const check2FAApi = () => {
  console.log('\n正在检查2FA API状态...');
  
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: '/api/2fa/status',
    method: 'GET',
    timeout: 5000
  };

  const req = http.request(options, (res) => {
    console.log(`✅ 2FA API 可访问`);
    console.log(`   状态码: ${res.statusCode}`);
    console.log(`   状态文本: ${res.statusMessage}`);
    
    let data = '';
    res.on('data', (chunk) => {
      data += chunk;
    });
    
    res.on('end', () => {
      try {
        const jsonData = JSON.parse(data);
        console.log(`   响应类型: JSON`);
        console.log(`   响应成功: ${jsonData.success || false}`);
        console.log(`   2FA状态: ${JSON.stringify(jsonData.data || {})}`);
      } catch (e) {
        console.log(`   响应类型: ${data.includes('<!DOCTYPE') ? 'HTML' : 'TEXT'}`);
        console.log(`   响应内容: ${data.substring(0, 100)}...`);
        if (data.includes('<!DOCTYPE')) {
          console.log(`   ⚠️  返回HTML页面，可能是404或500错误`);
        }
      }
    });
  });

  req.on('error', (err) => {
    console.log(`❌ 2FA API 连接失败:`);
    console.log(`   错误: ${err.message}`);
  });

  req.on('timeout', () => {
    console.log(`⏰ 2FA API 响应超时`);
    req.destroy();
  });

  req.end();
};

// 执行检查
console.log('='.repeat(50));
console.log('后端服务状态检查');
console.log('='.repeat(50));

checkBackendStatus();

setTimeout(() => {
  check2FAApi();
}, 1000);

setTimeout(() => {
  console.log('\n' + '='.repeat(50));
  console.log('检查完成');
  console.log('='.repeat(50));
}, 2000);
