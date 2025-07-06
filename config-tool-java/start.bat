@echo off
echo 正在启动配置文件修改工具...
echo.

:: 检查Java是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Java运行环境，请先安装Java JDK 8或更高版本
    pause
    exit /b 1
)

:: 检查jar文件是否存在
if not exist "target\config-tool-java-1.0.0.jar" (
    echo 错误: 找不到jar文件，请先运行 mvn clean package 打包项目
    pause
    exit /b 1
)

:: 启动应用
echo 启动中...
java -jar target\config-tool-java-1.0.0.jar

:: 如果程序异常退出，保持窗口打开
if %errorlevel% neq 0 (
    echo.
    echo 程序异常退出，错误代码: %errorlevel%
    pause
) 