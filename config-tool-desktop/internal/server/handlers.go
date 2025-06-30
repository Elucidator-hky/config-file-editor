package server

import (
	"encoding/json"
	"net/http"

	"config-tool-desktop/internal/template"

	"github.com/gorilla/mux"
)

// ResponseData 标准响应格式
type ResponseData struct {
	Data  interface{} `json:"data,omitempty"`
	Error string      `json:"error,omitempty"`
}

// FindMatchesRequest 查找匹配项请求
type FindMatchesRequest struct {
	FilePath string `json:"filePath"`
	Prefix   string `json:"prefix"`
	Suffix   string `json:"suffix"`
}

// ApplyChangesRequest 应用修改请求
type ApplyChangesRequest struct {
	Modifications []template.ModificationTask `json:"modifications"`
}

// handleGetTemplatesList 获取模板列表
func (s *APIServer) handleGetTemplatesList(w http.ResponseWriter, r *http.Request) {
	templates, err := s.templateManager.GetAll()
	if err != nil {
		s.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}

	s.writeSuccess(w, templates)
}

// handleGetTemplateDetail 获取模板详情（包含实时当前值）
// 实现文档3.4.1节描述的"读取模板并填充当前值"业务逻辑
func (s *APIServer) handleGetTemplateDetail(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id := vars["id"]

	// 1. 根据ID读取模板文件
	template, err := s.templateManager.GetByID(id)
	if err != nil {
		s.writeError(w, http.StatusNotFound, err.Error())
		return
	}

	// 2. 遍历Template.Items中的每一个ConfigItem
	for i := range template.Items {
		configItem := &template.Items[i]

		// 3. 对于每一个ConfigItem，读取其第一个FileTarget的当前值
		if len(configItem.Targets) > 0 {
			firstTarget := &configItem.Targets[0]

			// 4. 调用FileProcessor的GetValue方法
			currentValue, err := s.fileProcessor.GetValue(firstTarget)
			if err != nil {
				// 如果读取失败，设置为空值，不中断整个流程
				configItem.CurrentValue = ""
			} else {
				// 5. 将返回的值赋给ConfigItem.CurrentValue字段
				configItem.CurrentValue = currentValue
			}
		}
	}

	// 6. 返回包含CurrentValue的Template对象
	s.writeSuccess(w, template)
}

// handleSaveTemplate 创建或更新模板
// 实现文档3.4.2节描述的"保存模板"业务逻辑
func (s *APIServer) handleSaveTemplate(w http.ResponseWriter, r *http.Request) {
	// 1. 接收HTTP请求，反序列化为Template对象
	var template template.Template
	if err := json.NewDecoder(r.Body).Decode(&template); err != nil {
		s.writeError(w, http.StatusBadRequest, "请求数据格式错误")
		return
	}

	// 2-4. 检查ID字段，生成UUID等逻辑在TemplateManager.Save中处理
	// 5. 调用templateManager.Save方法
	err := s.templateManager.Save(&template)
	if err != nil {
		s.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}

	// 6. 返回保存后的Template对象（带有生成的ID）
	s.writeSuccess(w, template)
}

// handleDeleteTemplate 删除模板
func (s *APIServer) handleDeleteTemplate(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id := vars["id"]

	err := s.templateManager.Delete(id)
	if err != nil {
		s.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}

	s.writeSuccess(w, map[string]string{"message": "删除成功"})
}

// handleFindMatches 查找文件中的匹配项
func (s *APIServer) handleFindMatches(w http.ResponseWriter, r *http.Request) {
	var req FindMatchesRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		s.writeError(w, http.StatusBadRequest, "请求数据格式错误")
		return
	}

	matches, err := s.fileProcessor.FindMatches(req.FilePath, req.Prefix, req.Suffix)
	if err != nil {
		s.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}

	s.writeSuccess(w, matches)
}

// handleApplyChanges 执行所有修改
// 实现文档3.4.3节描述的"执行文件修改"业务逻辑
func (s *APIServer) handleApplyChanges(w http.ResponseWriter, r *http.Request) {
	var req ApplyChangesRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		s.writeError(w, http.StatusBadRequest, "请求数据格式错误")
		return
	}

	err := s.fileProcessor.ApplyModifications(req.Modifications)
	if err != nil {
		s.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}

	s.writeSuccess(w, map[string]string{"message": "修改应用成功"})
}

// writeSuccess 写入成功响应
func (s *APIServer) writeSuccess(w http.ResponseWriter, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(ResponseData{Data: data})
}

// writeError 写入错误响应
func (s *APIServer) writeError(w http.ResponseWriter, statusCode int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	json.NewEncoder(w).Encode(ResponseData{Error: message})
}
