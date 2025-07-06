# IDEA开发环境配置指南

## 环境要求
- **JDK**: 1.8 或更高版本
- **IntelliJ IDEA**: 2018.3 或更高版本
- **Maven**: 3.6+ (IDEA内置)

## 1. 导入项目到IDEA

### 方式一：直接打开项目
1. 启动IDEA
2. 点击 **"Open"** 或 **"Open or Import"**
3. 选择 `config-tool-java` 文件夹
4. 点击 **"OK"**
5. IDEA会自动识别Maven项目并导入

### 方式二：从版本控制导入
1. 点击 **"Get from VCS"**
2. 输入项目Git地址
3. 选择本地目录
4. 点击 **"Clone"**

## 2. 项目结构说明

导入后的项目结构：
```
config-tool-java/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/configtool/
│       │       ├── App.java              # 主程序入口
│       │       ├── controller/           # 控制器层
│       │       ├── model/               # 数据模型
│       │       ├── service/             # 业务逻辑层
│       │       └── utils/               # 工具类
│       └── resources/
│           ├── web/                     # 前端资源
│           └── data/                    # 数据文件
├── data/                                # 外部数据目录
├── target/                              # 编译输出目录
├── pom.xml                              # Maven配置文件
└── *.md                                 # 文档文件
```

## 3. 配置项目

### 3.1 检查JDK配置
1. 打开 **File -> Project Structure** (Ctrl+Alt+Shift+S)
2. 在 **Project** 标签页中：
   - **Project SDK**: 选择 JDK 1.8+
   - **Project language level**: 设置为 8
3. 在 **Modules** 标签页中：
   - 确认 **Language level** 为 8
   - 确认 **Dependencies** 中包含正确的JDK

### 3.2 Maven设置
1. 打开 **File -> Settings** (Ctrl+Alt+S)
2. 导航到 **Build, Execution, Deployment -> Build Tools -> Maven**
3. 确认配置：
   - **Maven home directory**: 使用IDEA内置的Maven
   - **User settings file**: 使用默认设置
   - **Local repository**: 默认为 `~/.m2/repository`

### 3.3 编码设置
1. 在 **File -> Settings** 中
2. 导航到 **Editor -> File Encodings**
3. 设置：
   - **Global Encoding**: UTF-8
   - **Project Encoding**: UTF-8
   - **Default encoding for properties files**: UTF-8

## 4. 运行项目

### 方式一：直接运行主类
1. 找到 `src/main/java/com/configtool/App.java`
2. 右键点击文件
3. 选择 **"Run 'App.main()'"**
4. 或者点击行号旁边的绿色箭头

### 方式二：使用Maven运行
1. 打开IDEA右侧的 **Maven** 面板
2. 展开 **config-tool-java -> Plugins -> exec**
3. 双击 **exec:java** 运行

### 方式三：使用命令行运行
1. 打开IDEA底部的 **Terminal** 面板
2. 运行命令：
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.configtool.App"
   ```

## 5. Maven依赖管理

### 5.1 依赖下载位置
Maven依赖的jar包存放在本地仓库中：
- **Windows**: `C:\Users\{用户名}\.m2\repository\`
- **Linux/macOS**: `~/.m2/repository/`

### 5.2 项目依赖列表
根据pom.xml，项目包含以下依赖：
```
├── gson-2.8.9.jar              # JSON处理
├── commons-io-2.11.0.jar       # 文件操作
├── slf4j-simple-1.7.36.jar     # 日志(简单实现)
├── slf4j-api-1.7.36.jar        # 日志API
├── java-uuid-generator-4.0.1.jar # UUID生成
└── juniversalchardet-2.4.0.jar # 编码检测
```

### 5.3 依赖存储路径示例
```
~/.m2/repository/
├── com/google/code/gson/gson/2.8.9/
│   └── gson-2.8.9.jar
├── commons-io/commons-io/2.11.0/
│   └── commons-io-2.11.0.jar
├── org/slf4j/slf4j-simple/1.7.36/
│   └── slf4j-simple-1.7.36.jar
└── ...
```

### 5.4 刷新依赖
如果依赖有问题，可以：
1. 在IDEA右侧Maven面板点击 **刷新** 按钮
2. 或者使用命令：
   ```bash
   mvn clean install
   ```

## 6. 打包和运行

### 6.1 编译项目
在IDEA中：
1. 点击 **Build -> Build Project** (Ctrl+F9)
2. 或使用Maven命令：
   ```bash
   mvn compile
   ```

### 6.2 打包项目
1. 在Maven面板中找到 **Lifecycle -> package**
2. 双击运行，或使用命令：
   ```bash
   mvn clean package
   ```
3. 生成的jar文件在 `target/config-tool-java-1.0.0.jar`

### 6.3 运行jar包
```bash
java -jar target/config-tool-java-1.0.0.jar
```

## 7. 开发调试

### 7.1 设置断点
1. 在代码行号左侧点击设置断点
2. 右键点击主类，选择 **"Debug 'App.main()'"**

### 7.2 日志查看
项目使用SLF4J日志，输出会显示在IDEA的控制台中

### 7.3 资源文件
项目的前端资源文件在：
- `src/main/resources/web/index.html`
- `src/main/resources/data/`

## 8. 常见问题

### 8.1 Maven依赖下载失败
**问题**：依赖jar包下载失败
**解决**：
1. 检查网络连接
2. 尝试使用阿里云Maven镜像：
   ```xml
   <mirror>
       <id>aliyun</id>
       <name>aliyun maven</name>
       <url>https://maven.aliyun.com/repository/public</url>
       <mirrorOf>central</mirrorOf>
   </mirror>
   ```
3. 删除 `~/.m2/repository` 中的问题依赖，重新下载

### 8.2 项目编译错误
**问题**：编译时出现错误
**解决**：
1. 检查JDK版本是否为1.8+
2. 确认Maven配置正确
3. 运行 `mvn clean compile` 重新编译

### 8.3 找不到主类
**问题**：运行时提示找不到主类
**解决**：
1. 确认主类路径：`com.configtool.App`
2. 检查src/main/java目录结构
3. 重新编译项目

### 8.4 依赖冲突
**问题**：不同依赖版本冲突
**解决**：
1. 查看Maven依赖树：`mvn dependency:tree`
2. 在pom.xml中排除冲突的依赖
3. 使用 `mvn dependency:resolve` 解决冲突

## 9. 开发技巧

### 9.1 快速导航
- **Ctrl+N**: 查找类
- **Ctrl+Shift+N**: 查找文件
- **Ctrl+B**: 跳转到定义
- **Alt+F7**: 查找用法

### 9.2 代码生成
- **Alt+Insert**: 生成构造函数、getter/setter等
- **Ctrl+O**: 重写父类方法
- **Ctrl+I**: 实现接口方法

### 9.3 重构
- **Shift+F6**: 重命名
- **Ctrl+Alt+M**: 提取方法
- **Ctrl+Alt+V**: 提取变量

## 10. 项目扩展

### 10.1 添加新依赖
1. 在pom.xml的 `<dependencies>` 标签中添加新依赖
2. 刷新Maven项目
3. 重新编译

### 10.2 修改配置
项目配置主要在：
- `pom.xml`: Maven配置
- `src/main/resources/`: 资源文件
- `data/`: 外部数据文件

---

**注意事项：**
1. 确保JDK版本正确（1.8+）
2. 网络连接正常以下载Maven依赖
3. 定期更新依赖版本以获得安全补丁
4. 使用合适的IDEA版本以获得最佳体验

**版本**: 1.0.0  
**更新日期**: 2025-07-03 