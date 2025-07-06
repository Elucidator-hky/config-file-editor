@echo off
java -jar target\config-tool-java-1.0.0.jar

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo 应用程序启动失败，错误代码: %ERRORLEVEL%
    echo 请检查Java环境是否正确安装。
    pause
) 