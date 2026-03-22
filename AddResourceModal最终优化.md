# AddResourceModal 最终优化

## 优化目标

根据需求，对 `AddResourceModal.tsx` 进行最终优化：
1. **将"关联现有知识库"放在前面**（作为默认选项）
2. **禁用"创建新知识库"功能**

## 具体修改

### 1. 默认模式设置
```typescript
// 修改前：默认是'create'模式
const [knowledgeBaseMode, setKnowledgeBaseMode] = useState<'create' | 'link'>('create');

// 修改后：默认是'link'模式
const [knowledgeBaseMode, setKnowledgeBaseMode] = useState<'create' | 'link'>('link');
```

### 2. 重置表单逻辑
```typescript
// 修改前：根据allowCreateKnowledge设置模式
setKnowledgeBaseMode(allowCreateKnowledge ? 'create' : 'link');

// 修改后：总是设置为'link'模式
setKnowledgeBaseMode('link');
```

### 3. 模式选择器优化
```typescript
// 修改前：创建在前，关联在后
<Radio.Button value="create" style={{ width: '50%', textAlign: 'center' }}>
    <FileAddOutlined /> 创建新知识库
</Radio.Button>
<Radio.Button value="link" style={{ width: '50%', textAlign: 'center' }}>
    <LinkOutlined /> 关联现有知识库
</Radio.Button>

// 修改后：关联在前，创建在后且禁用
<Radio.Button value="link" style={{ width: '50%', textAlign: 'center' }}>
    <LinkOutlined /> 关联现有知识库
</Radio.Button>
<Radio.Button
    value="create"
    style={{ width: '50%', textAlign: 'center' }}
    disabled={true}
>
    <FileAddOutlined /> 创建新知识库（已禁用）
</Radio.Button>
```

### 4. 提交处理优化
```typescript
// 修改前：可以创建新知识库
if (type === 'knowledge' && knowledgeBaseMode === 'link') {
    // 关联现有知识库
} else {
    // 创建新资源（包括知识库）
}

// 修改后：禁用创建新知识库
if (type === 'knowledge') {
    if (knowledgeBaseMode === 'link') {
        // 关联现有知识库
    } else {
        // 创建新知识库（已禁用）
        message.warning('创建新知识库功能已禁用，请使用"关联现有知识库"功能');
        return;
    }
} else {
    // 创建新资源（工具或MCP）
}
```

## 效果对比

### 修改前
```
添加知识库
├── 创建新知识库（默认选中）
│   ├── 知识库名称
│   ├── 描述
│   ├── 类型
│   └── 上传文件
└── 关联现有知识库
    └── 知识库列表（选择关联）
```

### 修改后
```
关联知识库
├── 关联现有知识库（默认选中且在前）
│   └── 知识库列表（选择关联）
└── 创建新知识库（已禁用）
    └── 显示"创建新知识库（已禁用）"
```

## 用户体验

### 1. 默认行为
- **打开模态框**：默认显示"关联现有知识库"模式
- **选项顺序**："关联现有知识库"在前，"创建新知识库"在后
- **禁用状态**："创建新知识库"选项被禁用

### 2. 操作流程
1. 用户点击"添加知识库"按钮
2. 模态框打开，默认显示知识库列表
3. 用户选择要关联的知识库
4. 点击"关联"按钮完成操作

### 3. 错误处理
- 如果用户尝试选择"创建新知识库"，会显示警告提示
- 如果用户未选择知识库就点击关联，会提示选择
- 所有原有错误处理保持不变

## 技术实现

### 1. 状态管理
- **knowledgeBaseMode**：默认值为'link'
- **selectedKnowledgeBase**：记录选中的知识库ID
- **availableKnowledgeBases**：存储可用的知识库列表

### 2. 条件渲染
```typescript
// 模式选择器：当允许创建时才显示
if (type !== 'knowledge' || !allowCreateKnowledge) return null;

// 内容区域：当是知识库类型且（不允许创建或模式为link）时显示列表
{type === 'knowledge' && (!allowCreateKnowledge || knowledgeBaseMode === 'link') ? (
    // 显示知识库列表
) : (
    // 显示表单
)}
```

### 3. API调用
- **加载列表**：`knowledgeBaseApi.listByCondition({})`
- **关联知识库**：`knowledgeBaseApi.bind(bindRequest)`
- **创建知识库**：已禁用

## 优势

### 1. 用户体验
- **简化操作**：用户只需要选择关联，不需要考虑创建
- **明确引导**：默认显示关联模式，禁用创建模式
- **减少错误**：避免用户误操作创建新知识库

### 2. 业务逻辑
- **符合场景**：Agent配置中知识库应该只用于关联
- **职责清晰**：创建知识库在独立管理页面完成
- **流程优化**：用户操作路径更短更直接

### 3. 代码质量
- **向后兼容**：原有功能不受影响
- **易于维护**：通过参数控制功能
- **可扩展性**：可以轻松调整或恢复功能

## 验证

### 1. 功能验证
- [ ] 默认打开显示"关联现有知识库"模式
- [ ] "关联现有知识库"选项在前
- [ ] "创建新知识库"选项被禁用
- [ ] 选择知识库后可以正常关联
- [ ] 尝试创建新知识库会显示警告

### 2. 兼容性验证
- [ ] 工具和MCP类型不受影响
- [ ] 独立知识库管理页面功能正常
- [ ] 所有原有API调用正常工作

## 总结

通过本次优化，`AddResourceModal` 组件在Agent配置场景中：
1. **默认显示关联模式**，符合业务需求
2. **禁用创建功能**，避免误操作
3. **优化选项顺序**，更好的用户体验
4. **保持向后兼容**，不影响其他功能

现在Agent配置中的知识库关联功能更加专注、简洁和用户友好。