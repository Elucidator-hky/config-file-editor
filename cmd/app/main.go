package main

import (
	"fmt"
	"log"
	"os/exec"
	"runtime"

	"config-tool/internal/processor"
	"config-tool/internal/server"
	"config-tool/internal/template"
)

func main() {
	fmt.Println("配置文件修改工具启动中...")

	// 初始化模板管理器
	templateManager := template.NewTemplateManager("./data/templates")

	// 初始化文件处理器
	fileProcessor := processor.NewFileProcessor()

	// 初始化API服务器
	apiServer := server.NewAPIServer(":8080", templateManager, fileProcessor)

	// 启动浏览器
	go openBrowser("http://localhost:8080")

	fmt.Println("服务已启动在 http://localhost:8080")
	fmt.Println("按 Ctrl+C 停止服务")

	// 启动服务器
	if err := apiServer.Start(); err != nil {
		log.Fatal("启动服务器失败:", err)
	}
}

// openBrowser 在默认浏览器中打开URL
func openBrowser(url string) {
	var err error

	switch runtime.GOOS {
	case "linux":
		err = exec.Command("xdg-open", url).Start()
	case "windows":
		err = exec.Command("rundll32", "url.dll,FileProtocolHandler", url).Start()
	case "darwin":
		err = exec.Command("open", url).Start()
	default:
		err = fmt.Errorf("不支持的操作系统")
	}

	if err != nil {
		fmt.Printf("无法打开浏览器: %v\n", err)
		fmt.Printf("请手动打开: %s\n", url)
	}
} 