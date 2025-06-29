package template

// Template 模板
type Template struct {
	ID          string       `json:"id"`
	Name        string       `json:"name"`
	Description string       `json:"description"`
	Items       []ConfigItem `json:"items"`
}

// ConfigItem 配置项
type ConfigItem struct {
	ID           string       `json:"id"`
	Name         string       `json:"name"`
	Description  string       `json:"description"`
	DefaultValue string       `json:"defaultValue"`
	Targets      []FileTarget `json:"targets"`
	CurrentValue string       `json:"currentValue,omitempty"` // API响应用
}

// FileTarget 目标点
type FileTarget struct {
	ID         string `json:"id"`
	FilePath   string `json:"filePath"`
	LineNumber int    `json:"lineNumber"`
	Prefix     string `json:"prefix"`
	Suffix     string `json:"suffix,omitempty"`
}

// MatchResult 搜索结果
type MatchResult struct {
	LineNumber  int    `json:"lineNumber"`
	LineContent string `json:"lineContent"`
}

// ModificationTask 修改任务
type ModificationTask struct {
	Target   *FileTarget `json:"target"`
	NewValue string      `json:"newValue"`
} 