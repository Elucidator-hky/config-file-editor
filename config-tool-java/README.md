# 配置文件修改工具 - Java桌面版

基于JavaFX的配置文件修改工具桌面版本，提供现代化的图形用户界面。

## 特性

- 🖥️ **原生桌面应用**：基于JavaFX技术，无需浏览器
- 🎨 **现代化界面**：美观的用户界面，支持模板管理
- 📝 **模板化配置**：通过模板快速配置多个文件
- 🔍 **智能匹配**：精确定位需要修改的配置项
- 🚀 **一键应用**：批量修改配置文件，提高效率
- 💾 **零端口占用**：完全本地运行，无网络依赖

## 系统要求

- Java 8 或更高版本
- Windows 7/8/10/11
- 至少 100MB 可用磁盘空间

## 快速开始

### 方式一：使用预编译版本

1. 双击 `run.bat` 启动应用
2. 如果提示Java环境问题，请先安装Java 8或更高版本

### 方式二：从源码编译

```bash
# 编译项目
mvn compile

# 打包应用
mvn package

# 运行应用
java -jar target/config-tool-java-1.0.0.jar
```

## 使用指南

### 1. 创建模板
1. 点击"新建模板"按钮
2. 填写模板名称和描述
3. 添加配置项：
   - 配置项名称：如"数据库地址"
   - 配置项描述：可选的说明文字
   - 默认值：可选的预设值

### 2. 配置目标点
1. 为每个配置项添加目标点
2. 选择要修改的文件
3. 设置匹配条件：
   - 前缀：用于定位行的关键字（必填）
   - 后缀：可选的结束标记

### 3. 应用配置
1. 选择一个模板
2. 查看每个配置项的当前值
3. 在输入框中填写新值
4. 点击"一键应用配置"完成修改

## 项目结构

```
config-tool-java/
├── src/main/java/
│   └── com/configtool/
│       ├── App.java                  # 主程序入口
│       ├── model/                    # 数据模型
│       ├── service/                  # 业务逻辑
│       ├── controller/               # 控制器
│       └── util/                     # 工具类
├── src/main/resources/
│   ├── web/                          # 前端资源
│   └── data/templates/               # 模板数据
├── target/
│   └── config-tool-java-1.0.0.jar   # 可执行JAR
├── pom.xml                           # Maven配置
├── run.bat                           # 启动脚本
└── README.md                         # 说明文档
```

## 技术栈

- **后端框架**：Java 8 + JavaFX
- **JSON处理**：Gson
- **文件操作**：Apache Commons IO
- **日志框架**：SLF4J
- **构建工具**：Maven

## 与其他版本对比

| 版本 | 技术栈 | 文件大小 | 启动速度 | 端口占用 | 适用场景 |
|------|--------|----------|----------|----------|----------|
| Web版 | Go + HTML | ~8MB | 快 | 需要端口 | 开发调试 |
| Wails版 | Go + Wails | ~10MB | 快 | 无 | 现场部署 |
| **Java版** | **Java + JavaFX** | **~40MB** | **中等** | **无** | **企业环境** |

## 开发指南

### 编译要求
- JDK 8 或更高版本
- Maven 3.6+

### 本地开发
```bash
# 克隆项目
git clone <repository-url>
cd config-tool-java

# 编译
mvn compile

# 运行
mvn exec:java -Dexec.mainClass="com.configtool.App"
```

### 打包发布
```bash
# 创建可执行JAR
mvn package

# JAR文件位置
target/config-tool-java-1.0.0.jar
```

## 故障排除

### 应用无法启动
1. 检查Java版本：`java -version`
2. 确保Java版本 ≥ 8
3. 检查JavaFX支持（Java 11+需要单独安装JavaFX）

### 界面显示异常
1. 检查显示器分辨率和DPI设置
2. 尝试设置JVM参数：`-Dprism.lcdtext=false`

### 配置文件修改失败
1. 检查文件路径是否正确
2. 确保有文件写入权限
3. 检查文件是否被其他程序占用

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 更新日志

### v1.0.0 (2025-06-30)
- 🎉 首次发布Java桌面版
- ✨ 完整的图形用户界面
- 🔧 支持模板管理和配置应用
- 📦 提供一键运行脚本 