// 全局变量
let currentTemplate = null;
let templates = [];
let editingTemplate = null;
let selectedConfigItem = null;
let currentMatches = [];

// 工具函数：清理文件路径，移除不可见字符
function cleanFilePath(path) {
    if (!path) return '';
    
    // 移除不可见的Unicode字符
    let cleaned = path.replace(/[\u200B-\u200D\uFEFF\u202A-\u202E]/g, '');
    
    // 去除首尾空白
    cleaned = cleaned.trim();
    
    // 标准化路径分隔符 (Windows)
    cleaned = cleaned.replace(/\//g, '\\');
    
    return cleaned;
}

// 显示成功消息
function showSuccess(message) {
    const banner = document.createElement('div');
    banner.className = 'success-banner';
    banner.innerHTML = message;
    document.body.insertBefore(banner, document.body.firstChild);
    setTimeout(() => banner.remove(), 3000);
}

// 显示错误消息
function showError(message) {
    const banner = document.createElement('div');
    banner.className = 'error-banner';
    banner.innerHTML = message;
    document.body.insertBefore(banner, document.body.firstChild);
    setTimeout(() => banner.remove(), 5000);
}

// 页面加载时获取模板列表
document.addEventListener('DOMContentLoaded', function() {
    loadTemplates();
});

// 加载模板列表
async function loadTemplates() {
    try {
        const response = await fetch('/api/templates');
        const result = await response.json();
        templates = result.data || [];
        
        // 临时调试：打印每个模板的配置项数量
        console.log('重新加载模板数据:');
        templates.forEach((template, index) => {
            console.log(`模板${index}: ${template.name}, 配置项数量: ${template.items ? template.items.length : 0}`);
            if (template.items && template.items.length > 0) {
                template.items.forEach((item, i) => {
                    console.log(`  配置项${i}: ${item.name}`);
                });
            }
        });
        
        renderTemplateList();
    } catch (error) {
        console.error('加载模板失败:', error);
        showError('❌ 加载模板失败: ' + error.message);
    }
}

// 渲染模板列表
function renderTemplateList() {
    const container = document.getElementById('templateList');
    container.innerHTML = '';
    
    if (templates.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #6c757d; margin-top: 20px; font-size: 14px;">暂无模板，点击上方按钮创建</p>';
        return;
    }
    
    templates.forEach(template => {
        const div = document.createElement('div');
        div.className = 'template-item';
        if (currentTemplate && currentTemplate.id === template.id) {
            div.classList.add('active');
        }
        
        let descHtml = '';
        if (template.description && template.description.trim()) {
            descHtml = `<div class="template-desc">${template.description}</div>`;
        }
        
        div.innerHTML = `
            <div class="template-name">${template.name}</div>
            ${descHtml}
        `;
        
        div.onclick = () => selectTemplate(template.id);
        container.appendChild(div);
    });
}

// 选择模板
async function selectTemplate(templateId) {
    try {
        // 从本地数组中查找模板
        let template = templates.find(t => t.id === templateId);
        
        if (!template) {
            showError('模板不存在');
            return;
        }
        
        // 获取模板并填充当前值
        const response = await fetch(`/api/templates/${templateId}`);
        const result = await response.json();
        
        if (response.ok) {
            currentTemplate = result.data;
            renderWorkspace();
            renderTemplateList(); // 更新活动状态
        } else {
            showError('加载模板失败: ' + result.error);
        }
    } catch (error) {
        console.error('选择模板失败:', error);
        showError('选择模板失败: ' + error.message);
    }
}

// 渲染工作区
function renderWorkspace() {
    const workspace = document.getElementById('workspace');
    
    if (!currentTemplate) {
        workspace.innerHTML = `
            <div class="empty-state">
                <h3>👋 欢迎使用配置文件修改工具</h3>
                <p>请在左侧选择一个模板开始配置，或创建新模板</p>
            </div>
        `;
        return;
    }

    let descHtml = '';
    if (currentTemplate.description && currentTemplate.description.trim()) {
        descHtml = `<p>${currentTemplate.description}</p>`;
    }

    let html = `
        <div class="template-header">
            <div class="template-info">
                <h2>${currentTemplate.name}</h2>
                ${descHtml}
            </div>
            <button class="btn btn-primary" onclick="editTemplate('${currentTemplate.id}')">
                <span>✏️</span> 编辑模板
            </button>
        </div>
    `;

    if (currentTemplate.items && currentTemplate.items.length > 0) {
        currentTemplate.items.forEach((item, index) => {
            html += `
                <div class="config-item">
                    <label>${item.name}</label>
                    <div class="current-value">当前值: ${item.currentValue || '未设置'}</div>
                    <input 
                        type="text" 
                        id="item_${index}"
                        placeholder="输入新值"
                        value="${item.defaultValue || ''}"
                    >
                    ${item.description && item.description.trim() ? `<small style="color: #6c757d;">${item.description}</small>` : ''}
                </div>
            `;
        });

        html += `
            <div style="margin-top: 32px; text-align: center;">
                <button class="btn btn-success btn-large" onclick="applyChanges()">
                    <span>🚀</span> 一键应用配置
                </button>
            </div>
        `;
    } else {
        html += `
            <div class="empty-state">
                <h3>📝 此模板暂无配置项</h3>
                <p>点击"编辑模板"按钮添加配置项</p>
            </div>
        `;
    }

    workspace.innerHTML = html;
}

// 创建新模板
function createTemplate() {
    editingTemplate = {
        name: '',
        description: '',
        items: []
    };
    selectedConfigItem = null;
    showTemplateEditor();
}

// 编辑模板 - 直接从API获取最新数据
async function editTemplate(templateId) {
    try {
        console.log('编辑模板 ID:', templateId);
        
        // 直接从API获取最新的模板数据，和主页选择模板一样的逻辑
        const response = await fetch(`/api/templates/${templateId}`);
        const result = await response.json();
        
        if (response.ok) {
            editingTemplate = result.data;
            console.log('从API获取的模板数据:', editingTemplate);
            console.log('配置项数量:', editingTemplate.items ? editingTemplate.items.length : 0);
            
            // 确保items是数组而不是null
            if (!editingTemplate.items || !Array.isArray(editingTemplate.items)) {
                editingTemplate.items = [];
            }
            
            selectedConfigItem = null;
            showTemplateEditor();
        } else {
            showError('加载模板失败: ' + result.error);
        }
    } catch (error) {
        console.error('编辑模板失败:', error);
        showError('编辑模板失败: ' + error.message);
    }
}

// 显示模板编辑器
function showTemplateEditor() {
    console.log('显示模板编辑器, editingTemplate:', editingTemplate);
    
    document.getElementById('templateName').value = editingTemplate.name;
    document.getElementById('templateDescription').value = editingTemplate.description;
    renderConfigItemsList();
    document.getElementById('targetsSection').style.display = 'none';
    document.getElementById('templateEditorModal').classList.add('show');
}

// 保存模板 - 修复配置项丢失问题
async function saveTemplate() {
    if (!editingTemplate) {
        showError('没有正在编辑的模板');
        return;
    }

    // 获取模板名称和描述
    editingTemplate.name = document.getElementById('templateName').value.trim();
    editingTemplate.description = document.getElementById('templateDescription').value.trim();

    if (!editingTemplate.name) {
        showError('请输入模板名称');
        return;
    }

    try {
        console.log('准备保存模板:', editingTemplate);
        
        const response = await fetch('/api/templates', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=utf-8'
            },
            body: JSON.stringify(editingTemplate)
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('保存失败:', errorText);
            throw new Error('保存失败: ' + errorText);
        }

        const result = await response.json();
        console.log('保存成功:', result);
        
        // 如果是新模板，更新ID
        if (!editingTemplate.id && result.data && result.data.id) {
            editingTemplate.id = result.data.id;
        }
        
        console.log('模板保存成功，重新加载模板列表...');
        
        // 重新从后端加载模板列表，确保数据同步
        await loadTemplates();
        
        showSuccess('✅ 模板保存成功');
    } catch (error) {
        console.error('保存模板失败:', error);
        showError('❌ 保存失败: ' + error.message);
    }
}

// 关闭模板编辑器
function closeTemplateEditor() {
    document.getElementById('templateEditorModal').classList.remove('show');
    editingTemplate = null;
    selectedConfigItem = null;
}

// 显示添加配置项表单
function showAddConfigItemForm() {
    document.getElementById('newConfigItemName').value = '';
    document.getElementById('newConfigItemDescription').value = '';
    document.getElementById('newConfigItemDefaultValue').value = '';
    document.getElementById('addConfigItemModal').classList.add('show');
}

// 关闭添加配置项表单
function closeAddConfigItemForm() {
    document.getElementById('addConfigItemModal').classList.remove('show');
}

// 确认添加配置项
function confirmAddConfigItem() {
    const name = document.getElementById('newConfigItemName').value.trim();
    const description = document.getElementById('newConfigItemDescription').value.trim();
    const defaultValue = document.getElementById('newConfigItemDefaultValue').value.trim();

    if (!name) {
        showError('请输入配置项名称');
        return;
    }

    const newItem = {
        id: 'item-' + Date.now(),
        name: name,
        description: description,
        defaultValue: defaultValue,
        targets: []
    };
    
    // 确保editingTemplate.items是数组
    if (!editingTemplate.items || !Array.isArray(editingTemplate.items)) {
        editingTemplate.items = [];
    }

    editingTemplate.items.push(newItem);
    renderConfigItemsList();
    closeAddConfigItemForm();
    showSuccess(`✅ 配置项"${name}"添加成功`);
}

// 渲染配置项列表
function renderConfigItemsList() {
    const container = document.getElementById('configItemsList');
    container.innerHTML = '';
    
    console.log('渲染配置项列表, editingTemplate:', editingTemplate);
    
    // 确保items是数组
    if (!editingTemplate.items || !Array.isArray(editingTemplate.items)) {
        editingTemplate.items = [];
    }
    
    console.log('配置项数量:', editingTemplate.items.length);
    
    if (editingTemplate.items.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #6c757d; padding: 20px; font-size: 14px;">暂无配置项，点击上方按钮添加</p>';
        return;
    }
    
    editingTemplate.items.forEach((item, index) => {
        const div = document.createElement('div');
        div.className = 'config-item-row';
        if (selectedConfigItem && selectedConfigItem.id === item.id) {
            div.classList.add('selected');
        }
        
        let descHtml = '';
        if (item.description && item.description.trim()) {
            descHtml = `<div class="config-item-desc">${item.description}</div>`;
        }
        
        div.innerHTML = `
            <div class="config-item-info">
                <div class="config-item-name">${item.name}</div>
                ${descHtml}
                <div class="config-item-meta">${(item.targets && item.targets.length) || 0} 个目标点</div>
            </div>
            <div class="config-item-actions">
                <button class="btn btn-danger btn-small" onclick="deleteConfigItem(${index})">🗑️ 删除</button>
            </div>
        `;
        div.onclick = (e) => {
            if (!e.target.classList.contains('btn')) {
                selectConfigItem(item);
            }
        };
        container.appendChild(div);
    });
}

// 删除配置项
function deleteConfigItem(index) {
    const item = editingTemplate.items[index];
    if (confirm(`确定要删除配置项"${item.name}"吗？`)) {
        editingTemplate.items.splice(index, 1);
        if (selectedConfigItem && selectedConfigItem === item) {
            selectedConfigItem = null;
            document.getElementById('targetsSection').style.display = 'none';
        }
        renderConfigItemsList();
        showSuccess('✅ 配置项删除成功');
    }
}

// 选择配置项
function selectConfigItem(item) {
    selectedConfigItem = item;
    renderConfigItemsList();
    renderTargetsList();
    document.getElementById('targetsSection').style.display = 'block';
}

// 渲染目标点列表
function renderTargetsList() {
    const container = document.getElementById('targetsList');
    container.innerHTML = '';
    
    if (!selectedConfigItem) {
        container.innerHTML = '<p style="text-align: center; color: #6c757d; padding: 20px;">请先选择一个配置项</p>';
        return;
    }
    
    // 确保targets是数组
    if (!selectedConfigItem.targets) {
        selectedConfigItem.targets = [];
    }
    
    if (selectedConfigItem.targets.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #6c757d; padding: 20px; font-size: 14px;">暂无目标点，点击上方按钮添加</p>';
        return;
    }
    
    selectedConfigItem.targets.forEach((target, index) => {
        const div = document.createElement('div');
        div.className = 'target-item';
        div.innerHTML = `
            <div class="target-file">${target.filePath}</div>
            <div class="target-details">
                <span>行号: ${target.lineNumber} | 前缀: "${target.prefix}" | 后缀: "${target.suffix || '(空)'}"</span>
                <button class="btn btn-danger btn-small" onclick="deleteTarget(${index})">🗑️ 删除</button>
            </div>
        `;
        container.appendChild(div);
    });
}

// 删除目标点
function deleteTarget(index) {
    if (confirm('确定要删除这个目标点吗？')) {
        selectedConfigItem.targets.splice(index, 1);
        renderTargetsList();
        renderConfigItemsList(); // 更新目标点数量显示
        showSuccess('✅ 目标点删除成功');
    }
}

// 添加目标点
function addTarget() {
    if (!selectedConfigItem) {
        showError('请先选择一个配置项');
        return;
    }
    document.getElementById('fileSearchModal').classList.add('show');
}

// 关闭文件搜索
function closeFileSearch() {
    document.getElementById('fileSearchModal').classList.remove('show');
    document.getElementById('targetFilePath').value = '';
    document.getElementById('searchPrefix').value = '';
    document.getElementById('searchSuffix').value = '';
    document.getElementById('matchResultsSection').style.display = 'none';
    document.getElementById('confirmSelectionBtn').style.display = 'none';
    currentMatches = [];
}

// 选择文件（模拟文件选择）
function selectFile() {
    const filePath = prompt('请输入完整的文件路径:');
    if (filePath) {
        const cleanedPath = cleanFilePath(filePath);
        document.getElementById('targetFilePath').value = cleanedPath;
    }
}

// 查找匹配项
async function findMatches() {
    const rawFilePath = document.getElementById('targetFilePath').value.trim();
    const filePath = cleanFilePath(rawFilePath);
    const prefix = document.getElementById('searchPrefix').value.trim();
    const suffix = document.getElementById('searchSuffix').value.trim();
    
    if (!filePath || !prefix) {
        showError('请输入文件路径和前缀');
        return;
    }
    
    // 更新清理后的路径到输入框
    document.getElementById('targetFilePath').value = filePath;
    
    try {
        const response = await fetch('/api/files/find-matches', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                filePath: filePath,
                prefix: prefix,
                suffix: suffix
            })
        });
        
        const result = await response.json();
        if (response.ok) {
            currentMatches = result.data || [];
            renderMatchResults();
        } else {
            showError('搜索失败: ' + result.error);
        }
    } catch (error) {
        console.error('搜索失败:', error);
        showError('搜索失败: ' + error.message);
    }
}

// 渲染匹配结果
function renderMatchResults() {
    const container = document.getElementById('matchResults');
    container.innerHTML = '';
    
    if (currentMatches.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #6c757d; padding: 20px;">没有找到匹配项，请检查文件路径和前缀</p>';
        document.getElementById('matchResultsSection').style.display = 'block';
        return;
    }
    
    currentMatches.forEach((match, index) => {
        const div = document.createElement('div');
        div.className = 'match-item';
        div.innerHTML = `
            <input type="checkbox" id="match_${index}" value="${index}">
            <label for="match_${index}" class="match-line-number">行 ${match.lineNumber}</label>
            <div class="match-content">${match.lineContent}</div>
        `;
        container.appendChild(div);
    });
    
    document.getElementById('matchResultsSection').style.display = 'block';
    document.getElementById('confirmSelectionBtn').style.display = 'inline-flex';
}

// 确认选择
function confirmSelection() {
    const checkboxes = document.querySelectorAll('#matchResults input[type="checkbox"]:checked');
    
    if (checkboxes.length === 0) {
        showError('请至少选择一个匹配项');
        return;
    }
    
    const rawFilePath = document.getElementById('targetFilePath').value.trim();
    const filePath = cleanFilePath(rawFilePath);
    const prefix = document.getElementById('searchPrefix').value.trim();
    const suffix = document.getElementById('searchSuffix').value.trim();
    
    // 确保targets是数组
    if (!selectedConfigItem.targets) {
        selectedConfigItem.targets = [];
    }
    
    checkboxes.forEach(checkbox => {
        const index = parseInt(checkbox.value);
        const match = currentMatches[index];
        
        const target = {
            id: 'target-' + Date.now() + '-' + Math.random(),
            filePath: filePath,  // 使用清理后的路径
            lineNumber: match.lineNumber,
            prefix: prefix,
            suffix: suffix
        };
        
        selectedConfigItem.targets.push(target);
    });
    
    renderTargetsList();
    renderConfigItemsList(); // 更新目标点数量显示
    closeFileSearch();
    showSuccess(`✅ 成功添加 ${checkboxes.length} 个目标点`);
}

// 应用修改
async function applyChanges() {
    if (!currentTemplate || !currentTemplate.items) {
        showError('没有可应用的配置项');
        return;
    }

    const modifications = [];

    currentTemplate.items.forEach((item, index) => {
        const input = document.getElementById(`item_${index}`);
        const newValue = input.value.trim();
        
        if (newValue && item.targets) {
            item.targets.forEach(target => {
                modifications.push({
                    target: target,
                    newValue: newValue
                });
            });
        }
    });

    if (modifications.length === 0) {
        showError('没有需要修改的配置项，请填写新值');
        return;
    }

    try {
        const response = await fetch('/api/apply', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                modifications: modifications
            })
        });

        const result = await response.json();
        if (response.ok) {
            showSuccess('✅ 配置应用成功! 已修改 ' + modifications.length + ' 个位置');
            // 重新加载当前模板以显示新的当前值
            selectTemplate(currentTemplate.id);
        } else {
            showError('应用失败: ' + result.error);
        }
    } catch (error) {
        console.error('应用修改失败:', error);
        showError('应用失败: ' + error.message);
    }
} 