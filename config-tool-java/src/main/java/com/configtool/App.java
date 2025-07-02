package com.configtool;

import com.configtool.controller.AppController;
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

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing JavaFX application...");
            
            // 初始化控制器
            appController = new AppController();
            logger.info("AppController initialized successfully");
            
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
            primaryStage.setTitle("Config File Modification Tool - Java Desktop");
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
            logger.info("Attempting to load HTML from resources: /web/index.html");
            
            InputStream inputStream = getClass().getResourceAsStream("/web/index.html");
            if (inputStream == null) {
                logger.error("HTML file not found: /web/index.html");
                throw new RuntimeException("HTML file not found: /web/index.html");
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
        // 设置JavaFX系统属性
        System.setProperty("javafx.preloader", "");
        System.setProperty("prism.lcdtext", "false");
        
        logger.info("Starting Config File Modification Tool - Java Desktop Version");
        logger.info("Java version: {}", System.getProperty("java.version"));
        logger.info("JavaFX runtime version: {}", System.getProperty("javafx.runtime.version"));
        logger.info("OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        
        try {
            // 启动JavaFX应用
            launch(args);
        } catch (Exception e) {
            logger.error("Failed to launch JavaFX application", e);
            System.exit(1);
        }
        
        logger.info("JavaFX application has exited");
    }
} 