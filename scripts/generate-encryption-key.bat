@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo === CPanel AES加密密钥生成工具 ===
echo.

REM 检查是否安装了PowerShell
powershell -Command "Get-Command" >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到PowerShell
    echo 请确保系统已安装PowerShell
    pause
    exit /b 1
)

echo 🔑 正在生成AES-256加密密钥...

REM 使用PowerShell生成32字节随机密钥并进行Base64编码
for /f "delims=" %%i in ('powershell -Command "[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))"') do set AES_KEY=%%i

echo.
echo ✅ 密钥生成成功！
echo.
echo 📋 请将以下密钥添加到您的环境变量中:
echo.
echo ENCRYPTION_AES_KEY=!AES_KEY!
echo.
echo 📝 使用方法:
echo.
echo 1. 在 .env 文件中添加:
echo    ENCRYPTION_AES_KEY=!AES_KEY!
echo.
echo 2. 或者在 docker-compose.yml 中设置环境变量:
echo    environment:
echo      ENCRYPTION_AES_KEY: !AES_KEY!
echo.
echo 3. 或者在系统环境变量中设置:
echo    set ENCRYPTION_AES_KEY=!AES_KEY!
echo.
echo ⚠️  重要提醒:
echo    - 请妥善保管此密钥，丢失后将无法解密已加密的数据
echo    - 不要在日志或版本控制系统中暴露此密钥
echo    - 建议定期备份密钥到安全位置
echo.

REM 检查是否存在.env文件
if exist ".env" (
    echo 🔍 检测到 .env 文件存在
    set /p "choice=是否要将密钥自动添加到 .env 文件中? (y/N): "
    
    if /i "!choice!"=="y" (
        REM 检查是否已存在ENCRYPTION_AES_KEY配置
        findstr /b "ENCRYPTION_AES_KEY=" .env >nul 2>&1
        if !errorlevel! equ 0 (
            echo ⚠️  .env 文件中已存在 ENCRYPTION_AES_KEY 配置
            set /p "overwrite=是否要覆盖现有配置? (y/N): "
            
            if /i "!overwrite!"=="y" (
                REM 创建临时文件替换现有配置
                powershell -Command "(Get-Content .env) -replace '^ENCRYPTION_AES_KEY=.*', 'ENCRYPTION_AES_KEY=!AES_KEY!' | Set-Content .env.tmp"
                move .env.tmp .env >nul
                echo ✅ 已更新 .env 文件中的加密密钥
            ) else (
                echo ❌ 已取消更新
            )
        ) else (
            REM 添加新配置
            echo. >> .env
            echo # AES加密密钥 >> .env
            echo ENCRYPTION_AES_KEY=!AES_KEY! >> .env
            echo ✅ 已将加密密钥添加到 .env 文件
        )
    ) else (
        echo ❌ 已跳过自动添加到 .env 文件
    )
) else (
    echo 💡 提示: 未找到 .env 文件，请手动创建并添加密钥配置
)

echo.
echo 🎉 密钥生成完成！
echo.
pause
