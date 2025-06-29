# 配置文件修改工具

一个简单的配置文件修改工具，支持通过模板管理多个配置文件的修改。

## 功能特性

- 🎯 **模板管理**：创建和管理配置模板
- 📁 **文件处理**：支持任意格式的文本配置文件
- 🔍 **智能搜索**：通过前缀/后缀匹配定位配置项
- ⚡ **一键应用**：批量修改多个文件的配置
- 🌐 **Web界面**：简洁直观的浏览器界面

## 项目结构

```
config-tool/
├── cmd/app/main.go              # 程序入口
├── internal/
│   ├── template/                # 模板管理模块
│   │   ├── models.go           # 数据结构定义
│   │   └── manager.go          # 模板CRUD操作
│   ├── processor/               # 文件处理模块
│   │   └── processor.go        # 文件读写和匹配
│   └── server/                  # Web服务模块
│       ├── server.go           # HTTP服务器
│       └── handlers.go         # API处理函数
├── web/dist/                    # 前端静态文件
│   └── index.html              # 主页面
├── data/templates/              # 模板存储目录
├── go.mod                       # Go模块定义
└── README.md                    # 项目说明
```

## 安装和运行

### 环境要求
- Go 1.21 或更高版本

### 安装步骤
1. 克隆项目
```bash
git clone <项目地址>
cd config-tool
```

2. 下载依赖
```bash
go mod tidy
```

3. 运行程序
```bash
go run cmd/app/main.go
```

4. 打开浏览器访问 `http://localhost:8080`

### 编译可执行文件
```bash
go build -o config-tool.exe cmd/app/main.go
```

## 使用指南

### 1. 创建模板
1. 在Web界面点击"新建模板"
2. 输入模板名称和描述
3. 保存后模板会出现在左侧列表中

### 2. 配置模板（目前需要手动编辑JSON文件）
在 `data/templates/` 目录下找到对应的模板文件，按以下格式添加配置项：

```json
{
  "id": "template-uuid",
  "name": "开发环境配置",
  "description": "开发环境的各项配置",
  "items": [
    {
      "id": "item-uuid-1",
      "name": "数据库地址",
      "description": "MySQL数据库连接地址",
      "defaultValue": "localhost",
      "targets": [
        {
          "id": "target-uuid-1",
          "filePath": "C:\\project\\config.ini",
          "lineNumber": 3,
          "prefix": "host=",
          "suffix": ""
        }
      ]
    }
  ]
}
```

### 3. 应用配置
1. 在Web界面选择一个模板
2. 查看每个配置项的当前值
3. 在输入框中填写新值
4. 点击"一键应用"完成修改

## API接口

### 模板管理
- `GET /api/templates` - 获取模板列表
- `GET /api/templates/{id}` - 获取模板详情（含当前值）
- `POST /api/templates` - 创建/更新模板
- `DELETE /api/templates/{id}` - 删除模板

### 文件处理
- `POST /api/files/find-matches` - 搜索文件匹配项
- `POST /api/apply` - 执行文件修改

### 响应格式
- 成功：`{"data": {...}}`
- 失败：`{"error": "错误信息"}`

## 数据结构

### 模板 (Template)
```go
type Template struct {
    ID          string       `json:"id"`
    Name        string       `json:"name"`
    Description string       `json:"description"`
    Items       []ConfigItem `json:"items"`
}
```

### 配置项 (ConfigItem)
```go
type ConfigItem struct {
    ID           string       `json:"id"`
    Name         string       `json:"name"`
    Description  string       `json:"description"`
    DefaultValue string       `json:"defaultValue"`
    Targets      []FileTarget `json:"targets"`
    CurrentValue string       `json:"currentValue,omitempty"`
}
```

### 目标点 (FileTarget)
```go
type FileTarget struct {
    ID         string `json:"id"`
    FilePath   string `json:"filePath"`
    LineNumber int    `json:"lineNumber"`
    Prefix     string `json:"prefix"`
    Suffix     string `json:"suffix,omitempty"`
}
```

## 工作原理

1. **模板存储**：每个模板作为独立的JSON文件存储在 `data/templates/` 目录下
2. **文件匹配**：通过行号和前缀/后缀精确定位配置项在文件中的位置
3. **值提取**：根据前缀/后缀规则从文件行中提取当前配置值
4. **批量修改**：按文件分组，一次性应用所有修改，保证效率

## 注意事项

- 请确保有足够的文件读写权限
- 建议在修改重要配置文件前先备份
- 行号从1开始计数
- 如果suffix为空，则替换从prefix后到行尾的所有内容

## 开发计划

- [ ] 完善的模板编辑界面
- [ ] 文件搜索和目标点配置功能
- [ ] 自动备份机制
- [ ] 更丰富的文件格式支持
- [ ] 配置验证功能 