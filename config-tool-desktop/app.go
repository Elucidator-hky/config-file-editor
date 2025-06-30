package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"config-tool-desktop/internal/processor"
	"config-tool-desktop/internal/template"
)

// App struct
type App struct {
	ctx             context.Context
	templateManager *template.TemplateManager
	fileProcessor   *processor.FileProcessor
}

// NewApp creates a new App application struct
func NewApp() *App {
	// 初始化模板管理器
	templateManager := template.NewTemplateManager("./data/templates")

	// 初始化文件处理器
	fileProcessor := processor.NewFileProcessor()

	return &App{
		templateManager: templateManager,
		fileProcessor:   fileProcessor,
	}
}

// startup is called when the app starts. The context is saved
// so we can call the runtime methods
func (a *App) startup(ctx context.Context) {
	a.ctx = ctx
	log.Println("配置文件修改工具启动中...")
}

// APIResponse 通用API响应结构
type APIResponse struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data,omitempty"`
	Error   string      `json:"error,omitempty"`
}

// GetTemplatesList 获取模板列表
func (a *App) GetTemplatesList() string {
	templates, err := a.templateManager.GetAll()
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   err.Error(),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	response := APIResponse{
		Success: true,
		Data:    templates,
	}
	jsonData, _ := json.Marshal(response)
	return string(jsonData)
}

// GetTemplateDetail 获取模板详情
func (a *App) GetTemplateDetail(templateID string) string {
	tmpl, err := a.templateManager.GetByID(templateID)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   err.Error(),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	// 填充当前值 - 遍历配置项的目标
	for i := range tmpl.Items {
		configItem := &tmpl.Items[i]
		if len(configItem.Targets) > 0 {
			firstTarget := &configItem.Targets[0]
			currentValue, err := a.fileProcessor.GetValue(firstTarget)
			if err != nil {
				configItem.CurrentValue = ""
			} else {
				configItem.CurrentValue = currentValue
			}
		}
	}

	response := APIResponse{
		Success: true,
		Data:    tmpl,
	}
	jsonData, _ := json.Marshal(response)
	return string(jsonData)
}

// SaveTemplate 保存模板
func (a *App) SaveTemplate(templateJSON string) string {
	var tmpl template.Template
	err := json.Unmarshal([]byte(templateJSON), &tmpl)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   fmt.Sprintf("解析模板数据失败: %v", err),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	err = a.templateManager.Save(&tmpl)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   err.Error(),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	response := APIResponse{
		Success: true,
		Data:    tmpl,
	}
	jsonData, _ := json.Marshal(response)
	return string(jsonData)
}

// DeleteTemplate 删除模板
func (a *App) DeleteTemplate(templateID string) string {
	err := a.templateManager.Delete(templateID)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   err.Error(),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	response := APIResponse{
		Success: true,
	}
	jsonData, _ := json.Marshal(response)
	return string(jsonData)
}

// FindMatchesRequest 查找匹配请求结构
type FindMatchesRequest struct {
	FilePath string `json:"filePath"`
	Prefix   string `json:"prefix"`
	Suffix   string `json:"suffix"`
}

// FindMatches 查找匹配项
func (a *App) FindMatches(requestJSON string) string {
	var req FindMatchesRequest
	err := json.Unmarshal([]byte(requestJSON), &req)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   fmt.Sprintf("解析请求数据失败: %v", err),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	matches, err := a.fileProcessor.FindMatches(req.FilePath, req.Prefix, req.Suffix)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   err.Error(),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	response := APIResponse{
		Success: true,
		Data:    matches,
	}
	jsonData, _ := json.Marshal(response)
	return string(jsonData)
}

// ApplyChangesRequest 应用更改请求结构
type ApplyChangesRequest struct {
	TemplateID string                 `json:"templateId"`
	Values     map[string]interface{} `json:"values"`
}

// ApplyChanges 应用配置更改
func (a *App) ApplyChanges(requestJSON string) string {
	var req ApplyChangesRequest
	err := json.Unmarshal([]byte(requestJSON), &req)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   fmt.Sprintf("解析请求数据失败: %v", err),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	// 获取模板
	tmpl, err := a.templateManager.GetByID(req.TemplateID)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   err.Error(),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	// 构建修改任务 - 需要根据模板和用户输入构建ModificationTask列表
	var modifications []template.ModificationTask
	for _, item := range tmpl.Items {
		if value, exists := req.Values[item.Name]; exists {
			for i := range item.Targets {
				modifications = append(modifications, template.ModificationTask{
					Target:   &item.Targets[i],
					NewValue: fmt.Sprintf("%v", value),
				})
			}
		}
	}

	// 应用更改
	err = a.fileProcessor.ApplyModifications(modifications)
	if err != nil {
		response := APIResponse{
			Success: false,
			Error:   err.Error(),
		}
		jsonData, _ := json.Marshal(response)
		return string(jsonData)
	}

	response := APIResponse{
		Success: true,
		Data:    map[string]string{"message": "修改应用成功"},
	}
	jsonData, _ := json.Marshal(response)
	return string(jsonData)
}
