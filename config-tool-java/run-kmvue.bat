@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo KMVUE配置修改工具
echo ========================================
echo.

REM 设置变量
set "NACOS_URL=http://localhost:8848"
set "NACOS_BIN_PATH=..\nacos\bin"

REM 检查Java环境
echo 1. 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误：未找到Java运行环境，请先安装Java
    pause
    exit /b 1
)
echo    Java环境检查通过

REM 检查jar文件
echo 2. 检查程序文件...
if not exist "config-tool-java-1.0.0.jar" (
    echo 错误：未找到config-tool-java-1.0.0.jar文件
    pause
    exit /b 1
)
echo    程序文件检查通过

REM 检查Nacos状态
echo 3. 检查Nacos服务状态...
curl -s -o nul -w "%%{http_code}" "%NACOS_URL%/nacos/v1/ns/operator/servers" 2>nul | findstr "200" >nul
if %errorlevel% equ 0 (
    echo    Nacos服务已运行
    goto :start_app
) else (
    echo    Nacos服务未运行，尝试启动...
)

REM 尝试启动Nacos
echo 4. 启动Nacos服务...
if exist "%NACOS_BIN_PATH%\startup.cmd" (
    echo    正在后台启动Nacos服务...
    
    REM 在完全独立的进程中启动Nacos（完全隐藏窗口）
    powershell -WindowStyle Hidden -Command "Start-Process -FilePath '%NACOS_BIN_PATH%\startup.cmd' -WorkingDirectory '%NACOS_BIN_PATH%' -WindowStyle Hidden" >nul 2>&1
    
    echo    等待Nacos服务启动（最多等待5秒）...
    
    REM 等待Nacos启动完成，但有超时限制
    set /a NACOS_WAIT_COUNT=0
    :WAIT_nacos_Start
    timeout /t 1 > nul
    set /a NACOS_WAIT_COUNT+=1
    
    curl -s -o nul -w "%%{http_code}" "%NACOS_URL%/nacos/v1/ns/operator/servers" 2>nul | findstr "200" >nul
    
    if !errorlevel! EQU 0 (
        echo    Nacos服务启动成功
        goto :start_app
    ) ELSE (
        if !NACOS_WAIT_COUNT! LSS 5 (
            echo    等待Nacos启动中...
            goto :WAIT_nacos_Start
        ) else (
            echo    Nacos启动超时，跳过Nacos连接，直接启动应用
            goto :start_app
        )
    )
    
) else (
    echo    未找到Nacos启动脚本：%NACOS_BIN_PATH%\startup.cmd
    echo    请确保nacos文件夹位于上级目录中，或手动启动Nacos服务
    echo    程序将继续运行，但Nacos功能可能不可用
)

:start_app
echo.
echo 5. 启动KMVUE配置修改界面...
start /MIN javaw -jar config-tool-java-1.0.0.jar kmvue

echo.
echo ========================================
echo KMVUE配置修改工具启动完成
echo ========================================
echo 程序已在后台运行，等待应用完全启动...
echo Nacos控制台：%NACOS_URL%/nacos
echo.

REM 等待Java应用启动完成
echo 等待应用启动中...
timeout /t 3 /nobreak >nul

REM 检查Java进程是否存在（通过jar包名字判断）
set /a JAVA_WAIT_COUNT=0
:CHECK_JAVA_PROCESS
set /a JAVA_WAIT_COUNT+=1

wmic process where "CommandLine like '%%config-tool-java-1.0.0.jar%%'" get ProcessId 2>nul | find /v "ProcessId" | find /v "" >nul
if %errorlevel% equ 0 (
    echo 应用启动完成，窗口将自动关闭
    timeout /t 2 /nobreak >nul
    exit
) else (
    if !JAVA_WAIT_COUNT! LSS 10 (
        echo 等待Java应用启动...
        timeout /t 2 /nobreak >nul
        goto :CHECK_JAVA_PROCESS
    ) else (
        echo 应用启动超时，但已启动命令，窗口将关闭
        timeout /t 2 /nobreak >nul
        exit
    )
) 