package com.configtool;

import com.configtool.controller.AppController;
import com.configtool.model.Template;
import com.configtool.service.TemplateService;
import com.configtool.service.TemplateAutoGenerator;
import com.configtool.service.NacosApiService;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 主应用程序类
 * 基于JavaFX WebView的桌面应用程序
 */
public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    private AppController appController;
    private static String appType = "dfm"; // 默认为dfm
    private static boolean isParameterMode = false; // 是否为参数启动模式

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing JavaFX application...");
            
            // 初始化控制器
            appController = new AppController(appType, isParameterMode);
            logger.info("AppController initialized successfully with app type: {}, parameter mode: {}", appType, isParameterMode);
            
            // 创建WebView
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            logger.info("WebView and WebEngine created");
            
            // 启用开发者工具（调试用）
            webEngine.setJavaScriptEnabled(true);
            logger.info("JavaScript enabled for WebEngine");
            
            // 设置页面加载完成后的回调
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                logger.info("WebEngine state changed from {} to {}", oldState, newState);
                
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        // 注入Java对象到JavaScript环境
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("javaApp", appController);
                        
                        logger.info("JavaScript Bridge initialized successfully");
                    } catch (Exception e) {
                        logger.error("Failed to initialize JavaScript Bridge", e);
                    }
                } else if (newState == Worker.State.FAILED) {
                    logger.error("WebEngine failed to load content");
                    Throwable exception = webEngine.getLoadWorker().getException();
                    if (exception != null) {
                        logger.error("WebEngine exception: ", exception);
                    }
                }
            });
            
            // 加载HTML页面
            logger.info("Loading HTML content...");
            String htmlContent = loadHtmlFromResources();
            logger.info("HTML content loaded, length: {} characters", htmlContent.length());
            
            webEngine.loadContent(htmlContent);
            logger.info("WebEngine.loadContent() called");
            
            // 设置窗口
            String windowTitle;
            if (isParameterMode) {
                windowTitle = appType.toUpperCase() + "配置修改工具";
            } else {
                windowTitle = "Config File Modification Tool - Java Desktop";
            }
            
            primaryStage.setTitle(windowTitle);
            primaryStage.setScene(new Scene(webView, 1200, 800));
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // 确保窗口始终在前台
            primaryStage.setAlwaysOnTop(true);
            primaryStage.setResizable(true);
            
            logger.info("Stage configured: title, scene, size, and properties set");
            
            // 设置关闭事件
            primaryStage.setOnCloseRequest(e -> {
                logger.info("User requested application shutdown");
                System.exit(0);
            });
            
            // 显示窗口
            primaryStage.show();
            logger.info("Stage.show() called - window should be visible now");
            
            // 强制焦点到窗口
            primaryStage.requestFocus();
            logger.info("Window focus requested");
            
            // 5秒后取消置顶
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> {
                        primaryStage.setAlwaysOnTop(false);
                        logger.info("Always on top disabled after 5 seconds");
                    });
                } catch (InterruptedException e) {
                    logger.warn("Sleep interrupted", e);
                }
            }).start();
            
            logger.info("Application started successfully - Window should be visible");
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    /**
     * 从资源文件加载HTML内容
     */
    private String loadHtmlFromResources() {
        try {
            // 统一使用index.html
            String htmlFileName = "/web/index.html";
            logger.info("Attempting to load HTML from resources: {}", htmlFileName);
            
            InputStream inputStream = getClass().getResourceAsStream(htmlFileName);
            if (inputStream == null) {
                logger.error("HTML file not found: {}", htmlFileName);
                throw new RuntimeException("HTML file not found: " + htmlFileName);
            }
            
            logger.info("HTML file stream opened successfully");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String content = reader.lines().collect(Collectors.joining("\n"));
            
            logger.info("HTML content read successfully, original length: {} characters", content.length());
            
            // 修改HTML中的JavaScript调用，使用Java Bridge
            content = content.replace("window.go.main.App.", "window.javaApp.");
            
            logger.info("HTML content processed, final length: {} characters", content.length());
            
            return content;
        } catch (Exception e) {
            logger.error("Failed to load HTML file", e);
            throw new RuntimeException("Failed to load HTML file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 设置系统编码为UTF-8，解决中文乱码问题
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        
        // 处理启动参数
        if (args.length > 0) {
            String type = args[0].toLowerCase();
            System.out.println("检测到启动参数: " + type);
            if ("kmvue".equals(type) || "dfm".equals(type)) {
                appType = type;
                isParameterMode = true; // 标记为参数启动模式
                System.out.println("进入参数模式，应用类型: " + appType.toUpperCase());
                
                // 方案2：自动清理模板并生成半成品模板
                System.out.println("开始执行自动生成操作...");
                performAutoGeneration();
                System.out.println("自动生成操作完成！");
            } else {
                System.out.println("未知的应用类型: " + type + ", 使用默认值: dfm");
            }
        } else {
            System.out.println("未检测到启动参数，使用默认模式");
        }
        
        // 检查Nacos服务状态
        checkNacosStatusOnStartup();
        
        // 设置JavaFX系统属性
        System.setProperty("javafx.preloader", "");
        System.setProperty("prism.lcdtext", "false");
        
        try {
            // 启动JavaFX应用
            launch(args);
        } catch (Exception e) {
            System.err.println("启动JavaFX应用失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * 自动生成操作（方案2）
     * 每次启动时自动清理模板目录并生成半成品模板
     */
    private static void performAutoGeneration() {
        try {
            System.out.println("正在初始化" + appType.toUpperCase() + "配置工具...");
            logger.info("开始执行{}自动生成操作", appType.toUpperCase());
            
            // 初始化服务
            TemplateService templateService = new TemplateService();
            TemplateAutoGenerator templateAutoGenerator = new TemplateAutoGenerator(appType);
            
            // 完全清空模板目录
            logger.info("完全清空模板目录");
            templateService.clearAllTemplates();
            System.out.println("✓ 清理现有模板完成");
            
            // 生成新的半成品模板
            System.out.println("✓ 正在生成" + appType.toUpperCase() + "配置模板...");
            logger.info("生成新的{}半成品模板", appType.toUpperCase());
            Template template = templateAutoGenerator.generateTemplate();
            
            // 确保模板名称固定
            template.setName(appType.toUpperCase() + "配置修改");
            
            Template savedTemplate = templateService.saveTemplate(template);
            
            logger.info("{}自动生成完成！模板已保存: {}", appType.toUpperCase(), savedTemplate.getName());
            System.out.println("✓ " + appType.toUpperCase() + "配置模板生成完成");
            System.out.println("✓ 正在启动配置修改界面...");
            
        } catch (Exception e) {
            logger.error("自动生成失败", e);
            System.err.println("⚠ 自动生成失败: " + e.getMessage());
            System.err.println("详细错误信息请查看: logs/config-tool.log");
            // 不退出程序，继续启动界面
        }
    }
    
    /**
     * 启动时检查Nacos服务状态
     */
    private static void checkNacosStatusOnStartup() {
        try {
            System.out.println("=== 检查Nacos服务状态 ===");
            
            // 创建NacosApiService实例
            NacosApiService nacosApiService = new NacosApiService();
            
            if (!nacosApiService.isEnabled()) {
                System.out.println("Nacos配置管理功能未启用");
                System.out.println("如需启用，请修改 src/main/resources/nacos-config.properties 中的 nacos.enabled=true");
                return;
            }
            
            System.out.println("Nacos配置管理功能已启用");
            System.out.println("Nacos服务地址: " + nacosApiService.getNacosUrl());
            System.out.println("Nacos命名空间: " + nacosApiService.getNamespace());
            
            // 检查Nacos服务是否可用
            if (nacosApiService.isNacosAvailable()) {
                System.out.println("✓ Nacos服务连接正常");
                System.out.println("数据库配置变更将自动同步到Nacos");
            } else {
                System.out.println("✗ Nacos服务连接失败");
                System.out.println("建议：");
                System.out.println("1. 检查Nacos服务是否正在运行");
                System.out.println("2. 确认Nacos服务地址配置正确");
                System.out.println("3. 确认网络连接正常");
                System.out.println("注意：Nacos服务不可用不会影响配置工具的基本功能");
            }
            
            System.out.println("=== Nacos状态检查完成 ===");
            
        } catch (Exception e) {
            System.err.println("检查Nacos服务状态失败: " + e.getMessage());
            // 不影响程序启动，只记录错误
        }
    }
} 