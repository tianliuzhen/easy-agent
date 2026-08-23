import {useEffect, useState} from 'react';
import {App, Button, Card, Form, Input, List, Modal, Space, Tag} from 'antd';
import {ArrowRightOutlined, DeleteOutlined, EditOutlined, PlusOutlined, RobotOutlined, ThunderboltOutlined} from '@ant-design/icons';
import {useNavigate} from 'react-router-dom';
import {flowApi, type FlowDefinition, type FlowSummary, type PortDefinition, type PortKind, type ValueType} from '../api/FlowApi';
import {eaAgentApi} from '../api/EaAgentApi';

const port = (key: string, name: string, kind: PortKind, valueType: ValueType, required = false, multiple = false): PortDefinition => ({
  key, name, kind, valueType, required, multiple,
});

const createDefaultExample = (agentIds: number[]): FlowDefinition => {
  const firstAgent = agentIds[0];
  const secondAgent = agentIds[1] || firstAgent;
  const analystConfig = firstAgent ? {agentId: firstAgent} : {};
  const writerConfig = secondAgent ? {agentId: secondAgent} : {};
  const start = 'start_example';
  const analyst = 'agent_analyst_example';
  const writer = 'agent_writer_example';
  const end = 'end_example';
  const control = (id: string, source: string, sourcePort: string, target: string, targetPort: string) => ({
    id, kind: 'CONTROL' as const, source: {nodeId: source, portKey: sourcePort}, target: {nodeId: target, portKey: targetPort}, config: {},
  });
  const data = (id: string, source: string, sourcePort: string, target: string, targetPort: string) => ({
    id, kind: 'DATA' as const, source: {nodeId: source, portKey: sourcePort}, target: {nodeId: target, portKey: targetPort}, config: {},
  });
  return {
    schemaVersion: '1.0', name: '客服双 Agent 示例',
    nodes: [
      {id: start, type: 'START', version: '1', name: '开始', position: {x: 80, y: 180}, config: {},
        inputPorts: [], outputPorts: [port('control', '开始', 'CONTROL', 'ANY', false, true), port('input', '用户输入', 'DATA', 'ANY', false, true)]},
      {id: analyst, type: 'AGENT', version: '1', name: '问题分析 Agent', position: {x: 340, y: 180}, config: analystConfig,
        inputPorts: [port('control', '执行', 'CONTROL'), port('worker', 'Worker 调用', 'WORKER', 'ANY', false, true), port('input', '输入', 'DATA', 'ANY', false, true)],
        outputPorts: [port('control', '完成', 'CONTROL', 'ANY', false, true), port('result', '输出', 'DATA', 'STRING', false, true)]},
      {id: writer, type: 'AGENT', version: '1', name: '回答生成 Agent', position: {x: 620, y: 180}, config: writerConfig,
        inputPorts: [port('control', '执行', 'CONTROL'), port('worker', 'Worker 调用', 'WORKER', 'ANY', false, true), port('input', '输入', 'DATA', 'ANY', false, true)],
        outputPorts: [port('control', '完成', 'CONTROL', 'ANY', false, true), port('result', '输出', 'DATA', 'STRING', false, true)]},
      {id: end, type: 'END', version: '1', name: '结束', position: {x: 900, y: 180}, config: {},
        inputPorts: [port('control', '执行', 'CONTROL', 'ANY', true, true), port('result', '最终结果', 'DATA', 'ANY', true)], outputPorts: [port('result', '最终结果', 'DATA', 'ANY', false, true)]},
    ],
    edges: [
      control('control_start_analyst', start, 'control', analyst, 'control'),
      data('data_start_analyst', start, 'input', analyst, 'input'),
      control('control_analyst_writer', analyst, 'control', writer, 'control'),
      data('data_analyst_writer', analyst, 'result', writer, 'input'),
      control('control_writer_end', writer, 'control', end, 'control'),
      data('data_writer_end', writer, 'result', end, 'result'),
    ],
  };
};

const FlowManager = () => {
  const {message} = App.useApp();
  const navigate = useNavigate();
  const [flows, setFlows] = useState<FlowSummary[]>([]);
  const [open, setOpen] = useState(false);
  const [exampleLoading, setExampleLoading] = useState(false);
  const [form] = Form.useForm();
  const load = async () => setFlows(await flowApi.list());
  useEffect(() => { void load(); }, []);

  const create = async () => {
    const values = await form.validateFields();
    const id = await flowApi.create(values);
    setOpen(false); form.resetFields(); navigate(`/page/FlowStudio/${id}`);
  };
  const remove = async (flow: FlowSummary) => { await flowApi.remove(flow.id); message.success('已删除'); await load(); };
  const useExample = async () => {
    setExampleLoading(true);
    try {
      const response = await eaAgentApi.listAgent({});
      const agents = Array.isArray(response) ? response : (response as unknown as {data?: Array<{id: number}>}).data || [];
      const id = await flowApi.create({flowName: '客服双 Agent 示例', flowKey: `customer-service-${Date.now()}`, flowDesc: '开始 → 问题分析 Agent → 回答生成 Agent → 结束', avatar: '🤖'});
      await flowApi.saveDraft(id, createDefaultExample(agents.map(agent => agent.id)));
      message.success('示例编排已创建');
      navigate(`/page/FlowStudio/${id}`);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建示例失败');
    } finally {
      setExampleLoading(false);
    }
  };

  return <App>
    <div style={{display: 'flex', justifyContent: 'flex-end', marginBottom: 16}}><Button type="primary" icon={<PlusOutlined/>} onClick={() => setOpen(true)}>新建编排</Button></div>
    {flows.length === 0 && <Card style={{marginBottom: 18, border: '1px solid #d6e4ff', background: 'linear-gradient(135deg, #f0f7ff, #fff)'}}>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 28, flexWrap: 'wrap'}}>
        <div style={{minWidth: 300, flex: 1}}><Space align="start"><RobotOutlined style={{fontSize: 28, color: '#1677ff', marginTop: 4}}/><div><div style={{fontSize: 18, fontWeight: 600}}>从一个默认示例开始</div><div style={{color: '#667085', marginTop: 6}}>示例会创建两个可独立配置的 Agent 节点，帮助你快速理解拖拽、连线和数据传递。</div></div></Space></div>
        <div style={{display: 'flex', alignItems: 'center', gap: 10, color: '#344054', padding: '12px 16px', background: '#fff', borderRadius: 10, border: '1px solid #e4e7ec'}}><span>开始</span><ArrowRightOutlined/><span><ThunderboltOutlined/> 分析</span><ArrowRightOutlined/><span><ThunderboltOutlined/> 生成</span><ArrowRightOutlined/><span>结束</span></div>
        <Button type="primary" loading={exampleLoading} onClick={() => void useExample()}>使用此示例</Button>
      </div>
    </Card>}
    <List grid={{gutter: 16, xs: 1, sm: 2, lg: 3, xl: 4}} dataSource={flows} renderItem={(flow) =>
      <List.Item><Card title={<Space><span>{flow.avatar || '🧩'}</span>{flow.flowName}</Space>} actions={[
        <EditOutlined key="edit" onClick={() => navigate(`/page/FlowStudio/${flow.id}`)}/>,
        <DeleteOutlined key="delete" onClick={() => void remove(flow)}/>,
      ]}><Space direction="vertical"><span>{flow.flowDesc || '可视化多 Agent 编排'}</span>
        {flow.publishedRevisionId ? <Tag color="green">已发布 R{flow.publishedRevisionNo || ''}</Tag> : <Tag>草稿</Tag>}
      </Space></Card></List.Item>}/>
    <Modal title="新建多 Agent 编排" open={open} onCancel={() => setOpen(false)} onOk={() => void create()}>
      <Form form={form} layout="vertical">
        <Form.Item name="flowName" label="名称" rules={[{required: true}]}><Input/></Form.Item>
        <Form.Item name="flowKey" label="唯一 Key" rules={[{required: true}]}><Input/></Form.Item>
        <Form.Item name="flowDesc" label="说明"><Input.TextArea/></Form.Item>
        <Form.Item name="avatar" label="头像" initialValue="🧩"><Input/></Form.Item>
      </Form>
    </Modal>
  </App>;
};

export default FlowManager;
