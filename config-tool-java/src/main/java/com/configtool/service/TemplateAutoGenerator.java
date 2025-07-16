package com.configtool.service;

import com.configtool.config.EnvironmentConfig;
import com.configtool.model.ConfigItem;
import com.configtool.model.FileTarget;
import com.configtool.model.MatchResult;
import com.configtool.model.Template;
import com.configtool.util.JsonUtil;
import com.fasterxml.uuid.Generators;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板自动生成器
 * 负责从半成品模板生成完整的可用模板
 */
public class TemplateAutoGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TemplateAutoGenerator.class);
    
    // 使用EnvironmentConfig中统一配置的环境变量列表
    
    private final FileProcessor fileProcessor;
    
    // 生成统计信息
    private final List<String> skippedEnvVars = new ArrayList<>();
    private final List<String> skippedPaths = new ArrayList<>();
    private int totalTargets = 0;
    private int validTargets = 0;
    
    // 当前应用类型
    private String appType = "dfm";
    
    public TemplateAutoGenerator() {
        this.fileProcessor = new FileProcessor();
    }
    
    public TemplateAutoGenerator(String appType) {
        this.fileProcessor = new FileProcessor();
        this.appType = appType != null ? appType.toLowerCase() : "dfm";
    }
    
    /**
     * 生成配置模板（根据appType生成对应类型）
     */
    public Template generateTemplate() {
        try {
            String templateType = appType.toUpperCase();
            logger.info("========================================");
            logger.info("开始生成{}配置模板", templateType);
            logger.info("========================================");
            
            // 重置统计信息
            resetStatistics();
            
            // 1. 先验证环境变量
            validateEnvironmentVariables();
            
            // 2. 确保skeleton目录存在
            ensureSkeletonDirectory();
            
            // 3. 加载半成品模板
            Template skeleton = loadSkeletonTemplate();
            if (skeleton == null) {
                throw new RuntimeException("无法加载半成品模板");
            }
            
            // 4. 创建一个新的模板副本，不修改原始skeleton
            Template generatedTemplate = createTemplateFromSkeleton(skeleton);
            
            // 5. 先根据环境变量过滤目标点，再处理路径替换
            filterTargetsByEnvironmentVariables(generatedTemplate);
            
            // 6. 处理所有配置项的路径（替换占位符并过滤无效目标点）
            processTemplateItems(generatedTemplate);
            
            // 7. 生成新的模板信息
            generateTemplateMetadata(generatedTemplate);
            
            // 8. 记录生成结果
            logGenerationResult(generatedTemplate);
            
            logger.info("{}配置模板生成完成", templateType);
            return generatedTemplate;
            
        } catch (Exception e) {
            logger.error("生成{}配置模板失败", appType.toUpperCase(), e);
            throw new RuntimeException("生成" + appType.toUpperCase() + "配置模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成DFM配置模板（向下兼容）
     */
    public Template generateDfmTemplate() {
        return generateTemplate();
    }
    

    
    /**
     * 重置统计信息
     */
    private void resetStatistics() {
        skippedEnvVars.clear();
        skippedPaths.clear();
        totalTargets = 0;
        validTargets = 0;
    }
    
    /**
     * 从skeleton创建一个新的模板副本（深拷贝）
     */
    private Template createTemplateFromSkeleton(Template skeleton) {
        try {
            // 使用JSON序列化/反序列化进行深拷贝
            String skeletonJson = JsonUtil.toJson(skeleton);
            Template copy = JsonUtil.fromJson(skeletonJson, Template.class);
            logger.info("成功创建skeleton模板的副本");
            return copy;
        } catch (Exception e) {
            logger.error("创建skeleton模板副本失败", e);
            throw new RuntimeException("创建模板副本失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据环境变量配置情况过滤目标点
     * 如果环境变量未配置，删除相关的目标点
     */
    private void filterTargetsByEnvironmentVariables(Template template) {
        if (template.getItems() == null) {
            return;
        }
        
        logger.info("开始根据环境变量过滤目标点...");
        
        for (ConfigItem item : template.getItems()) {
            if (item.getTargets() == null) {
                continue;
            }
            
            List<FileTarget> validTargets = new ArrayList<>();
            for (FileTarget target : item.getTargets()) {
                totalTargets++;
                
                if (shouldKeepTargetBasedOnEnvVar(target)) {
                    validTargets.add(target);
                } else {
                    String envVar = extractEnvVarFromPath(target.getFilePath());
                    skippedPaths.add(target.getFilePath() + " (原因: ENV:" + envVar + ")");
                    logger.info("删除目标点 (环境变量未配置): {} -> ENV:{}", target.getId(), envVar);
                }
            }
            
            // 更新目标点列表
            item.setTargets(validTargets);
        }
        
        logger.info("环境变量过滤完成");
    }
    
    /**
     * 判断是否应该保留目标点（基于环境变量检查）
     */
    private boolean shouldKeepTargetBasedOnEnvVar(FileTarget target) {
        String path = target.getFilePath();
        if (path == null || !path.contains("{{ENV:")) {
            // 不包含环境变量，保留
            return true;
        }
        
        String envVar = extractEnvVarFromPath(path);
        if (envVar == null) {
            // 解析失败，保留（后续处理时会报错）
            return true;
        }
        
        // 检查环境变量是否在预设列表中
        if (!isRequiredEnvVar(envVar)) {
            logger.warn("环境变量不在预设列表中: {}", envVar);
            return false;
        }
        
        // 检查环境变量是否已配置
        String envValue = System.getenv(envVar);
        boolean isConfigured = envValue != null && !envValue.trim().isEmpty();
        
        if (!isConfigured && !skippedEnvVars.contains(envVar)) {
            skippedEnvVars.add(envVar);
            logger.warn("环境变量未配置，将删除相关目标点: {}", envVar);
        }
        
        return isConfigured;
    }
    
    /**
     * 从路径中提取环境变量名
     */
    private String extractEnvVarFromPath(String path) {
        if (path == null || !path.contains("{{ENV:")) {
            return null;
        }
        
        Pattern pattern = Pattern.compile("\\{\\{ENV:([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(path);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 标准化路径分隔符，避免双反斜杠等问题
     */
    private String normalizePathSeparators(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        // 将双反斜杠替换为单反斜杠
        path = path.replace("\\\\", "\\");
        
        // 将双正斜杠替换为单正斜杠
        path = path.replace("//", "/");
        
        // 处理混合分隔符：如果是Windows环境，统一为反斜杠
        if (File.separator.equals("\\")) {
            path = path.replace("/", "\\");
        }
        
        return path;
    }
    
    /**
     * 检查环境变量是否在配置的必需环境变量列表中
     */
    private boolean isRequiredEnvVar(String envVar) {
        for (String requiredVar : EnvironmentConfig.REQUIRED_ENV_VARS) {
            if (requiredVar.equals(envVar)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 验证环境变量
     */
    private void validateEnvironmentVariables() {
        logger.info("开始验证预设环境变量...");
        
        for (String envVar : EnvironmentConfig.REQUIRED_ENV_VARS) {
            String envValue = System.getenv(envVar);
            
            if (envValue != null && !envValue.isEmpty()) {
                // 验证路径是否存在
                File envDir = new File(envValue);
                if (envDir.exists()) {
                    logger.info("✅ 环境变量有效: {}={}", envVar, envValue);
                } else {
                    logger.warn("⚠️ 环境变量路径不存在: {}={}", envVar, envValue);
                }
            } else {
                logger.warn("❌ 环境变量未设置: {}", envVar);
                skippedEnvVars.add(envVar);
            }
        }
    }
    
    /**
     * 确保skeleton目录存在
     */
    private void ensureSkeletonDirectory() {
        File dir = new File(EnvironmentConfig.SKELETON_TEMPLATES_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("创建skeleton目录: {}", dir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 加载半成品模板（根据appType选择文件）
     */
    private Template loadSkeletonTemplate() {
        String skeletonFileName;
        if ("kmvue".equals(appType)) {
            skeletonFileName = EnvironmentConfig.KMVUE_SKELETON_TEMPLATE;
        } else {
            skeletonFileName = EnvironmentConfig.DFM_SKELETON_TEMPLATE;
        }
        
        String skeletonPath = EnvironmentConfig.SKELETON_TEMPLATES_DIR + skeletonFileName;
        File skeletonFile = new File(skeletonPath);
        
        if (!skeletonFile.exists()) {
            logger.warn("{}半成品模板文件不存在: {}", appType.toUpperCase(), skeletonFile.getAbsolutePath());
            return null;
        }
        
        try {
            String content = FileUtils.readFileToString(skeletonFile, StandardCharsets.UTF_8);
            Template template = JsonUtil.fromJson(content, Template.class);
            logger.info("成功加载{}半成品模板: {}", appType.toUpperCase(), skeletonFile.getAbsolutePath());
            return template;
        } catch (IOException e) {
            logger.error("读取{}半成品模板失败: {}", appType.toUpperCase(), skeletonFile.getAbsolutePath(), e);
            return null;
        }
    }
    

    
    /**
     * 处理模板中的所有配置项（替换占位符、过滤无效目标点、自动检测行号）
     */
    private void processTemplateItems(Template template) {
        if (template.getItems() == null) {
            return;
        }
        
        logger.info("开始处理模板配置项...");
        
        List<ConfigItem> validItems = new ArrayList<>();
        
        for (ConfigItem item : template.getItems()) {
            if (item.getTargets() == null) {
                continue;
            }
            
            // 先处理目标点（解析路径、搜索匹配项等）
            List<FileTarget> validTargets = new ArrayList<>();
            for (FileTarget target : item.getTargets()) {
                // 注意：totalTargets已经在环境变量过滤阶段统计过了
                String originalPath = target.getFilePath();
                
                if (isValidTargetAfterPathReplacement(target)) {
                    // 处理目标点（解析路径、搜索匹配项等）
                    List<FileTarget> processedTargets = processFileTargetWithMultipleMatches(target);
                    
                    // 检查处理后的目标点是否真正有效
                    for (FileTarget processedTarget : processedTargets) {
                        if (isTargetValidAfterProcessing(processedTarget, originalPath)) {
                            validTargets.add(processedTarget);
                        this.validTargets++;
                    } else {
                        logger.info("跳过处理后无效的目标点: {}", originalPath);
                        }
                    }
                } else {
                    logger.info("跳过无效目标点: {}", originalPath);
                }
            }
            
            // 更新配置项的目标点列表，只保留有效的
            item.setTargets(validTargets);
            
            // 如果配置项有有效的目标点，则保留该配置项
            if (!validTargets.isEmpty()) {
                                  validItems.add(item);
                  
                  // 暂时不处理defaultValue，稍后统一处理
                  
                  // 清空currentValue字段，确保生成的是半成品模板
                  item.setCurrentValue(null);
                logger.debug("清空配置项currentValue字段: {}", item.getName());
            } else {
                logger.info("删除无目标点的配置项: {} (原因: 没有有效的目标点)", item.getName());
            }
        }
        
                  // 更新模板的配置项列表，只保留有有效目标点的配置项
          template.setItems(validItems);
          
          // 跳过默认值应用，避免文件修改影响配置项识别
          logger.info("跳过默认值应用，保持原始文件状态");
          
          logger.info("模板配置项处理完成");
    }
    
    /**
     * 处理配置项默认值中的占位符
     */
    private void processDefaultValue(ConfigItem item) {
        String defaultValue = item.getDefaultValue();
        if (defaultValue == null || defaultValue.isEmpty()) {
            return;
        }
        
        // 如果默认值包含占位符，需要解析
        if (defaultValue.contains("{{") && defaultValue.contains("}}")) {
            String originalDefaultValue = defaultValue;
            String resolvedDefaultValue = resolvePath(defaultValue);
            
            // 如果解析成功（不再包含占位符），则应用到所有有效目标点
            if (!resolvedDefaultValue.contains("{{") && !resolvedDefaultValue.contains("}}")) {
                applyDefaultValueToTargets(item, resolvedDefaultValue);
                item.setDefaultValue(null); // 清空默认值，避免阻塞用户修改
                logger.info("处理配置项默认值占位符: {} -> {} (已应用到目标点并清空默认值)", originalDefaultValue, resolvedDefaultValue);
            } else {
                logger.warn("配置项默认值占位符解析失败: {} -> {}", originalDefaultValue, resolvedDefaultValue);
            }
        } else {
            // 普通默认值，直接应用到所有有效目标点
            applyDefaultValueToTargets(item, defaultValue);
            item.setDefaultValue(null); // 清空默认值，避免阻塞用户修改
            logger.info("应用配置项默认值: {} -> {} (已应用到目标点并清空默认值)", item.getName(), defaultValue);
        }
    }
    
    /**
     * 将默认值应用到配置项的所有有效目标点
     */
    private void applyDefaultValueToTargets(ConfigItem item, String defaultValue) {
        if (item.getTargets() == null || item.getTargets().isEmpty()) {
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (FileTarget target : item.getTargets()) {
            try {
                // 只对有效的目标点应用默认值（行号大于0表示有效）
                if (target.getLineNumber() > 0) {
                    fileProcessor.applyChange(target, defaultValue);
                    successCount++;
                    logger.debug("成功应用默认值到目标点: {} -> {}", target.getId(), defaultValue);
                } else {
                    logger.debug("跳过无效目标点: {}", target.getId());
                }
            } catch (Exception e) {
                failCount++;
                logger.error("应用默认值失败: target={}, value={}, error={}", target.getId(), defaultValue, e.getMessage());
            }
        }
        
        logger.info("配置项 {} 默认值应用完成: 成功 {} 个，失败 {} 个", item.getName(), successCount, failCount);
    }
    
        /**
     * 判断目标点是否有效（路径替换和匹配搜索后）
     */
    private boolean isValidTargetAfterPathReplacement(FileTarget target) {
        String originalPath = target.getFilePath();
        if (originalPath == null || originalPath.isEmpty()) {
            return false;
        }

        // 尝试解析路径
        String resolvedPath = resolvePath(originalPath);

        // 如果路径解析失败（仍包含未解析的标记），则无效
        if (resolvedPath.contains("{{") || resolvedPath.contains("}}")) {
            String reason = extractUnresolvedMarkers(resolvedPath);
            skippedPaths.add(originalPath + " (原因: " + reason + ")");
            return false;
        }

        // 验证解析后的路径是否存在
        File targetFile = new File(resolvedPath);
        if (!targetFile.exists()) {
            skippedPaths.add(originalPath + " (原因: 文件不存在)");
            return false;
        }

        return true;
    }
    
    /**
     * 在处理后检查目标点是否真正有效（包含匹配搜索结果）
     */
    private boolean isTargetValidAfterProcessing(FileTarget target, String originalPath) {
        // 检查行号是否有效（行号为0表示搜索失败）
        if (target.getLineNumber() <= 0) {
            skippedPaths.add(originalPath + " (原因: 未找到匹配项)");
            return false;
        }
        
        return true;
    }
    
    /**
     * 提取未解析的标记
     */
    private String extractUnresolvedMarkers(String path) {
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(path);
        List<String> markers = new ArrayList<>();
        
        while (matcher.find()) {
            markers.add(matcher.group(1));
        }
        
        return String.join(", ", markers);
    }
    
        /**
     * 处理单个文件目标点，支持多个匹配项
     */
    private List<FileTarget> processFileTargetWithMultipleMatches(FileTarget target) {
        List<FileTarget> results = new ArrayList<>();
        String originalPath = target.getFilePath();
        if (originalPath == null || originalPath.isEmpty()) {
            results.add(target);
            return results;
        }

        // 解析路径模板
        String resolvedPath = resolvePath(originalPath);
        
        try {
            // 使用FileProcessor在实际文件中搜索匹配项
            List<MatchResult> matches = fileProcessor.findMatches(resolvedPath, target.getPrefix(), target.getSuffix());
            
            if (matches.isEmpty()) {
                // 没有找到匹配项，设置无效行号
                target.setFilePath(resolvedPath);
                target.setLineNumber(0);
                results.add(target);
                logger.warn("未找到匹配项: {} -> {} (前缀: {})", originalPath, resolvedPath, target.getPrefix());
            } else {
                // 找到匹配项，为每个匹配项创建一个目标点
                for (int i = 0; i < matches.size(); i++) {
                    MatchResult match = matches.get(i);
                    FileTarget newTarget;
                    
                    if (i == 0) {
                        // 第一个匹配项使用原始目标点
                        newTarget = target;
                    } else {
                        // 后续匹配项创建新的目标点
                        newTarget = new FileTarget(
                            target.getId() + "_" + (i + 1),
                            target.getFilePath(),
                            target.getLineNumber(),
                            target.getPrefix(),
                            target.getSuffix()
                        );
                    }
                    
                    newTarget.setFilePath(resolvedPath);
                    newTarget.setLineNumber(match.getLineNumber());
                    results.add(newTarget);
                }
                
                StringBuilder lineNumbers = new StringBuilder();
                for (int i = 0; i < matches.size(); i++) {
                    if (i > 0) lineNumbers.append(", ");
                    lineNumbers.append(matches.get(i).getLineNumber());
                }
                logger.info("找到{}个匹配项: {} -> {} (行号: {})", 
                          matches.size(), originalPath, resolvedPath, lineNumbers.toString());
            }
            
        } catch (Exception e) {
            // 搜索过程中出现异常，设置无效行号
            target.setFilePath(resolvedPath);
            target.setLineNumber(0);
            results.add(target);
            logger.error("搜索匹配项时出现异常: {} -> {}", originalPath, resolvedPath, e);
        }
        
        return results;
    }
    
        /**
     * 处理单个文件目标点
     */
    private void processFileTarget(FileTarget target) {
        String originalPath = target.getFilePath();
        if (originalPath == null || originalPath.isEmpty()) {
            return;
        }

        // 解析路径模板
        String resolvedPath = resolvePath(originalPath);
        target.setFilePath(resolvedPath);

        // 重新搜索匹配项并更新目标点信息
        updateTargetWithMatchedResults(target, originalPath, resolvedPath);
    }
    
    /**
     * 根据搜索结果更新目标点信息
     */
    private void updateTargetWithMatchedResults(FileTarget target, String originalPath, String resolvedPath) {
        try {
            // 使用FileProcessor在实际文件中搜索匹配项
            List<MatchResult> matches = fileProcessor.findMatches(resolvedPath, target.getPrefix(), target.getSuffix());
            
            if (matches.isEmpty()) {
                // 没有找到匹配项，设置无效行号
                target.setLineNumber(0);
                logger.warn("未找到匹配项: {} -> {} (前缀: {})", originalPath, resolvedPath, target.getPrefix());
            } else {
                // 找到匹配项，使用第一个匹配项（通常一个文件只有一个匹配项）
                MatchResult firstMatch = matches.get(0);
                target.setLineNumber(firstMatch.getLineNumber());
                
                if (matches.size() > 1) {
                    logger.info("找到多个匹配项({}个)，使用第一个: {} -> {} (行号: {})", 
                              matches.size(), originalPath, resolvedPath, firstMatch.getLineNumber());
                } else {
                    logger.info("找到匹配项: {} -> {} (行号: {})", 
                              originalPath, resolvedPath, firstMatch.getLineNumber());
                }
            }
            
        } catch (Exception e) {
            // 搜索过程中出现异常，设置无效行号
            target.setLineNumber(0);
            logger.error("搜索匹配项时出现异常: {} -> {}", originalPath, resolvedPath, e);
        }
    }
    
    /**
     * 解析路径模板
     */
    private String resolvePath(String pathTemplate) {
        // 处理相对路径
        if (pathTemplate.startsWith(EnvironmentConfig.RELATIVE_PATH_MARKER)) {
            return resolveRelativePath(pathTemplate);
        }
        
        // 处理环境变量路径
        if (pathTemplate.contains(EnvironmentConfig.ENV_PATH_MARKER_PREFIX)) {
            return resolveEnvironmentPath(pathTemplate);
        }
        
        // 原样返回
        return pathTemplate;
    }
    
    /**
     * 解析相对路径
     */
    private String resolveRelativePath(String pathTemplate) {
        try {
            String relativePart = pathTemplate.substring(EnvironmentConfig.RELATIVE_PATH_MARKER.length());
            
            // 移除开头的路径分隔符（如果有的话）
            if (relativePart.startsWith("\\") || relativePart.startsWith("/")) {
                relativePart = relativePart.substring(1);
            }
            
            String basePath = getCurrentWorkingDirectory();
            File resolvedFile = new File(basePath, relativePart);
            String absolutePath = resolvedFile.getCanonicalPath();
            
            // 标准化路径分隔符
            absolutePath = normalizePathSeparators(absolutePath);
            
            logger.info("相对路径解析: {} -> {}", pathTemplate, absolutePath);
            return absolutePath;
        } catch (IOException e) {
            logger.error("相对路径解析失败: {}", pathTemplate, e);
            return pathTemplate;
        }
    }
    
    /**
     * 获取当前工作目录
     */
    private String getCurrentWorkingDirectory() {
        try {
            String workDir = System.getProperty("user.dir");
            File workDirFile = new File(workDir);
            
            // 如果当前工作目录名为config-tool-java，返回其上级目录
            if (workDirFile.getName().equals("config-tool-java") && workDirFile.getParentFile() != null) {
                return workDirFile.getParentFile().getCanonicalPath();
            }
            
            // 否则返回当前工作目录
            return workDir;
            
        } catch (Exception e) {
            logger.error("获取当前工作目录失败", e);
            return System.getProperty("user.dir");
        }
    }
    
    /**
     * 解析环境变量路径
     */
    private String resolveEnvironmentPath(String pathTemplate) {
        Pattern pattern = Pattern.compile("\\{\\{ENV:([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(pathTemplate);
        
        if (matcher.find()) {
            String envVar = matcher.group(1);
            
            // 只处理预设的环境变量
            if (!isRequiredEnvVar(envVar)) {
                logger.warn("环境变量不在预设列表中: {}", envVar);
                return pathTemplate; // 返回原始模板，保留标记
            }
            
            String envValue = System.getenv(envVar);
            
            if (envValue != null && !envValue.isEmpty()) {
                String resolvedPath = pathTemplate.replace("{{ENV:" + envVar + "}}", envValue);
                
                // 处理路径分隔符，避免双反斜杠或混合分隔符
                resolvedPath = normalizePathSeparators(resolvedPath);
                
                logger.info("环境变量路径解析: {} -> {} ({}={})", pathTemplate, resolvedPath, envVar, envValue);
                return resolvedPath;
            } else {
                logger.warn("环境变量未设置或为空: {}", envVar);
                return pathTemplate; // 返回原始模板，保留标记
            }
        }
        
        return pathTemplate;
    }
    
    /**
     * 获取jar包父目录
     */
    private String getJarParentDirectory() {
        try {
            // 方法1：尝试通过jar文件位置获取
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            File jarFile = new File(jarPath);
            
            // 如果是jar文件，获取其父目录的父目录
            if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                File jarDir = jarFile.getParentFile(); // jar文件所在目录
                if (jarDir != null && jarDir.getParentFile() != null) {
                    return jarDir.getParentFile().getCanonicalPath(); // jar文件目录的上级目录
                }
            }
            
            // 方法2：通过当前工作目录获取
            String workDir = System.getProperty("user.dir");
            File workDirFile = new File(workDir);
            
            // 如果当前工作目录名为config-tool-java，返回其上级目录
            if (workDirFile.getName().equals("config-tool-java") && workDirFile.getParentFile() != null) {
                return workDirFile.getParentFile().getCanonicalPath();
            }
            
            // 否则返回当前工作目录
            return workDir;
            
        } catch (Exception e) {
            logger.error("获取jar包父目录失败", e);
            // fallback：返回当前工作目录
            return System.getProperty("user.dir");
        }
    }
    
    /**
     * 自动检测行号（已废弃，使用动态搜索机制替代）
     */
    @Deprecated
    private int autoDetectLineNumber(String filePath, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return 1; // 默认第一行
        }
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("文件不存在，无法检测行号: {}", filePath);
                return 1;
            }
            
            // 使用FileProcessor的文件编码检测功能
            java.nio.charset.Charset encoding = fileProcessor.detectFileEncoding(file);
            java.util.List<String> lines = FileUtils.readLines(file, encoding);
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains(prefix)) {
                    int lineNumber = i + 1; // 行号从1开始
                    logger.info("自动检测到行号 {} (前缀: {}): {}", lineNumber, prefix, filePath);
                    return lineNumber;
                }
            }
            
            logger.warn("未找到匹配前缀的行 (前缀: {}): {}", prefix, filePath);
            return 1; // 默认第一行
            
        } catch (Exception e) {
            logger.error("自动检测行号失败: {}", filePath, e);
            return 1; // 默认第一行
        }
    }
    
    /**
     * 记录生成结果
     */
    private void logGenerationResult(Template template) {
        String templateType = appType.toUpperCase();
        StringBuilder result = new StringBuilder();
        result.append(String.format("\n========== %s配置模板生成完成 ==========\n", templateType));
        result.append(String.format("✅ 成功生成模板: %s\n", template.getName()));
        result.append(String.format("📊 统计信息: 总计 %d 个目标点，成功 %d 个\n", totalTargets, validTargets));
        
        if (!skippedEnvVars.isEmpty()) {
            result.append("⚠️ 跳过的环境变量:\n");
            for (String envVar : skippedEnvVars) {
                result.append(String.format("   - %s\n", envVar));
            }
        }
        
        if (!skippedPaths.isEmpty()) {
            result.append("⚠️ 跳过的目标点:\n");
            for (String path : skippedPaths) {
                result.append(String.format("   - %s\n", path));
            }
        }
        
        if (skippedEnvVars.isEmpty() && skippedPaths.isEmpty()) {
            result.append("🎉 所有目标点都已成功处理！\n");
        } else {
            result.append("💡 如需添加被跳过的配置，请设置相应环境变量或检查文件路径后重新生成\n");
        }
        
        result.append("==========================================");
        logger.info(result.toString());
    }
    
    /**
     * 生成模板元数据
     */
    private void generateTemplateMetadata(Template template) {
        // 生成新的模板ID
        template.setId(Generators.timeBasedGenerator().generate().toString());
        
        // 生成新的模板名称 - 简化版本
        String templateType = appType.toUpperCase();
        template.setName(templateType + "配置修改");
        
        // 简化描述
        template.setDescription("");
    }
} 