package com.configtool.service;

import com.configtool.model.FileTarget;
import com.configtool.model.MatchResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.universalchardet.UniversalDetector;

/**
 * 文件处理器
 * 负责文件的读取、写入和内容匹配
 */
public class FileProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    /**
     * 使用专业库检测文件编码
     */
    public Charset detectFileEncoding(File file) {
        try {
            // 使用 UniversalDetector 进行精确编码检测
            UniversalDetector detector = new UniversalDetector(null);
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int nread;
                while ((nread = fis.read(buffer)) > 0 && !detector.isDone()) {
                    detector.handleData(buffer, 0, nread);
                }
            }
            
            detector.dataEnd();
            String detectedEncoding = detector.getDetectedCharset();
            detector.reset();
            
            if (detectedEncoding != null) {
                try {
                    Charset charset = Charset.forName(detectedEncoding);
                    logger.info("专业检测到编码 {}: {}", detectedEncoding, file.getPath());
                    return charset;
                } catch (Exception e) {
                    logger.warn("不支持的编码 {}，进行fallback检测: {}", detectedEncoding, file.getPath());
                }
            }
            
            // fallback到基础检测方法
            return fallbackDetectEncoding(file);
            
        } catch (IOException e) {
            logger.warn("专业编码检测失败，使用fallback: {}", file.getPath(), e);
            return fallbackDetectEncoding(file);
        }
    }
    
    /**
     * fallback编码检测方法
     */
    private Charset fallbackDetectEncoding(File file) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            
            // 检测BOM
            if (bytes.length >= 3) {
                // UTF-8 BOM: EF BB BF
                if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                    logger.info("BOM检测到UTF-8编码: {}", file.getPath());
                    return StandardCharsets.UTF_8;
                }
            }
            
            if (bytes.length >= 2) {
                // UTF-16 BE BOM: FE FF
                if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                    logger.info("BOM检测到UTF-16BE编码: {}", file.getPath());
                    return StandardCharsets.UTF_16BE;
                }
                // UTF-16 LE BOM: FF FE
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                    logger.info("BOM检测到UTF-16LE编码: {}", file.getPath());
                    return StandardCharsets.UTF_16LE;
                }
            }
            
            // 尝试UTF-8解码
            try {
                String content = new String(bytes, StandardCharsets.UTF_8);
                if (!content.contains("\uFFFD")) {
                    logger.info("启发式检测到UTF-8编码: {}", file.getPath());
                    return StandardCharsets.UTF_8;
                }
            } catch (Exception ignored) {}
            
            // 中文Windows系统尝试GBK
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                try {
                    Charset gbk = Charset.forName("GBK");
                    logger.info("Windows系统默认使用GBK编码: {}", file.getPath());
                    return gbk;
                } catch (Exception ignored) {}
            }
            
            // 最终使用系统默认编码
            Charset defaultCharset = Charset.defaultCharset();
            logger.info("使用系统默认编码 {}: {}", defaultCharset.name(), file.getPath());
            return defaultCharset;
            
        } catch (IOException e) {
            logger.warn("fallback编码检测失败，使用UTF-8: {}", file.getPath(), e);
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 清理文件路径，移除不可见字符，并将相对路径转换为绝对路径
     */
    public String cleanFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        // 移除不可见的Unicode字符
        String cleaned = path.replaceAll("[\\u200B-\\u200D\\uFEFF\\u202A-\\u202E]", "");
        
        // 去除首尾空白
        cleaned = cleaned.trim();
        
        // 标准化路径分隔符 (Windows)
        cleaned = cleaned.replace("/", "\\");
        
        // 检查是否为相对路径，如果是则转换为绝对路径
        if (!isAbsolutePath(cleaned)) {
            cleaned = convertToAbsolutePath(cleaned);
        }
        
        return cleaned;
    }
    
    /**
     * 判断是否为绝对路径
     */
    private boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Windows绝对路径判断：以盘符开头(如C:)或网络路径(\\)
        return path.matches("^[a-zA-Z]:[/\\\\].*") || path.startsWith("\\\\");
    }
    
    /**
     * 将相对路径转换为绝对路径
     * 基准目录为config-tool-java项目的上级目录
     */
    private String convertToAbsolutePath(String relativePath) {
        try {
            // 获取基准目录：config-tool-java项目的上级目录
            String baseDir = getBaseDirectory();
            
            // 组合基准目录和相对路径
            File absoluteFile = new File(baseDir, relativePath);
            String absolutePath = absoluteFile.getCanonicalPath();
            
            logger.info("相对路径转换: {} -> {}", relativePath, absolutePath);
            return absolutePath;
            
        } catch (IOException e) {
            logger.error("转换相对路径失败: {}", relativePath, e);
            // 如果转换失败，返回原始路径
            return relativePath;
        }
    }
    
    /**
     * 获取基准目录：config-tool-java项目的上级目录
     */
    private String getBaseDirectory() {
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
            logger.error("获取基准目录失败", e);
            // fallback：返回当前工作目录
            return System.getProperty("user.dir");
        }
    }

    /**
     * 在文件中查找匹配项
     */
    public List<MatchResult> findMatches(String filePath, String prefix, String suffix) {
        List<MatchResult> results = new ArrayList<>();
        
        try {
            String cleanedPath = cleanFilePath(filePath);
            File file = new File(cleanedPath);
            
            if (!file.exists()) {
                logger.warn("文件不存在: {}", cleanedPath);
                return results;
            }
            
            // 检测文件编码
            Charset fileEncoding = detectFileEncoding(file);
            List<String> lines = FileUtils.readLines(file, fileEncoding);
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains(prefix)) {
                    // 如果没有指定suffix，或者suffix为空，或者line包含suffix
                    if (suffix == null || suffix.isEmpty() || line.contains(suffix)) {
                        results.add(new MatchResult(i + 1, line)); // 行号从1开始
                    }
                }
            }
            
            logger.info("在文件 {} 中找到 {} 个匹配项", cleanedPath, results.size());
            
        } catch (IOException e) {
            logger.error("读取文件失败: {}", filePath, e);
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * 从文件目标点提取当前值
     */
    public String extractCurrentValue(FileTarget target) {
        try {
            String cleanedPath = cleanFilePath(target.getFilePath());
            File file = new File(cleanedPath);
            
            if (!file.exists()) {
                return "文件不存在";
            }
            
            // 检测文件编码
            Charset fileEncoding = detectFileEncoding(file);
            List<String> lines = FileUtils.readLines(file, fileEncoding);
            
            // 检查行号是否有效
            if (target.getLineNumber() < 1 || target.getLineNumber() > lines.size()) {
                return "行号无效";
            }
            
            String line = lines.get(target.getLineNumber() - 1);
            String prefix = target.getPrefix();
            String suffix = target.getSuffix();
            
            // 查找前缀位置
            int prefixIndex = line.indexOf(prefix);
            if (prefixIndex == -1) {
                return "前缀未找到";
            }
            
            int startIndex = prefixIndex + prefix.length();
            int endIndex = line.length();
            
            // 如果指定了后缀，查找后缀位置
            if (suffix != null && !suffix.isEmpty()) {
                int suffixIndex = line.indexOf(suffix, startIndex);
                if (suffixIndex != -1) {
                    endIndex = suffixIndex;
                }
            }
            
            if (startIndex >= endIndex) {
                return "";
            }
            
            return line.substring(startIndex, endIndex);
            
        } catch (IOException e) {
            logger.error("提取当前值失败: {}", target.getFilePath(), e);
            return "读取失败";
        }
    }

    /**
     * 应用配置更改到文件
     */
    public void applyChange(FileTarget target, String newValue) {
        try {
            String cleanedPath = cleanFilePath(target.getFilePath());
            File file = new File(cleanedPath);
            
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + cleanedPath);
            }
            
            // 检测文件编码
            Charset fileEncoding = detectFileEncoding(file);
            List<String> lines = FileUtils.readLines(file, fileEncoding);
            
            // 检查行号是否有效
            if (target.getLineNumber() < 1 || target.getLineNumber() > lines.size()) {
                throw new RuntimeException("行号无效: " + target.getLineNumber());
            }
            
            String line = lines.get(target.getLineNumber() - 1);
            String prefix = target.getPrefix();
            String suffix = target.getSuffix();
            
            // 查找前缀位置
            int prefixIndex = line.indexOf(prefix);
            if (prefixIndex == -1) {
                throw new RuntimeException("前缀未找到: " + prefix);
            }
            
            int startIndex = prefixIndex + prefix.length();
            int endIndex = line.length();
            
            // 如果指定了后缀，查找后缀位置
            if (suffix != null && !suffix.isEmpty()) {
                int suffixIndex = line.indexOf(suffix, startIndex);
                if (suffixIndex != -1) {
                    endIndex = suffixIndex;
                }
            }
            
            // 构建新行
            String newLine = line.substring(0, startIndex) + newValue;
            if (suffix != null && !suffix.isEmpty() && endIndex < line.length()) {
                newLine += line.substring(endIndex);
            }
            
            // 更新行
            lines.set(target.getLineNumber() - 1, newLine);
            
            // 使用原有编码写回文件
            FileUtils.writeLines(file, fileEncoding.name(), lines);
            
            logger.info("成功更新文件 {} 第 {} 行 (编码: {})", cleanedPath, target.getLineNumber(), fileEncoding.name());
            
        } catch (IOException e) {
            logger.error("应用配置更改失败: {}", target.getFilePath(), e);
            throw new RuntimeException("应用配置更改失败: " + e.getMessage());
        }
    }
} 