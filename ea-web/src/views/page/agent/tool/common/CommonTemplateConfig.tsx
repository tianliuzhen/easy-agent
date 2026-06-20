import React from 'react';
import { App, Input, Select, Button, Space, Table, Collapse, message, Tooltip } from 'antd';
import { PlusOutlined, DeleteOutlined, CopyOutlined, QuestionCircleOutlined } from '@ant-design/icons';

const { Panel } = Collapse;

// 引用值（JSONPath）说明与示例：后端据此把入参值动态注入到 HTTP 请求的对应位置
const referenceValueHelp = (
  <div style={{ fontSize: 12, lineHeight: 1.7 }}>
    <div>引用值用 JSONPath 指定该参数注入到请求的哪个位置，后端按前缀动态赋值：</div>
    <div style={{ marginTop: 6 }}>
      <code>$.requestParams.xxx</code> → URL Query 参数
    </div>
    <div>
      <code>$.headers.xxx</code> → 请求头
    </div>
    <div>
      <code>$.requestBody.xxx</code> → JSON Body 字段
    </div>
    <div>
      <code>$.requestBody.items[0].trackingNo</code> → Body 数组元素字段
    </div>
    <div style={{ marginTop: 6, color: '#ffd591' }}>
      示例：参数 <code>accountId</code> 引用值填 <code>$.requestBody.accountId</code>，
      调用时会把 accountId 的值写入请求体的 accountId 字段。
    </div>
  </div>
);

const ReferenceValueTitle = (
  <span>
    引用值
    <Tooltip title={referenceValueHelp} overlayStyle={{ maxWidth: 420 }}>
      <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999', cursor: 'help' }} />
    </Tooltip>
  </span>
);

interface TemplateParam {
  name: string;
  type: string;
  description: string;
  required: boolean;
  defaultValue?: string;
  referenceValue?: string; // 新增引用值字段
}

interface CommonTemplateConfigProps {
  inputParams?: TemplateParam[];
  outputParams?: TemplateParam[];
  onInputParamsChange?: (params: TemplateParam[]) => void;
  onOutputParamsChange?: (params: TemplateParam[]) => void;
}

const typeOptions = ['string', 'number', 'boolean', 'object', 'array', 'int', 'float', 'date', 'datetime', 'bigint', 'text'];

const CommonTemplateConfig: React.FC<CommonTemplateConfigProps> = ({
  inputParams = [],
  outputParams = [],
  onInputParamsChange,
  onOutputParamsChange
}) => {
  const app = App.useApp();

  // 添加输入参数
  const addInputParam = () => {
    onInputParamsChange?.([...inputParams, {
      name: '', type: '', description: '', required: false, defaultValue: '', referenceValue: '',
    }]);
  };

  // 添加输出参数
  const addOutputParam = () => {
    onOutputParamsChange?.([...outputParams, {
      name: '', type: '', description: '', required: false, defaultValue: '', referenceValue: '',
    }]);
  };

  // 复制输入参数
  const copyInputParam = (index: number) => {
    const paramToCopy = inputParams[index];
    onInputParamsChange?.([...inputParams, { ...paramToCopy, name: paramToCopy.name + '_copy' }]);
    message.success('输入参数复制成功');
  };

  // 复制输出参数
  const copyOutputParam = (index: number) => {
    const paramToCopy = outputParams[index];
    onOutputParamsChange?.([...outputParams, { ...paramToCopy, name: paramToCopy.name + '_copy' }]);
    message.success('输出参数复制成功');
  };

  // 删除输入参数（带确认）
  const deleteInputParam = (index: number) => {
    app.modal.confirm({
      title: '确认删除',
      content: '确定要删除这个输入参数吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        onInputParamsChange?.(inputParams.filter((_, i) => i !== index));
        message.success('输入参数删除成功');
      }
    });
  };

  // 删除输出参数（带确认）
  const deleteOutputParam = (index: number) => {
    app.modal.confirm({
      title: '确认删除',
      content: '确定要删除这个输出参数吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        onOutputParamsChange?.(outputParams.filter((_, i) => i !== index));
        message.success('输出参数删除成功');
      }
    });
  };

  // 更新输入参数值
  const updateInputParamValue = (index: number, field: keyof TemplateParam, value: any) => {
    const updatedParams = [...inputParams];
    updatedParams[index] = { ...updatedParams[index], [field]: value };
    onInputParamsChange?.(updatedParams);
  };

  // 更新输出参数值
  const updateOutputParamValue = (index: number, field: keyof TemplateParam, value: any) => {
    const updatedParams = [...outputParams];
    updatedParams[index] = { ...updatedParams[index], [field]: value };
    onOutputParamsChange?.(updatedParams);
  };

  // 通用：构建可直接点击编辑的列（单元格本身即输入控件，类似表格内联编辑）
  const buildColumns = (
    params: TemplateParam[],
    updateValue: (index: number, field: keyof TemplateParam, value: any) => void,
    copyParam: (index: number) => void,
    deleteParam: (index: number) => void,
    referenceTitle: React.ReactNode
  ) => [
    {
      title: '参数名',
      dataIndex: 'name',
      key: 'name',
      width: 130,
      render: (_: any, __: any, index: number) => (
        <Input
          variant="filled"
          value={params[index].name}
          onChange={(e) => updateValue(index, 'name', e.target.value)}
          placeholder="参数名"
        />
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (_: any, __: any, index: number) => (
        <Select
          variant="filled"
          style={{ width: '100%' }}
          value={params[index].type || undefined}
          onChange={(value) => updateValue(index, 'type', value)}
          placeholder="类型"
          options={typeOptions.map((t) => ({ label: t, value: t }))}
        />
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (_: any, __: any, index: number) => (
        <Input
          variant="filled"
          value={params[index].description}
          onChange={(e) => updateValue(index, 'description', e.target.value)}
          placeholder="描述"
        />
      ),
    },
    {
      title: '必需',
      dataIndex: 'required',
      key: 'required',
      width: 90,
      render: (_: any, __: any, index: number) => (
        <Select
          variant="filled"
          style={{ width: '100%' }}
          value={params[index].required ? 'true' : 'false'}
          onChange={(value) => updateValue(index, 'required', value === 'true')}
          options={[
            { label: '是', value: 'true' },
            { label: '否', value: 'false' },
          ]}
        />
      ),
    },
    {
      title: '默认值',
      dataIndex: 'defaultValue',
      key: 'defaultValue',
      render: (_: any, __: any, index: number) => (
        <Input
          variant="filled"
          value={params[index].defaultValue || ''}
          onChange={(e) => updateValue(index, 'defaultValue', e.target.value)}
          placeholder="默认值"
        />
      ),
    },
    {
      title: referenceTitle,
      dataIndex: 'referenceValue',
      key: 'referenceValue',
      render: (_: any, __: any, index: number) => (
        <Input
          variant="filled"
          value={params[index].referenceValue || ''}
          onChange={(e) => updateValue(index, 'referenceValue', e.target.value)}
          placeholder="引用值"
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 90,
      render: (_: any, __: any, index: number) => (
        <Space>
          <Tooltip title="复制">
            <Button type="link" size="small" onClick={() => copyParam(index)} icon={<CopyOutlined />} />
          </Tooltip>
          <Tooltip title="删除">
            <Button type="link" danger size="small" onClick={() => deleteParam(index)} icon={<DeleteOutlined />} />
          </Tooltip>
        </Space>
      ),
    },
  ];

  const inputColumns = buildColumns(inputParams, updateInputParamValue, copyInputParam, deleteInputParam, ReferenceValueTitle);
  const outputColumns = buildColumns(outputParams, updateOutputParamValue, copyOutputParam, deleteOutputParam, '引用值');

  return (
    <Collapse defaultActiveKey={['input', 'output']}>
      <Panel header="通用模板入参配置" key="input">
        <Table
          dataSource={inputParams}
          columns={inputColumns}
          rowKey={(_, index) => `input-${index}`}
          pagination={false}
          size="small"
        />
        <Button type="dashed" onClick={addInputParam} icon={<PlusOutlined />} block style={{ marginTop: 8 }}>
          添加输入参数
        </Button>
      </Panel>

      <Panel header="通用模板出参配置" key="output">
        <Table
          dataSource={outputParams}
          columns={outputColumns}
          rowKey={(_, index) => `output-${index}`}
          pagination={false}
          size="small"
        />
        <Button type="dashed" onClick={addOutputParam} icon={<PlusOutlined />} block style={{ marginTop: 8 }}>
          添加输出参数
        </Button>
      </Panel>
    </Collapse>
  );
};

export default CommonTemplateConfig;
