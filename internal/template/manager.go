package template

import (
	"encoding/json"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"strings"

	"github.com/google/uuid"
)

// TemplateManager 模板管理器
type TemplateManager struct {
	templatesDir string
}

// NewTemplateManager 创建模板管理器
func NewTemplateManager(dir string) *TemplateManager {
	// 确保目录存在
	os.MkdirAll(dir, 0755)
	return &TemplateManager{
		templatesDir: dir,
	}
}

// GetAll 获取所有模板列表
func (tm *TemplateManager) GetAll() ([]Template, error) {
	var templates []Template

	err := filepath.WalkDir(tm.templatesDir, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}

		if d.IsDir() || !strings.HasSuffix(path, ".json") {
			return nil
		}

		template, err := tm.loadTemplate(path)
		if err != nil {
			return err
		}

		// 返回完整的模板信息，包含配置项
		templates = append(templates, *template)

		return nil
	})

	return templates, err
}

// GetByID 根据ID获取模板详情
func (tm *TemplateManager) GetByID(id string) (*Template, error) {
	filePath := filepath.Join(tm.templatesDir, id+".json")
	return tm.loadTemplate(filePath)
}

// Save 保存模板
func (tm *TemplateManager) Save(template *Template) error {
	// 如果没有ID，生成新的UUID
	if template.ID == "" {
		template.ID = uuid.New().String()
	}

	// 确保Items不为nil
	if template.Items == nil {
		template.Items = []ConfigItem{}
	}

	// 为配置项和目标点生成ID
	for i := range template.Items {
		if template.Items[i].ID == "" {
			template.Items[i].ID = uuid.New().String()
		}
		// 确保Targets不为nil
		if template.Items[i].Targets == nil {
			template.Items[i].Targets = []FileTarget{}
		}
		for j := range template.Items[i].Targets {
			if template.Items[i].Targets[j].ID == "" {
				template.Items[i].Targets[j].ID = uuid.New().String()
			}
		}
	}

	// 序列化为JSON
	data, err := json.MarshalIndent(template, "", "  ")
	if err != nil {
		return fmt.Errorf("序列化模板失败: %v", err)
	}

	// 写入文件
	filePath := filepath.Join(tm.templatesDir, template.ID+".json")
	err = os.WriteFile(filePath, data, 0644)
	if err != nil {
		return fmt.Errorf("写入模板文件失败: %v", err)
	}

	return nil
}

// Delete 删除模板
func (tm *TemplateManager) Delete(id string) error {
	filePath := filepath.Join(tm.templatesDir, id+".json")
	err := os.Remove(filePath)
	if err != nil {
		return fmt.Errorf("删除模板文件失败: %v", err)
	}
	return nil
}

// loadTemplate 从文件加载模板
func (tm *TemplateManager) loadTemplate(filePath string) (*Template, error) {
	data, err := os.ReadFile(filePath)
	if err != nil {
		return nil, fmt.Errorf("读取模板文件失败: %v", err)
	}

	var template Template
	err = json.Unmarshal(data, &template)
	if err != nil {
		return nil, fmt.Errorf("解析模板文件失败: %v", err)
	}

	return &template, nil
}
