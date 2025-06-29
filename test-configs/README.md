# 测试配置文件集合

这个文件夹包含了不同格式的配置文件，用于测试配置文件修改工具的功能。

## 文件列表

### 1. server.conf
- **格式**: 简单的键值对配置
- **用途**: 类似Apache/Nginx配置风格
- **测试场景**: 
  - 前缀匹配: `ServerHost = ` → 修改服务器地址
  - 前缀匹配: `DatabasePassword = ` → 修改数据库密码
  - 前缀匹配: `LogLevel = ` → 修改日志级别

### 2. application.ini
- **格式**: INI格式（分section）
- **用途**: 经典的Windows风格配置文件
- **测试场景**:
  - 前缀匹配: `host = ` → 修改主机地址
  - 前缀匹配: `password = ` → 修改各种密码
  - 前缀匹配: `level = ` → 修改日志级别

### 3. config.json
- **格式**: JSON格式
- **用途**: 现代Web应用配置
- **测试场景**:
  - 前缀匹配: `"host": "` → 修改主机配置
  - 前缀匹配: `"password": "` → 修改密码
  - 前缀匹配: `"level": "` → 修改日志级别
  - 前缀匹配: `"debug": ` → 修改调试模式

### 4. settings.xml
- **格式**: XML格式
- **用途**: 企业级应用配置
- **测试场景**:
  - 前缀后缀匹配: `<host>` 和 `</host>` → 修改主机
  - 前缀后缀匹配: `<password>` 和 `</password>` → 修改密码
  - 前缀后缀匹配: `<level>` 和 `</level>` → 修改日志级别

### 5. app.yaml
- **格式**: YAML格式
- **用途**: 云原生应用配置
- **测试场景**:
  - 前缀匹配: `host: "` → 修改主机配置
  - 前缀匹配: `password: "` → 修改密码
  - 前缀匹配: `level: "` → 修改日志级别
  - 前缀匹配: `debug: ` → 修改调试模式

## 建议的测试流程

### 测试1: 创建基础模板
1. 创建一个名为"开发环境配置"的模板
2. 添加配置项：
   - 服务器地址 (默认值: localhost)
   - 数据库密码 (默认值: newpassword)
   - 日志级别 (默认值: debug)

### 测试2: 关联不同格式文件
为每个配置项添加目标点，在不同格式的文件中查找相应的配置行：

**服务器地址配置项**:
- server.conf: 前缀 `ServerHost = `
- application.ini: 前缀 `host = `
- config.json: 前缀 `"host": "`
- settings.xml: 前缀 `<host>`, 后缀 `</host>`
- app.yaml: 前缀 `host: "`

**数据库密码配置项**:
- server.conf: 前缀 `DatabasePassword = `
- application.ini: 前缀 `password = `
- config.json: 前缀 `"password": "`
- settings.xml: 前缀 `<password>`, 后缀 `</password>`
- app.yaml: 前缀 `password: "`

### 测试3: 一键应用修改
1. 在主界面选择创建的模板
2. 修改配置项的值
3. 点击"一键应用"
4. 检查所有文件是否都正确修改

## 注意事项
- 使用绝对路径，例如: `E:\code\editPath\test-configs\server.conf`
- 确保前缀匹配准确，包括空格和引号
- JSON和YAML文件中注意引号的使用
- XML文件使用前缀+后缀的方式更精确 