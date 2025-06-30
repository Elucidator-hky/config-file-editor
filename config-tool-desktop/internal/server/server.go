package server

import (
	"net/http"

	"config-tool-desktop/internal/processor"
	"config-tool-desktop/internal/template"

	"github.com/gorilla/mux"
)

// APIServer Web服务器
type APIServer struct {
	listenAddr      string
	templateManager *template.TemplateManager
	fileProcessor   *processor.FileProcessor
}

// NewAPIServer 创建API服务器
func NewAPIServer(addr string, tm *template.TemplateManager, fp *processor.FileProcessor) *APIServer {
	return &APIServer{
		listenAddr:      addr,
		templateManager: tm,
		fileProcessor:   fp,
	}
}

// Start 启动HTTP服务器
func (s *APIServer) Start() error {
	router := mux.NewRouter()

	// 注册API路由
	api := router.PathPrefix("/api").Subrouter()

	// 模板接口
	api.HandleFunc("/templates", s.handleGetTemplatesList).Methods("GET")
	api.HandleFunc("/templates/{id}", s.handleGetTemplateDetail).Methods("GET")
	api.HandleFunc("/templates", s.handleSaveTemplate).Methods("POST")
	api.HandleFunc("/templates/{id}", s.handleSaveTemplate).Methods("PUT")
	api.HandleFunc("/templates/{id}", s.handleDeleteTemplate).Methods("DELETE")

	// 文件处理接口
	api.HandleFunc("/files/find-matches", s.handleFindMatches).Methods("POST")

	// 应用接口
	api.HandleFunc("/apply", s.handleApplyChanges).Methods("POST")

	// 静态文件服务
	router.PathPrefix("/").Handler(http.FileServer(http.Dir("./web/dist/")))

	// 启动服务器
	return http.ListenAndServe(s.listenAddr, router)
}
