@echo off

REM ========== 配置参数 ==========
REM 应用类型：dfm 或 kmvue （可直接编辑修改）
set APP_TYPE=kmvue
REM ================================

echo 启动配置工具 - 类型: %APP_TYPE%

java -jar config-tool-java-1.0.0.jar %APP_TYPE%

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo 应用程序启动失败，错误代码: %ERRORLEVEL%
    echo 请检查Java环境是否正确安装。
    pause
) 