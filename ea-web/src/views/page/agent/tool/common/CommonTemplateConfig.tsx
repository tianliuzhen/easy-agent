import React, { useState } from 'react';
import { App, Card, Form, Input, Button, Space, Divider, Row, Col, Table, Collapse, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons';

const { Panel } = Collapse;

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

const CommonTemplateConfig: React.FC<CommonTemplateConfigProps> = ({
  inputParams = [],
  outputParams = [],
  onInputParamsChange,
  onOutputParamsChange
}) => {
  const app = App.useApp();
  const [editingInputIndex, setEditingInputIndex] = useState<number | null>(null);
  const [editingOutputIndex, setEditingOutputIndex] = useState<number | null>(null);
  
  // 添加输入参数
  const addInputParam = () => {
    const newParam = {
      name: '',
      type: '',
      description: '',
      required: false,
      defaultValue: '',
      referenceValue: '', // 新增引用值字段
    };
    const newIndex = inputParams.length;
    onInputParamsChange?.([...inputParams, newParam]);
    setEditingInputIndex(newIndex);
  };

  // 添加输出参数
  const addOutputParam = () => {
    const newParam = {
      name: '',
      type: '',
      description: '',
      required: false,
      defaultValue: '',
      referenceValue: '', // 新增引用值字段
    };
    const newIndex = outputParams.length;
    onOutputParamsChange?.([...outputParams, newParam]);
    setEditingOutputIndex(newIndex);
  };

  // 开始编辑输入参数
  const startEditingInputParam = (index: number) => {
    setEditingInputIndex(index);
  };

  // 开始编辑输出参数
  const startEditingOutputParam = (index: number) => {
    setEditingOutputIndex(index);
  };

  // 保存编辑的输入参数
  const saveEditedInputParam = (index: number) => {
    const param = inputParams[index];
    if (!param.name || !param.type) {
      message.error('参数名和类型不能为空');
      return;
    }
    setEditingInputIndex(null);
  };

  // 保存编辑的输出参数
  const saveEditedOutputParam = (index: number) => {
    const param = outputParams[index];
    if (!param.name || !param.type) {
      message.error('参数名和类型不能为空');
      return;
    }
    setEditingOutputIndex(null);
  };

  // 取消编辑
  const cancelEditing = () => {
    setEditingInputIndex(null);
    setEditingOutputIndex(null);
  };

  // 复制输入参数
  const copyInputParam = (index: number) => {
    const paramToCopy = inputParams[index];
    const newParam = {
      ...paramToCopy,
      name: paramToCopy.name + '_copy', // 为复制的参数名添加后缀
    };
    onInputParamsChange?.([...inputParams, newParam]);
    message.success('输入参数复制成功');
  };

  // 复制输出参数
  const copyOutputParam = (index: number) => {
    const paramToCopy = outputParams[index];
    const newParam = {
      ...paramToCopy,
      name: paramToCopy.name + '_copy', // 为复制的参数名添加后缀
    };
    onOutputParamsChange?.([...outputParams, newParam]);
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
        const updatedParams = inputParams.filter((_, i) => i !== index);
        onInputParamsChange?.(updatedParams);
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
        const updatedParams = outputParams.filter((_, i) => i !== index);
        onOutputParamsChange?.(updatedParams);
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

  // 输入参数列定义
  const inputColumns = [
    {
      title: '参数名',
      dataIndex: 'name',
      key: 'name',
      render: (text, record, index) => {
        if (editingInputIndex === index) {
          return (
            <Input
              value={inputParams[index].name}
              onChange={(e) => updateInputParamValue(index, 'name', e.target.value)}
              placeholder="输入参数名"
              autoFocus
            />
          );
        }
        return text;
      },
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (text, record, index) => {
        if (editingInputIndex === index) {
          return (
            <select
              value={inputParams[index].type}
              onChange={(e) => updateInputParamValue(index, 'type', e.target.value)}
              style={{ width: '100%', height: 24, borderRadius: 4, border: '1px solid #d9d9d9' }}
            >
              <option value="">请选择类型</option>
              <option value="string">string</option>
              <option value="number">number</option>
              <option value="boolean">boolean</option>
              <option value="object">object</option>
              <option value="array">array</option>
              <option value="int">int</option>
              <option value="float">float</option>
              <option value="date">date</option>
              <option value="datetime">datetime</option>
              <option value="bigint">bigint</option>
              <option value="text">text</option>
            </select>
          );
        }
        return text;
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (text, record, index) => {
        if (editingInputIndex === index) {
          return (
            <Input
              value={inputParams[index].description}
              onChange={(e) => updateInputParamValue(index, 'description', e.target.value)}
              placeholder="输入描述"
            />
          );
        }
        return text;
      },
    },
    {
      title: '必需',
      dataIndex: 'required',
      key: 'required',
      render: (required, record, index) => {
        if (editingInputIndex === index) {
          return (
            <select
              value={inputParams[index].required ? 'true' : 'false'}
              onChange={(e) => updateInputParamValue(index, 'required', e.target.value === 'true')}
              style={{ width: '100%', height: 24, borderRadius: 4, border: '1px solid #d9d9d9' }}
            >
              <option value="true">是</option>
              <option value="false">否</option>
            </select>
          );
        }
        return required ? '是' : '否';
      },
    },
    {
      title: '默认值',
      dataIndex: 'defaultValue',
      key: 'defaultValue',
      render: (text, record, index) => {
        if (editingInputIndex === index) {
          return (
            <Input
              value={inputParams[index].defaultValue || ''}
              onChange={(e) => updateInputParamValue(index, 'defaultValue', e.target.value)}
              placeholder="输入默认值"
            />
          );
        }
        return text;
      },
    },
    {
      title: '引用值',
      dataIndex: 'referenceValue',
      key: 'referenceValue',
      render: (text, record, index) => {
        if (editingInputIndex === index) {
          return (
            <Input
              value={inputParams[index].referenceValue || ''}
              onChange={(e) => updateInputParamValue(index, 'referenceValue', e.target.value)}
              placeholder="输入引用值"
            />
          );
        }
        return text;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record, index) => (
        <Space>
          {editingInputIndex === index ? (
            <>
              <Button type="link" size="small" onClick={() => saveEditedInputParam(index)}>保存</Button>
              <Button type="link" danger size="small" onClick={cancelEditing}>取消</Button>
            </>
          ) : (
            <>
              <Button type="link" size="small" onClick={() => startEditingInputParam(index)} icon={<EditOutlined />}>编辑</Button>
              <Button type="link" size="small" onClick={() => copyInputParam(index)} icon={<CopyOutlined />}>复制</Button>
              <Button type="link" danger size="small" onClick={() => deleteInputParam(index)} icon={<DeleteOutlined />}>删除</Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  // 输出参数列定义
  const outputColumns = [
    {
      title: '参数名',
      dataIndex: 'name',
      key: 'name',
      render: (text, record, index) => {
        if (editingOutputIndex === index) {
          return (
            <Input
              value={outputParams[index].name}
              onChange={(e) => updateOutputParamValue(index, 'name', e.target.value)}
              placeholder="输入参数名"
              autoFocus
            />
          );
        }
        return text;
      },
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (text, record, index) => {
        if (editingOutputIndex === index) {
          return (
            <select
              value={outputParams[index].type}
              onChange={(e) => updateOutputParamValue(index, 'type', e.target.value)}
              style={{ width: '100%', height: 24, borderRadius: 4, border: '1px solid #d9d9d9' }}
            >
              <option value="">请选择类型</option>
              <option value="string">string</option>
              <option value="number">number</option>
              <option value="boolean">boolean</option>
              <option value="object">object</option>
              <option value="array">array</option>
              <option value="int">int</option>
              <option value="float">float</option>
              <option value="date">date</option>
              <option value="datetime">datetime</option>
              <option value="bigint">bigint</option>
              <option value="text">text</option>
            </select>
          );
        }
        return text;
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (text, record, index) => {
        if (editingOutputIndex === index) {
          return (
            <Input
              value={outputParams[index].description}
              onChange={(e) => updateOutputParamValue(index, 'description', e.target.value)}
              placeholder="输入描述"
            />
          );
        }
        return text;
      },
    },
    {
      title: '必需',
      dataIndex: 'required',
      key: 'required',
      render: (required, record, index) => {
        if (editingOutputIndex === index) {
          return (
            <select
              value={outputParams[index].required ? 'true' : 'false'}
              onChange={(e) => updateOutputParamValue(index, 'required', e.target.value === 'true')}
              style={{ width: '100%', height: 24, borderRadius: 4, border: '1px solid #d9d9d9' }}
            >
              <option value="true">是</option>
              <option value="false">否</option>
            </select>
          );
        }
        return required ? '是' : '否';
      },
    },
    {
      title: '默认值',
      dataIndex: 'defaultValue',
      key: 'defaultValue',
      render: (text, record, index) => {
        if (editingOutputIndex === index) {
          return (
            <Input
              value={outputParams[index].defaultValue || ''}
              onChange={(e) => updateOutputParamValue(index, 'defaultValue', e.target.value)}
              placeholder="输入默认值"
            />
          );
        }
        return text;
      },
    },
    {
      title: '引用值',
      dataIndex: 'referenceValue',
      key: 'referenceValue',
      render: (text, record, index) => {
        if (editingOutputIndex === index) {
          return (
            <Input
              value={outputParams[index].referenceValue || ''}
              onChange={(e) => updateOutputParamValue(index, 'referenceValue', e.target.value)}
              placeholder="输入引用值"
            />
          );
        }
        return text;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record, index) => (
        <Space>
          {editingOutputIndex === index ? (
            <>
              <Button type="link" size="small" onClick={() => saveEditedOutputParam(index)}>保存</Button>
              <Button type="link" danger size="small" onClick={cancelEditing}>取消</Button>
            </>
          ) : (
            <>
              <Button type="link" size="small" onClick={() => startEditingOutputParam(index)} icon={<EditOutlined />}>编辑</Button>
              <Button type="link" size="small" onClick={() => copyOutputParam(index)} icon={<CopyOutlined />}>复制</Button>
              <Button type="link" danger size="small" onClick={() => deleteOutputParam(index)} icon={<DeleteOutlined />}>删除</Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Collapse defaultActiveKey={['input', 'output']}>
      <Panel header="通用模板入参配置" key="input">
        <Table
          dataSource={inputParams}
          columns={inputColumns}
          rowKey={(record, index) => `input-${index}`}
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
          rowKey={(record, index) => `output-${index}`}
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