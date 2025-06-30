package processor

import (
	"bufio"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"unicode"

	"config-tool-desktop/internal/template"
)

// FileProcessor 文件处理器
type FileProcessor struct{}

// NewFileProcessor 创建文件处理器
func NewFileProcessor() *FileProcessor {
	return &FileProcessor{}
}

// cleanPath 清理文件路径，移除不可见字符和标准化路径
func (fp *FileProcessor) cleanPath(path string) string {
	// 移除所有不可见的Unicode字符
	cleaned := strings.Map(func(r rune) rune {
		if unicode.IsControl(r) || r == '\u202A' || r == '\u202B' || r == '\u202C' || r == '\u202D' || r == '\u202E' {
			return -1 // 删除这些字符
		}
		return r
	}, path)

	// 去除首尾空白
	cleaned = strings.TrimSpace(cleaned)

	// 标准化路径
	cleaned = filepath.Clean(cleaned)

	return cleaned
}

// GetValue 根据FileTarget读取当前值
func (fp *FileProcessor) GetValue(target *template.FileTarget) (string, error) {
	cleanedPath := fp.cleanPath(target.FilePath)
	file, err := os.Open(cleanedPath)
	if err != nil {
		return "", fmt.Errorf("打开文件失败: %v", err)
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	currentLine := 0

	for scanner.Scan() {
		currentLine++
		if currentLine == target.LineNumber {
			lineContent := scanner.Text()
			return fp.extractValue(lineContent, target.Prefix, target.Suffix), nil
		}
	}

	return "", fmt.Errorf("未找到第%d行", target.LineNumber)
}

// ApplyModifications 执行文件修改
func (fp *FileProcessor) ApplyModifications(mods []template.ModificationTask) error {
	// 按文件分组处理
	fileGroups := make(map[string][]template.ModificationTask)
	for _, mod := range mods {
		cleanedPath := fp.cleanPath(mod.Target.FilePath)
		fileGroups[cleanedPath] = append(fileGroups[cleanedPath], mod)
	}

	// 逐个文件处理
	for filePath, fileMods := range fileGroups {
		err := fp.processFile(filePath, fileMods)
		if err != nil {
			return fmt.Errorf("处理文件%s失败: %v", filePath, err)
		}
	}

	return nil
}

// FindMatches 查找文件中的匹配项
func (fp *FileProcessor) FindMatches(filePath, prefix, suffix string) ([]template.MatchResult, error) {
	cleanedPath := fp.cleanPath(filePath)
	file, err := os.Open(cleanedPath)
	if err != nil {
		return nil, fmt.Errorf("打开文件失败: %v", err)
	}
	defer file.Close()

	var results []template.MatchResult
	scanner := bufio.NewScanner(file)
	lineNumber := 0

	for scanner.Scan() {
		lineNumber++
		lineContent := scanner.Text()

		// 检查是否匹配前缀
		if strings.Contains(lineContent, prefix) {
			// 如果有后缀，也要检查后缀
			if suffix == "" || strings.Contains(lineContent, suffix) {
				results = append(results, template.MatchResult{
					LineNumber:  lineNumber,
					LineContent: lineContent,
				})
			}
		}
	}

	return results, nil
}

// extractValue 从行内容中提取值
func (fp *FileProcessor) extractValue(lineContent, prefix, suffix string) string {
	// 找到前缀位置
	prefixIndex := strings.Index(lineContent, prefix)
	if prefixIndex == -1 {
		return ""
	}

	startIndex := prefixIndex + len(prefix)

	// 如果没有后缀，取从前缀后到行尾的内容
	if suffix == "" {
		return strings.TrimSpace(lineContent[startIndex:])
	}

	// 找到后缀位置
	suffixIndex := strings.Index(lineContent[startIndex:], suffix)
	if suffixIndex == -1 {
		return strings.TrimSpace(lineContent[startIndex:])
	}

	return strings.TrimSpace(lineContent[startIndex : startIndex+suffixIndex])
}

// processFile 处理单个文件的所有修改
func (fp *FileProcessor) processFile(filePath string, mods []template.ModificationTask) error {
	// 读取文件所有行
	file, err := os.Open(filePath)
	if err != nil {
		return err
	}

	var lines []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		lines = append(lines, scanner.Text())
	}
	file.Close()

	// 应用修改
	for _, mod := range mods {
		if mod.Target.LineNumber > 0 && mod.Target.LineNumber <= len(lines) {
			lineIndex := mod.Target.LineNumber - 1
			oldLine := lines[lineIndex]
			newLine := fp.buildNewLine(oldLine, mod.Target.Prefix, mod.Target.Suffix, mod.NewValue)
			lines[lineIndex] = newLine
		}
	}

	// 写回文件
	outFile, err := os.Create(filePath)
	if err != nil {
		return err
	}
	defer outFile.Close()

	writer := bufio.NewWriter(outFile)
	for _, line := range lines {
		fmt.Fprintln(writer, line)
	}
	writer.Flush()

	return nil
}

// buildNewLine 构建新的行内容
func (fp *FileProcessor) buildNewLine(oldLine, prefix, suffix, newValue string) string {
	// 如果没有后缀，替换整行为 prefix + newValue
	if suffix == "" {
		return prefix + newValue
	}

	// 有后缀的情况，替换中间部分
	prefixIndex := strings.Index(oldLine, prefix)
	if prefixIndex == -1 {
		return oldLine // 找不到前缀，保持原样
	}

	beforePrefix := oldLine[:prefixIndex]
	afterPrefixStart := prefixIndex + len(prefix)

	suffixIndex := strings.Index(oldLine[afterPrefixStart:], suffix)
	if suffixIndex == -1 {
		// 找不到后缀，从前缀后开始替换到行尾
		return beforePrefix + prefix + newValue
	}

	afterSuffix := oldLine[afterPrefixStart+suffixIndex:]
	return beforePrefix + prefix + newValue + afterSuffix
}
