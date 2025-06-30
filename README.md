# 配置文件修改工具

一个简单的配置文件修改工具，支持通过模板管理多个配置文件的修改。

## 📦 版本选择

### 🖥️ 桌面版（推荐用于现场部署）
- **无端口占用** - 完全解决端口冲突问题
- **单文件部署** - 约10MB，复制即用
- **离线运行** - 无需网络连接
- **位置**: `config-tool-desktop/release-desktop/`

### 🌐 Web版（推荐用于开发调试）  
- **Web界面** - 浏览器访问，界面美观
- **开发友好** - 易于调试和修改
- **需要端口** - 占用8080端口
- **位置**: 项目根目录

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

### 🖥️ 桌面版（零配置运行）

#### 直接使用
1. 进入桌面版目录
```bash
cd config-tool-desktop/release-desktop/
```

2. 双击运行
- 双击 `config-tool-desktop.exe` 直接启动
- 或双击 `启动程序.bat` 查看启动日志

#### 文件说明
```
release-desktop/
├── config-tool-desktop.exe    # 主程序（约10MB）
├── data/                      # 模板数据目录
├── 使用说明.txt               # 详细使用说明
└── 启动程序.bat               # 启动脚本
```

### 🌐 Web版（开发调试）

#### 环境要求
- Go 1.21 或更高版本

#### 安装步骤
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

#### 编译Web版可执行文件
```bash
go build -o config-tool.exe cmd/app/main.go
```

#### 构建桌面版
```bash
cd config-tool-desktop
wails build
```

## 使用指南

### 界面操作（桌面版和Web版相同）

#### 1. 创建模板
1. 点击"新建模板"按钮
2. 输入模板名称和描述
3. 保存后模板会出现在左侧列表中

#### 2. 编辑模板
1. 选择一个模板，点击"编辑模板"
2. 点击"添加配置项"添加新的配置项
3. 为每个配置项添加目标点（指定文件和匹配规则）
4. 点击"保存模板"完成编辑

#### 3. 添加目标点
1. 在模板编辑器中选择一个配置项
2. 点击"添加目标点"
3. 输入目标文件路径和搜索条件
4. 点击"查找匹配项"验证匹配结果
5. 选择要管理的行，点击"确认选择"

### 高级配置（手动编辑JSON文件）
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

#### 4. 应用配置
1. 选择一个模板
2. 查看每个配置项的当前值
3. 在输入框中填写新值
4. 点击"一键应用配置"完成修改

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