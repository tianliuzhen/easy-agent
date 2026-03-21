### agent[AgentConfig.tsx](../../AgentConfig.tsx) 改造
中间的知识库添加+按钮，可以关联到外边的知识库


### [IndexLayout.tsx](../../../IndexLayout.tsx) 改造
添加 知识库主菜单 我的知识库子菜单
并且我的知识库指向文件  [KnowledgeBaseList.tsx](KnowledgeBaseList.tsx)


### 后端接口改造
新增了 EaKnowledgeRelationDAO，已经生成数据表 （知识库关联Agent关系表）, 为了适配 AgentConfig.tsx
在 com.aaa.easyagent.core.service.KnowledgeBaseService 接口添加新的方法
要求所有的接口都使用request 对象格式接受参数
1. 查询AgentID下面的知识库
2. 绑定知识库和AgentID



### 知识库改造实施计划
# 知识库改造实施计划

## 上下文
根据用户提供的计划文档，需要实现以下改造：
1. **前端 AgentConfig.tsx 改造** - 在知识库区域添加"+"按钮，可以关联到外部的知识库
2. **前端 IndexLayout.tsx 改造** - 添加知识库主菜单和"我的知识库"子菜单，指向 KnowledgeBaseList.tsx
3. **后端接口改造** - 新增 EaKnowledgeRelationDAO（已创建），在 KnowledgeBaseService 中添加新方法，要求所有接口使用 request 对象格式接受参数

## 当前状态分析

### 已存在的组件和文件
1. **前端**:
    - `AgentConfig.tsx` - Agent配置主页面，包含三个面板
    - `ResourceBindingPanel.tsx` - 资源绑定面板，包含知识库、工具、MCP三个折叠面板
    - `KnowledgeBaseList.tsx` - 知识库列表组件（已存在但使用模拟数据）
    - `AddResourceModal.tsx` - 添加资源模态框
    - `IndexLayout.tsx` - 主布局文件，包含菜单系统

2. **后端**:
    - `KnowledgeBaseService.java` - 知识库服务接口（已有 listKnowledgeBaseByAgentId 方法）
    - `KnowledgeBaseServiceImpl.java` - 服务实现
    - `EaKnowledgeRelationDO.java` - 知识库关联实体（已创建）
    - `EaKnowledgeRelationDAO.java` - 知识库关联DAO（已创建）
    - `KnowledgeController.java` - 知识库控制器

3. **API**:
    - `KnowledgeBaseApi.ts` - 前端API调用

### 需要解决的问题
1. 现有的知识库关联逻辑不清晰 - EaKnowledgeBaseDO 中已有 agentId 字段，但新增了 EaKnowledgeRelationDO 表。根据用户选择，将使用 EaKnowledgeRelationDO 表实现多对多关联关系。
2. 前端知识库列表使用模拟数据，需要连接真实API
3. 添加资源模态框需要支持关联现有知识库（而不仅仅是上传新文件）
4. 菜单系统需要添加知识库管理入口

## 实施计划

### 阶段1：后端接口改造

#### 1.1 更新 KnowledgeBaseService 接口
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\core\service\KnowledgeBaseService.java`

需要添加以下方法（使用Request对象格式）：
1. `List<EaKnowledgeBaseDO> listKnowledgeBaseByAgentId(KnowledgeBaseQueryRequest request)` - 查询Agent关联的知识库
2. `void bindKnowledgeBaseToAgent(KnowledgeBaseBindRequest request)` - 绑定知识库到Agent
3. `void unbindKnowledgeBaseFromAgent(KnowledgeBaseUnbindRequest request)` - 解绑知识库

需要创建Request对象：
- `KnowledgeBaseQueryRequest` - 包含 agentId
- `KnowledgeBaseBindRequest` - 包含 agentId, knowledgeBaseId
- `KnowledgeBaseUnbindRequest` - 包含 agentId, knowledgeBaseId

#### 1.2 更新 KnowledgeBaseServiceImpl 实现
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\core\service\impl\KnowledgeBaseServiceImpl.java`

实现新的接口方法，使用 EaKnowledgeRelationDAO 进行关联关系管理。

#### 1.3 更新 KnowledgeController
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\web\biz\knowledge\KnowledgeController.java`

添加新的API端点：
1. `POST /knowledge/listByAgentId` - 改为使用Request对象
2. `POST /knowledge/bind` - 绑定知识库
3. `POST /knowledge/unbind` - 解绑知识库

#### 1.4 创建Request对象
在合适的包下创建Request DTO类。

### 阶段2：前端API层更新

#### 2.1 更新 KnowledgeBaseApi.ts
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\api\KnowledgeBaseApi.ts`

添加新的API方法：
1. `bindKnowledgeBase` - 绑定知识库
2. `unbindKnowledgeBase` - 解绑知识库
3. 更新现有的 `listByAgentId` 方法使用Request对象格式

### 阶段3：前端组件改造

#### 3.1 改造 AddResourceModal.tsx
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\page\agent\common\AddResourceModal.tsx`

为知识库类型添加新的选项：
- 选项1：创建新知识库（现有功能）
- 选项2：关联现有知识库（新功能）

关联现有知识库时需要：
1. 显示所有可用的知识库列表
2. 支持搜索和筛选
3. 选择后调用绑定API

#### 3.2 改造 KnowledgeBaseList.tsx
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\page\agent\knowledge\KnowledgeBaseList.tsx`

1. 移除模拟数据，连接真实API
2. 添加解绑功能按钮
3. 根据agentId参数显示关联的知识库

#### 3.3 更新 ResourceBindingPanel.tsx
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\page\agent\ResourceBindingPanel.tsx`

确保知识库列表正确传递agentId参数。

### 阶段4：菜单系统改造

#### 4.1 改造 IndexLayout.tsx
**文件**: `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\IndexLayout.tsx`

在routes数组中添加新的菜单项：
```javascript
{
    key: 'sub5',
    icon: <BookOutlined />, // 需要导入图标
    label: '知识库管理',
    children: [
        {
            key: '4',
            label: '我的知识库',
            path: '/page/knowledge/KnowledgeBaseList',
            component: <AuthGuard><KnowledgeBaseList /></AuthGuard>
        },
    ]
}
```

需要：
1. 导入 `BookOutlined` 图标
2. 导入 `KnowledgeBaseList` 组件
3. 创建对应的路由

#### 4.2 创建独立的知识库管理页面
可能需要创建一个新的页面组件来包装 `KnowledgeBaseList`，使其可以作为独立页面使用。

### 阶段5：测试和验证

#### 5.1 后端接口测试
测试所有新的API端点。

#### 5.2 前端功能测试
测试：
1. 菜单导航到知识库管理页面
2. Agent配置页面中的知识库关联功能
3. 绑定和解绑操作

## 关键文件路径

### 后端文件
1. `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\core\service\KnowledgeBaseService.java`
2. `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\core\service\impl\KnowledgeBaseServiceImpl.java`
3. `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\web\biz\knowledge\KnowledgeController.java`
4. `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\core\domain\DO\EaKnowledgeRelationDO.java`
5. `F:\WorkSpace\MyGithub\easy-agent\ea-service\src\main\java\com\aaa\easyagent\core\mapper\EaKnowledgeRelationDAO.java`

### 前端文件
1. `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\page\agent\ResourceBindingPanel.tsx`
2. `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\page\agent\common\AddResourceModal.tsx`
3. `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\page\agent\knowledge\KnowledgeBaseList.tsx`
4. `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\IndexLayout.tsx`
5. `F:\WorkSpace\MyGithub\easy-agent\ea-web\src\views\api\KnowledgeBaseApi.ts`

## 注意事项

1. **数据一致性**: 需要确保 EaKnowledgeBaseDO 中的 agentId 字段和 EaKnowledgeRelationDO 表的数据一致性
2. **错误处理**: 所有API调用需要适当的错误处理
3. **用户体验**: 添加资源时提供清晰的选项（新建 vs 关联现有）
4. **权限控制**: 确保用户只能访问自己有权限的知识库

## 验证计划

1. 启动后端服务，验证新API端点可访问
2. 在前端测试菜单导航到知识库管理页面
3. 创建Agent，测试知识库关联功能
4. 验证绑定和解绑操作正常工作
5. 测试知识库列表正确显示关联关系
