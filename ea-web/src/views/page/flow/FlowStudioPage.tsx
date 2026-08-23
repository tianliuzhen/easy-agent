import {useCallback, useEffect, useMemo, useRef, useState, type DragEvent} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {App, Button, Divider, Form, Input, InputNumber, Modal, Select, Space, Spin, Tag, Typography} from 'antd';
import {ArrowLeftOutlined, BugOutlined, CheckCircleOutlined, CloseOutlined, DeleteOutlined, FullscreenExitOutlined, FullscreenOutlined, MinusCircleOutlined, PlusOutlined, RocketOutlined, SaveOutlined} from '@ant-design/icons';
import {
  Background, Controls, Handle, MiniMap, Position, ReactFlow, ReactFlowProvider, addEdge,
  useEdgesState, useNodesState, useReactFlow, type Connection, type Edge, type Node, type NodeProps,
} from '@xyflow/react';
import {fetchEventSource} from '@microsoft/fetch-event-source';
import '@xyflow/react/dist/style.css';
import './flowStudio.css';
import {flowApi, type FlowDefinition, type FlowEdgeDefinition, type FlowNodeDefinition, type NodeMetadata, type PortDefinition, type PortKind, type ValidationIssue} from '../../api/FlowApi';
import {eaAgentApi} from '../../api/EaAgentApi';

interface StudioNodeData extends Record<string, unknown> {
  label: string;
  metadata: NodeMetadata;
  config: Record<string, unknown>;
  runStatus?: string;
  onDelete?: () => void;
}
type StudioNode = Node<StudioNodeData, 'studio'>;
type StudioEdge = Edge<{kind: PortKind}>;
interface AgentOption {id: number; agentName: string;}
interface RouterBranch {key: string; label: string; description: string;}

const portColor: Record<PortKind, string> = {CONTROL: '#1677ff', DATA: '#722ed1', WORKER: '#fa8c16'};
const fullscreenDraftKey = (flowId: number) => `easy-agent:flow-fullscreen:${flowId}`;
const handleId = (port: PortDefinition) => `${port.kind}:${port.key}`;
const isRouterBranch = (value: unknown): value is RouterBranch => typeof value === 'object' && value !== null && 'key' in value;
const routerBranches = (config: Record<string, unknown>): RouterBranch[] =>
  Array.isArray(config.branches) ? config.branches.filter(isRouterBranch).map(branch => ({
    key: String(branch.key || ''), label: String(branch.label || branch.key || ''), description: String(branch.description || ''),
  })) : [];
const withDynamicPorts = (meta: NodeMetadata, config: Record<string, unknown>): NodeMetadata => meta.type !== 'ROUTER' ? meta : ({
  ...meta,
  outputPorts: [
    ...meta.outputPorts.filter(port => port.kind !== 'CONTROL'),
    ...routerBranches(config).filter(branch => branch.key.trim()).map(branch => ({
      key: branch.key.trim(), name: branch.label || branch.key, kind: 'CONTROL' as const,
      valueType: 'ANY' as const, required: false, multiple: true,
    })),
  ],
});

const StudioNodeView = ({data, selected}: NodeProps<StudioNode>) => <div className={`flow-node ${selected ? 'selected' : ''} ${data.runStatus || ''}`}>
  <div className="flow-node-title"><span className="flow-node-type">{data.metadata.type}</span><strong>{data.label}</strong>{data.onDelete && <button className="flow-node-delete" type="button" aria-label={`删除${data.label}`} onMouseDown={event => event.stopPropagation()} onClick={event => {event.stopPropagation(); data.onDelete?.();}}><DeleteOutlined/></button>}</div>
  <div className="flow-node-desc">{data.metadata.description}</div>
  <div className="flow-node-ports">
    <div>{data.metadata.inputPorts.map((port) => <div className="flow-port-label input" key={handleId(port)}>
      <Handle id={handleId(port)} type="target" position={Position.Left} style={{top: '50%', background: portColor[port.kind], transform: 'translateY(-50%)'}}/>{port.name}
    </div>)}</div>
    <div>{data.metadata.outputPorts.map((port) => <div className="flow-port-label output" key={handleId(port)}>
      {port.name}<Handle id={handleId(port)} type="source" position={Position.Right} style={{top: '50%', background: portColor[port.kind], transform: 'translateY(-50%)'}}/>
    </div>)}</div>
  </div>
</div>;

const nodeTypes = {studio: StudioNodeView};
const edgeKind = (handle?: string | null): PortKind | null => {
  const kind = handle?.split(':')[0] as PortKind | undefined;
  return kind && ['CONTROL', 'DATA', 'WORKER'].includes(kind) ? kind : null;
};
const portKey = (handle?: string | null) => handle?.substring((handle?.indexOf(':') || 0) + 1) || '';
const valueTypesCompatible = (source: PortDefinition, target: PortDefinition) => source.valueType === 'ANY'
  || target.valueType === 'ANY' || source.valueType === target.valueType
  || (source.valueType === 'STRING' && target.valueType === 'MESSAGE')
  || (source.valueType === 'MESSAGE' && target.valueType === 'STRING');

const FlowStudioCanvas = ({fullscreen = false}: {fullscreen?: boolean}) => {
  const {message} = App.useApp();
  const navigate = useNavigate();
  const params = useParams();
  const flowId = Number(params.flowId);
  const reactFlow = useReactFlow<StudioNode, StudioEdge>();
  const [nodes, setNodes, onNodesChange] = useNodesState<StudioNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<StudioEdge>([]);
  const [metadata, setMetadata] = useState<NodeMetadata[]>([]);
  const [agents, setAgents] = useState<AgentOption[]>([]);
  const [flowName, setFlowName] = useState('多 Agent 编排');
  const [selectedId, setSelectedId] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [issues, setIssues] = useState<ValidationIssue[]>([]);
  const [events, setEvents] = useState<string[]>([]);
  const [debugInput, setDebugInput] = useState('请根据输入完成任务');
  const [dirty, setDirty] = useState(false);
  const [inspectorOpen, setInspectorOpen] = useState(true);
  const initialLoad = useRef(true);
  const nodesRef = useRef<StudioNode[]>([]);

  useEffect(() => { nodesRef.current = nodes; }, [nodes]);

  const removeNode = useCallback((nodeId: string) => {
    setNodes(current => current.filter(node => node.id !== nodeId));
    setEdges(current => current.filter(edge => edge.source !== nodeId && edge.target !== nodeId));
    setSelectedId(current => current === nodeId ? undefined : current);
  }, [setEdges, setNodes]);

  const confirmDeleteNodes = useCallback((nodeIds: string[]) => new Promise<boolean>(resolve => {
    const targetNodes = nodesRef.current.filter(node => nodeIds.includes(node.id));
    if (targetNodes.length === 0) {
      resolve(false);
      return;
    }
    const names = targetNodes.map(node => node.data.label).join('、');
    Modal.confirm({
      title: `确认删除${targetNodes.length > 1 ? '这些节点' : '节点'}？`,
      content: `删除“${names}”后，关联连线也会一并移除。`,
      okText: '删除',
      cancelText: '取消',
      okButtonProps: {danger: true},
      onOk: () => {
        targetNodes.forEach(node => removeNode(node.id));
        resolve(true);
      },
      onCancel: () => resolve(false),
    });
  }), [removeNode]);

  const toDefinition = useCallback((): FlowDefinition => ({
    schemaVersion: '1.0', flowId, name: flowName,
    nodes: nodes.map((node): FlowNodeDefinition => ({
      id: node.id, type: node.data.metadata.type, version: node.data.metadata.version, name: node.data.label,
      position: node.position, config: node.data.config,
      inputPorts: node.data.metadata.inputPorts, outputPorts: node.data.metadata.outputPorts,
    })),
    edges: edges.map((edge): FlowEdgeDefinition => ({
      id: edge.id, kind: edge.data?.kind || 'CONTROL',
      source: {nodeId: edge.source, portKey: portKey(edge.sourceHandle)},
      target: {nodeId: edge.target, portKey: portKey(edge.targetHandle)}, config: {},
    })),
  }), [edges, flowId, flowName, nodes]);

  useEffect(() => {
    const load = async () => {
      const [types, detail, agentResponse] = await Promise.all([flowApi.nodeTypes(), flowApi.detail(flowId), eaAgentApi.listAgent({})]);
      setMetadata(types); setFlowName(detail.flowName);
      const agentData = Array.isArray(agentResponse) ? agentResponse : (agentResponse as unknown as {data?: AgentOption[]}).data;
      setAgents(agentData || []);
      let definition = detail.draft || {schemaVersion: '1.0', nodes: [], edges: []};
      if (fullscreen) {
        const fullscreenDraft = localStorage.getItem(fullscreenDraftKey(flowId));
        localStorage.removeItem(fullscreenDraftKey(flowId));
        if (fullscreenDraft) {
          try { definition = JSON.parse(fullscreenDraft) as FlowDefinition; }
          catch { message.warning('未能读取原页面的临时画布，已加载已保存草稿'); }
        }
      }
      const typeMap = new Map(types.map(item => [`${item.type}@${item.version}`, item]));
      setNodes(definition.nodes.map(node => {
        const meta = typeMap.get(`${node.type}@${node.version}`) || types.find(item => item.type === node.type);
        if (!meta) throw new Error(`未知节点类型 ${node.type}`);
        const savedMeta = {
          ...meta,
          inputPorts: node.inputPorts?.length ? node.inputPorts : meta.inputPorts,
          outputPorts: node.outputPorts?.length ? node.outputPorts : meta.outputPorts,
        };
        return {id: node.id, type: 'studio', position: node.position,
          data: {label: node.name, metadata: withDynamicPorts(savedMeta, node.config), config: node.config,
            onDelete: () => { void confirmDeleteNodes([node.id]); }}};
      }));
      setEdges(definition.edges.map(edge => ({
        id: edge.id, source: edge.source.nodeId, target: edge.target.nodeId,
        sourceHandle: `${edge.kind}:${edge.source.portKey}`, targetHandle: `${edge.kind}:${edge.target.portKey}`,
        data: {kind: edge.kind}, animated: edge.kind === 'CONTROL', style: {stroke: portColor[edge.kind]},
      })));
      setLoading(false); initialLoad.current = false;
    };
    void load().catch(error => {message.error(error.message); setLoading(false);});
  }, [confirmDeleteNodes, flowId, fullscreen, message, removeNode, setEdges, setNodes]);

  useEffect(() => { if (!initialLoad.current) setDirty(true); }, [nodes, edges]);
  const selected = nodes.find(node => node.id === selectedId);

  const onConnect = useCallback((connection: Connection) => {
    const sourceKind = edgeKind(connection.sourceHandle); const targetKind = edgeKind(connection.targetHandle);
    if (!sourceKind || sourceKind !== targetKind) { message.error('只能连接相同类型的端口'); return; }
    const sourceNode = nodes.find(node => node.id === connection.source);
    const targetNode = nodes.find(node => node.id === connection.target);
    const sourcePort = sourceNode?.data.metadata.outputPorts.find(port => handleId(port) === connection.sourceHandle);
    const targetPort = targetNode?.data.metadata.inputPorts.find(port => handleId(port) === connection.targetHandle);
    if (!sourcePort || !targetPort) { message.error('端口不存在，请刷新节点配置'); return; }
    if (sourceKind === 'DATA' && !valueTypesCompatible(sourcePort, targetPort)) {
      message.error(`数据类型不兼容：${sourcePort.valueType} → ${targetPort.valueType}`); return;
    }
    if (!targetPort.multiple && edges.some(edge => edge.target === connection.target && edge.targetHandle === connection.targetHandle)) {
      message.error('该输入端口只允许一条连线'); return;
    }
    setEdges(current => addEdge({...connection, id: crypto.randomUUID(), type: 'smoothstep', data: {kind: sourceKind}, animated: sourceKind === 'CONTROL', style: {stroke: portColor[sourceKind]}}, current));
  }, [edges, message, nodes, setEdges]);

  const onDrop = useCallback((event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    const type = event.dataTransfer.getData('application/easy-agent-node');
    const meta = metadata.find(item => item.type === type); if (!meta) return;
    const position = reactFlow.screenToFlowPosition({x: event.clientX, y: event.clientY});
    const defaults = Object.fromEntries(Object.entries(meta.configSchema.properties || {}).filter(([, schema]) => schema.default !== undefined).map(([key, schema]) => [key, schema.default]));
    const nodeId = `${type.toLowerCase()}_${crypto.randomUUID().slice(0, 8)}`;
    setNodes(current => [...current, {id: nodeId, type: 'studio', position,
      data: {label: meta.displayName, metadata: withDynamicPorts(meta, defaults), config: defaults,
        onDelete: () => { void confirmDeleteNodes([nodeId]); }}}]);
  }, [confirmDeleteNodes, metadata, reactFlow, setNodes]);

  const save = async () => { await flowApi.saveDraft(flowId, toDefinition()); setDirty(false); message.success('草稿已保存'); };
  const validate = async () => {
    const result = await flowApi.validate(flowId, toDefinition());
    setIssues(result.issues);
    if (result.valid) message.success('图校验通过');
    else message.error(`发现 ${result.issues.filter(i => i.severity === 'ERROR').length} 个错误`);
  };
  const publish = async () => { await save(); await validate(); const result = await flowApi.validate(flowId, toDefinition()); if (!result.valid) return; await flowApi.publish(flowId); message.success('发布成功'); };

  const debug = async () => {
    await save(); setEvents([]); setNodes(current => current.map(node => ({...node, data: {...node.data, runStatus: undefined}})));
    const token = localStorage.getItem('AUTH_TOKEN');
    await fetchEventSource(`http://localhost:8080/eaAgent/flow/${flowId}/debug-run`, {
      method: 'POST', headers: {'Content-Type': 'application/json', ...(token ? {Authorization: `Bearer ${token}`} : {})},
      body: JSON.stringify({definition: toDefinition(), input: {input: debugInput, question: debugInput}, sessionId: `debug-${Date.now()}`}), openWhenHidden: true,
      onmessage: event => {
        const data = JSON.parse(event.data) as {eventType?: string; nodeId?: string; nodeName?: string; content?: string};
        setEvents(current => [...current, `${data.eventType || 'event'} · ${data.nodeName || data.nodeId || 'Flow'} · ${data.content || ''}`]);
        if (data.nodeId) setNodes(current => current.map(node => node.id === data.nodeId ? {...node, data: {...node.data, runStatus: data.eventType === 'node_failed' ? 'failed' : data.eventType === 'node_succeeded' ? 'succeeded' : 'running'}} : node));
      }, onerror: error => { throw error; },
    });
  };

  const categories = useMemo(() => metadata.reduce((groups, item) => {
    const items = groups.get(item.category) || [];
    items.push(item); groups.set(item.category, items); return groups;
  }, new Map<string, NodeMetadata[]>()), [metadata]);
  const openFullscreen = () => {
    localStorage.setItem(fullscreenDraftKey(flowId), JSON.stringify(toDefinition()));
    window.open(`${window.location.origin}/page/FlowStudio/${flowId}/fullscreen`, '_blank', 'noopener,noreferrer');
  };
  const closeFullscreen = () => {
    window.close();
    window.setTimeout(() => { if (!window.closed) navigate(`/page/FlowStudio/${flowId}`); }, 120);
  };
  return <Spin spinning={loading}><div className={`flow-studio ${inspectorOpen ? '' : 'inspector-collapsed'} ${fullscreen ? 'flow-studio-fullscreen' : ''}`}>
    <header className="flow-toolbar"><Space><Button icon={<ArrowLeftOutlined/>} onClick={() => fullscreen ? closeFullscreen() : navigate('/page/FlowManager')}/><Typography.Title level={4} style={{margin: 0}}>{flowName}</Typography.Title>{dirty && <Tag color="orange">未保存</Tag>}</Space>
      <Space>{fullscreen ? <Button icon={<FullscreenExitOutlined/>} aria-label="退出全屏" title="退出全屏" onClick={closeFullscreen}/> : <Button icon={<FullscreenOutlined/>} aria-label="全屏画布" title="全屏画布" onClick={openFullscreen}/>}<Button icon={<SaveOutlined/>} onClick={() => void save()}>保存</Button><Button icon={<CheckCircleOutlined/>} onClick={() => void validate()}>校验</Button><Button icon={<BugOutlined/>} onClick={() => void debug()}>试运行</Button><Button type="primary" icon={<RocketOutlined/>} onClick={() => void publish()}>发布</Button></Space>
    </header>
    <aside className="flow-palette"><Typography.Text strong>节点库</Typography.Text>{Array.from(categories.entries()).map(([category, items]) => <div key={category}><Divider titlePlacement="start" plain>{category}</Divider>{items.map(item => <div className="flow-palette-item" draggable key={item.type} onDragStart={event => {event.dataTransfer.setData('application/easy-agent-node', item.type); event.dataTransfer.effectAllowed = 'move';}}><strong>{item.displayName}</strong><small>{item.description}</small></div>)}</div>)}</aside>
    <main className="flow-canvas" onDrop={onDrop} onDragOver={event => {event.preventDefault(); event.dataTransfer.dropEffect = 'move';}}>
      <ReactFlow nodes={nodes} edges={edges} nodeTypes={nodeTypes} onNodesChange={onNodesChange} onBeforeDelete={async ({nodes: deletedNodes}) => {
        if (deletedNodes.length === 0) return true;
        await confirmDeleteNodes(deletedNodes.map(node => node.id));
        return false;
      }} onNodesDelete={deletedNodes => setSelectedId(current => deletedNodes.some(node => node.id === current) ? undefined : current)} onEdgesChange={onEdgesChange} onConnect={onConnect} onNodeClick={(_, node) => {setSelectedId(node.id); setInspectorOpen(true);}} onPaneClick={() => setSelectedId(undefined)} fitView deleteKeyCode={['Backspace', 'Delete']}><Background/><MiniMap/><Controls/></ReactFlow>
    </main>
    {inspectorOpen && <aside className="flow-inspector"><div className="flow-inspector-header"><Typography.Title level={5} style={{margin: 0}}>{selected ? selected.data.label : '节点配置'}</Typography.Title><Button type="text" aria-label="关闭节点配置" icon={<CloseOutlined/>} onClick={() => setInspectorOpen(false)}/></div>{selected ? <NodeInspector node={selected} agents={agents} onChange={patch => setNodes(current => current.map(node => node.id === selected.id ? {...node, data: {...node.data, ...patch}} : node))}/> : <Typography.Text type="secondary">选择画布中的节点后配置参数</Typography.Text>}
      <Divider/><Typography.Text strong>校验问题</Typography.Text><div className="flow-issues">{issues.length === 0 ? <Typography.Text type="secondary">暂无</Typography.Text> : issues.map((issue, index) => <div key={`${issue.code}-${index}`} className={issue.severity === 'ERROR' ? 'issue-error' : 'issue-warning'} onClick={() => issue.targetId && setSelectedId(issue.targetId)}>{issue.message}</div>)}</div>
    </aside>}
    <section className="flow-debug"><div className="flow-debug-input"><Typography.Text strong>试运行输入</Typography.Text><Input value={debugInput} onChange={event => setDebugInput(event.target.value)} placeholder="输入本次调试问题"/></div>{events.slice(-8).map((event, index) => <div key={index}>{event}</div>)}</section>
  </div></Spin>;
};

const NodeInspector = ({node, agents, onChange}: {node: StudioNode; agents: AgentOption[]; onChange: (patch: Partial<StudioNodeData>) => void}) => {
  const properties = node.data.metadata.configSchema.properties || {};
  const updateConfig = (key: string, value: unknown) => onChange({config: {...node.data.config, [key]: value}});
  const updateBranches = (branches: RouterBranch[]) => {
    const config = {...node.data.config, branches};
    onChange({config, metadata: withDynamicPorts(node.data.metadata, config)});
  };
  const branches = routerBranches(node.data.config);
  return <Form layout="vertical" size="small">
    <Form.Item label="节点名称"><Input value={node.data.label} onChange={event => onChange({label: event.target.value})}/></Form.Item>
    {Object.entries(properties).map(([key, schema]) => <Form.Item key={key} label={schema.title || key} tooltip={schema.description} required={node.data.metadata.configSchema.required?.includes(key)}>
      {key === 'branches' && node.data.metadata.type === 'ROUTER' ? <div className="router-branches">{branches.map((branch, index) => <div className="router-branch" key={`${branch.key}-${index}`}>
          <Input value={branch.key} placeholder="key" onChange={event => updateBranches(branches.map((item, i) => i === index ? {...item, key: event.target.value.replace(/[^a-zA-Z0-9_-]/g, '')} : item))}/>
          <Input value={branch.label} placeholder="分支名称" onChange={event => updateBranches(branches.map((item, i) => i === index ? {...item, label: event.target.value} : item))}/>
          <Input value={branch.description} placeholder="匹配说明" onChange={event => updateBranches(branches.map((item, i) => i === index ? {...item, description: event.target.value} : item))}/>
          <Button type="text" danger icon={<MinusCircleOutlined/>} onClick={() => updateBranches(branches.filter((_, i) => i !== index))}/>
        </div>)}<Button block type="dashed" icon={<PlusOutlined/>} onClick={() => updateBranches([...branches, {key: `branch_${branches.length + 1}`, label: `分支 ${branches.length + 1}`, description: ''}])}>添加分支</Button></div>
        : key === 'agentId' ? <Select value={node.data.config[key] as number | undefined} onChange={value => updateConfig(key, value)} options={agents.map(agent => ({value: agent.id, label: agent.agentName}))}/>
        : key === 'fallback' && node.data.metadata.type === 'ROUTER' ? <Select allowClear value={node.data.config[key] as string | undefined} onChange={value => updateConfig(key, value)} options={branches.map(branch => ({value: branch.key, label: `${branch.label} (${branch.key})`}))}/>
        : schema.enum ? <Select value={node.data.config[key]} onChange={value => updateConfig(key, value)} options={schema.enum.map(value => ({value, label: String(value)}))}/>
          : schema.type === 'integer' ? <InputNumber style={{width: '100%'}} value={node.data.config[key] as number | undefined} onChange={value => updateConfig(key, value)}/>
            : <Input.TextArea value={String(node.data.config[key] ?? '')} onChange={event => updateConfig(key, event.target.value)} autoSize={{minRows: 2, maxRows: 7}}/>}
    </Form.Item>)}
    <Divider titlePlacement="start" plain>端口</Divider>{[...node.data.metadata.inputPorts, ...node.data.metadata.outputPorts].map(port => <div className="inspector-port" key={`${port.kind}-${port.key}`}><span style={{background: portColor[port.kind]}}/>{port.name}<code>{port.valueType}</code></div>)}
  </Form>;
};

export default function FlowStudioPage({fullscreen = false}: {fullscreen?: boolean}) { return <ReactFlowProvider><FlowStudioCanvas fullscreen={fullscreen}/></ReactFlowProvider>; }
