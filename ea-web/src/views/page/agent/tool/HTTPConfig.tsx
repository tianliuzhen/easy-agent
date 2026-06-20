import React, { useState, useEffect, useRef } from 'react';
import { App, Card, Form, Input, Button, Space, Divider, Collapse, Select, Row, Col, Tabs, Alert, Table, ConfigProvider, Descriptions, Typography, Modal } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ImportOutlined } from '@ant-design/icons';
import { eaToolApi } from '../../../api/EaToolApi';
import CommonTemplateConfig from './common/CommonTemplateConfig';
import DebugResult from './common/DebugResult';
import { parseCurl } from './common/curlParser';

const { Panel } = Collapse;
const { TabPane } = Tabs;

interface HTTPConfigProps {
  toolConfigs?: any[];
  agentId?: string | number;
  onRefresh?: () => void;
}

const HTTPConfig: React.FC<HTTPConfigProps> = ({ toolConfigs = [], agentId, onRefresh }) => {
  const [form] = Form.useForm();
  const app = App.useApp();
  const [activeTab, setActiveTab] = useState('params');
  const [debugResult, setDebugResult] = useState<any>(null);  // 用于存储API原始响应
  const [debugging, setDebugging] = useState<boolean>(false);  // 用于标记调试状态
  const [loading, setLoading] = useState(false);
  const [inputParams, setInputParams] = useState<any[]>([]);
  const [outputParams, setOutputParams] = useState<any[]>([]);
  const [httpConfigData, setHttpConfigData] = useState<any>(null);
  const [paramsList, setParamsList] = useState<{ key: string; value: string; description?: string }[]>([]);
  const [headersList, setHeadersList] = useState<{ key: string; value: string; description?: string }[]>([]);
  const [editingParam, setEditingParam] = useState<{ type: 'param' | 'header'; index: number } | null>(null);
  const [curlModalOpen, setCurlModalOpen] = useState(false);
  const [curlText, setCurlText] = useState('');

  // 解析并应用 cURL 命令
  const applyCurl = () => {
    if (!curlText.trim()) {
      app.message.warning('请粘贴 cURL 命令');
      return;
    }
    try {
      const parsed = parseCurl(curlText);
      if (!parsed.url) {
        app.message.error('未解析到请求 URL，请检查 cURL 命令');
        return;
      }
      form.setFieldsValue({
        method: parsed.method,
        url: parsed.url,
        bodyType: parsed.bodyType,
        rawDataType: parsed.rawDataType,
        rawData: parsed.rawData,
        bodyData: parsed.bodyData,
        authType: parsed.authType,
        bearerToken: parsed.bearerToken,
        username: parsed.username,
        password: parsed.password,
      });
      setParamsList(parsed.params);
      setHeadersList(parsed.headers);

      // 同步初始化通用模板入参：query 参数映射到 $.requestParams.<key>，
      // raw JSON body 顶层字段映射到 $.requestBody.<key>，供后端 JSONPath 动态赋值
      const generatedInputParams: any[] = [];
      parsed.params.forEach((p) => {
        if (!p.key) return;
        generatedInputParams.push({
          name: p.key,
          type: 'string',
          description: p.description || '',
          required: true,
          defaultValue: p.value || '',
          referenceValue: `$.requestParams.${p.key}`,
        });
      });
      if (parsed.bodyType === 'raw' && parsed.rawDataType === 'json' && parsed.rawData) {
        try {
          const bodyObj = JSON.parse(parsed.rawData);
          if (bodyObj && typeof bodyObj === 'object' && !Array.isArray(bodyObj)) {
            Object.keys(bodyObj).forEach((key) => {
              const v = bodyObj[key];
              const t = typeof v;
              generatedInputParams.push({
                name: key,
                type: t === 'number' ? 'number' : t === 'boolean' ? 'boolean' : t === 'object' ? 'object' : 'string',
                description: '',
                required: true,
                defaultValue: v != null && t === 'object' ? JSON.stringify(v) : String(v ?? ''),
                referenceValue: `$.requestBody.${key}`,
              });
            });
          }
        } catch { /* body 非合法 JSON，忽略入参生成 */ }
      }
      if (generatedInputParams.length > 0) {
        setInputParams(generatedInputParams);
      }

      // 切换到含内容的 Tab，触发字段挂载并便于用户核对
      if (parsed.bodyType !== 'none') {
        setActiveTab('body');
      } else if (parsed.params.length > 0) {
        setActiveTab('params');
      }
      setCurlModalOpen(false);
      setCurlText('');
      app.message.success('cURL 解析完成，已填充配置');
    } catch (e: any) {
      console.error('解析 cURL 失败:', e);
      app.message.error('解析失败：' + (e?.message || '无效的 cURL 命令'));
    }
  };

  // 添加参数
  const addParam = () => {
    // 添加一行空数据
    const newParam = { key: '', value: '', description: '' };
    const newIndex = paramsList.length;
    setParamsList([...paramsList, newParam]);
    // 设置编辑状态为新添加的参数
    setEditingParam({ type: 'param', index: newIndex });
  };

  // 添加请求头
  const addHeader = () => {
    // 添加一行空数据
    const newHeader = { key: '', value: '', description: '' };
    const newIndex = headersList.length;
    setHeadersList([...headersList, newHeader]);
    // 设置编辑状态为新添加的请求头
    setEditingParam({ type: 'header', index: newIndex });
  };

  // 编辑参数
  const startEditingParam = (type: 'param' | 'header', index: number) => {
    setEditingParam({ type, index });
  };

  // 保存编辑的参数
  const saveEditedParam = (type: 'param' | 'header', index: number) => {
    // 验证参数是否有效
    if (type === 'param') {
      const param = paramsList[index];
      if (!param.key || !param.value) {
        app.message.error('键和值不能为空');
        return;
      }
    } else {
      const header = headersList[index];
      if (!header.key || !header.value) {
        app.message.error('键和值不能为空');
        return;
      }
    }
    setEditingParam(null);
  };

  // 取消编辑
  const cancelEditing = () => {
    setEditingParam(null);
  };

  // 删除参数
  const deleteParam = (type: 'param' | 'header', index: number) => {
    if (type === 'param') {
      const newParams = [...paramsList];
      newParams.splice(index, 1);
      setParamsList(newParams);
    } else {
      const newHeaders = [...headersList];
      newHeaders.splice(index, 1);
      setHeadersList(newHeaders);
    }
  };

  // 更新参数值
  const updateParamValue = (type: 'param' | 'header', index: number, field: string, value: string) => {
    if (type === 'param') {
      setParamsList(prevParamsList => {
        const newParams = [...prevParamsList];
        newParams[index] = { ...newParams[index], [field]: value };
        return newParams;
      });
    } else {
      setHeadersList(prevHeadersList => {
        const newHeaders = [...prevHeadersList];
        newHeaders[index] = { ...newHeaders[index], [field]: value };
        return newHeaders;
      });
    }
  };

  // 参数表格列定义
  const paramColumns = [
    {
      title: '键',
      dataIndex: 'key',
      key: 'key',
      width: '30%',
      render: (text, record, index) => {
        if (editingParam && editingParam.type === 'param' && editingParam.index === index) {
          return (
              <Input
                  value={paramsList[index].key}
                  onChange={(e) => updateParamValue('param', index, 'key', e.target.value)}
                  placeholder="输入键名"
                  autoFocus // 添加自动聚焦，方便直接输入
              />
          );
        }
        return text;
      },
    },
    {
      title: '值',
      dataIndex: 'value',
      key: 'value',
      width: '40%',
      render: (text, record, index) => {
        if (editingParam && editingParam.type === 'param' && editingParam.index === index) {
          return (
              <Input
                  value={paramsList[index].value}
                  onChange={(e) => updateParamValue('param', index, 'value', e.target.value)}
                  placeholder="输入值"
              />
          );
        }
        return text;
      },
    },
    {
      title: '说明',
      dataIndex: 'description',
      key: 'description',
      width: '20%',
      render: (text, record, index) => {
        if (editingParam && editingParam.type === 'param' && editingParam.index === index) {
          return (
              <Input
                  value={paramsList[index].description}
                  onChange={(e) => updateParamValue('param', index, 'description', e.target.value)}
                  placeholder="输入说明"
              />
          );
        }
        return text;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: '10%',
      render: (_, record, index) => (
          <Space>
            {editingParam && editingParam.type === 'param' && editingParam.index === index ? (
                <>
                  <Button type="link" size="small" onClick={() => saveEditedParam('param', index)}>保存</Button>
                  <Button type="link" danger size="small" onClick={cancelEditing}>取消</Button>
                </>
            ) : (
                <>
                  <Button type="link" size="small" onClick={() => startEditingParam('param', index)} icon={<EditOutlined />}>编辑</Button>
                  <Button type="link" danger size="small" onClick={() => deleteParam('param', index)} icon={<DeleteOutlined />}>删除</Button>
                </>
            )}
          </Space>
      ),
    },
  ];

  // 请求头表格列定义
  const headerColumns = [
    {
      title: '键',
      dataIndex: 'key',
      key: 'key',
      width: '30%',
      render: (text, record, index) => {
        if (editingParam && editingParam.type === 'header' && editingParam.index === index) {
          return (
              <Input
                  value={headersList[index].key}
                  onChange={(e) => updateParamValue('header', index, 'key', e.target.value)}
                  placeholder="输入键名"
                  autoFocus // 添加自动聚焦，方便直接输入
              />
          );
        }
        return text;
      },
    },
    {
      title: '值',
      dataIndex: 'value',
      key: 'value',
      width: '40%',
      render: (text, record, index) => {
        if (editingParam && editingParam.type === 'header' && editingParam.index === index) {
          return (
              <Input
                  value={headersList[index].value}
                  onChange={(e) => updateParamValue('header', index, 'value', e.target.value)}
                  placeholder="输入值"
              />
          );
        }
        return text;
      },
    },
    {
      title: '说明',
      dataIndex: 'description',
      key: 'description',
      width: '20%',
      render: (text, record, index) => {
        if (editingParam && editingParam.type === 'header' && editingParam.index === index) {
          return (
              <Input
                  value={headersList[index].description}
                  onChange={(e) => updateParamValue('header', index, 'description', e.target.value)}
                  placeholder="输入说明"
              />
          );
        }
        return text;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: '10%',
      render: (_, record, index) => (
          <Space>
            {editingParam && editingParam.type === 'header' && editingParam.index === index ? (
                <>
                  <Button type="link" size="small" onClick={() => saveEditedParam('header', index)}>保存</Button>
                  <Button type="link" danger size="small" onClick={cancelEditing}>取消</Button>
                </>
            ) : (
                <>
                  <Button type="link" size="small" onClick={() => startEditingParam('header', index)} icon={<EditOutlined />}>编辑</Button>
                  <Button type="link" danger size="small" onClick={() => deleteParam('header', index)} icon={<DeleteOutlined />}>删除</Button>
                </>
            )}
          </Space>
      ),
    },
  ];

  // 根据 bodyType 构建后端需要的 requestBody
  // raw(json) -> 解析为对象；raw(其它) -> 原始文本；form 类型 -> 键值文本；none -> undefined
  const buildRequestBody = (values: any): any => {
    if (values.bodyType === 'raw') {
      const raw = values.rawData;
      if (!raw) return undefined;
      if (values.rawDataType === 'json') {
        try {
          return JSON.parse(raw);
        } catch {
          return raw;
        }
      }
      return raw;
    }
    if (values.bodyType === 'x-www-form-urlencoded' || values.bodyType === 'form-data') {
      return values.bodyData || undefined;
    }
    return undefined;
  };

  // 保存：显式校验并提交，校验失败时定位到对应 Tab 并给出提示（避免静默无反应）
  const handleSave = async () => {
    try {
      await form.validateFields();
    } catch (err: any) {
      const firstField = err?.errorFields?.[0]?.name?.[0];
      const tabOf: Record<string, string> = {
        authType: 'auth', bearerToken: 'auth', username: 'auth', password: 'auth',
        apiKeyName: 'auth', apiKeyValue: 'auth',
        bodyType: 'body', rawDataType: 'body', rawData: 'body', bodyData: 'body',
      };
      if (firstField && tabOf[firstField]) {
        setActiveTab(tabOf[firstField]);
      }
      app.message.error('请完善必填项后再保存');
      return;
    }
    handleSubmit(null);
  };

  // 表单提交处理
  const handleSubmit = (_values: any) => {
    if (!agentId) {
      console.error('Agent ID is required to save HTTP config');
      return;
    }

    // 使用 getFieldsValue(true) 获取全部字段值（含未挂载 Tab 内的 Body/Auth 字段）
    const values = form.getFieldsValue(true);

    // 将HTTP请求参数保存到toolValue
    const httpConfig: any = {};
    // 将与HTTP请求相关的所有参数放入httpConfig对象中，paramsList和headersList除外
    Object.keys(values).forEach(key => {
      if (key !== 'toolInstanceName' && key !== 'displayName' && key !== 'paramsList' && key !== 'headersList') { // toolInstanceName/displayName应该在根级别
        httpConfig[key] = values[key];
      }
    });

    // 将表格格式的params和headers转换为JSON格式
    httpConfig.requestParams = paramsList;
    httpConfig.headers = headersList;
    // 按 bodyType 构建 requestBody（后端读取该字段）
    httpConfig.requestBody = buildRequestBody(values);

    // 构建工具配置对象
    const toolConfig = {
      agentId: Number(agentId),
      toolType: 'HTTP',
      displayName: values.displayName || '',
      toolInstanceName: values.toolInstanceName || 'HTTP请求',
      toolInstanceDesc: values.toolInstanceDesc || '',
      inputTemplate: JSON.stringify(inputParams),
      outTemplate: JSON.stringify(outputParams),
      isRequired: false,
      isActive: true,
      toolValue: JSON.stringify(httpConfig), // 将HTTP请求参数保存到toolValue字段
    };

    // 如果是从已有配置编辑，需要包含ID
    if (toolConfigs && toolConfigs.length > 0 && toolConfigs[0].id) {
      (toolConfig as any).id = toolConfigs[0].id;
    }

    // 调用API保存配置
    eaToolApi.addTool(toolConfig)
        .then((result) => {
          // 根据success字段或code字段判断是否保存成功
          if (result.success === true || result.code === 200) {
            console.log('HTTP config saved successfully:', result);
            // 检查API返回结果是否包含新工具ID
            if (result.data && result.data.id) {
              console.log('新创建的工具ID:', result.data.id);
            }
            app.message.success('HTTP配置保存成功');
            // 调用父组件传递的刷新函数以更新工具列表
            if (onRefresh) {
              onRefresh();
            }
          } else {
            console.error('Failed to save HTTP config:', result);
            const errorMessage = result.message || result.msg || 'Unknown error';
            app.message.error('HTTP配置保存失败: ' + errorMessage);
            // 即使失败也要调用刷新函数以更新列表状态
            if (onRefresh) {
              onRefresh();
            }
          }
        })
        .catch((error) => {
          console.error('Error saving HTTP config:', error);
          app.message.error('HTTP配置保存失败: ' + error.message);
        });
  };

  // 调试功能
  const handleDebug = async () => {
    try {
      setDebugging(true);
      // 调试只校验请求本身必需的字段，不强制要求工具名称/实例名等元信息
      await form.validateFields(['url', 'method']);
      // 使用 getFieldsValue(true) 获取全部字段值（含未挂载 Tab 内的 Body/Auth 字段）
      const values = form.getFieldsValue(true);

      // 将HTTP请求参数保存到toolValue
      const httpConfig: any = {};
      // 将与HTTP请求相关的所有参数放入httpConfig对象中，paramsList和headersList除外
      Object.keys(values).forEach(key => {
        if (key !== 'toolInstanceName' && key !== 'displayName' && key !== 'paramsList' && key !== 'headersList') { // toolInstanceName/displayName应该在根级别
          httpConfig[key] = values[key];
        }
      });

      // 将表格格式的params和headers转换为JSON格式
      httpConfig.requestParams = paramsList;
      httpConfig.headers = headersList;
      // 按 bodyType 构建 requestBody（后端读取该字段）
      httpConfig.requestBody = buildRequestBody(values);

      // 构建用于调试的工具配置对象
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'HTTP',
        toolInstanceName: values.toolInstanceName || 'HTTP请求',
        toolInstanceDesc: values.toolInstanceDesc || '',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        toolValue: JSON.stringify(httpConfig), // 将HTTP请求参数保存到toolValue字段
      };

      // 记录请求开始时间
      const startTime = Date.now();

      // 调用API进行调试
      eaToolApi.debug(toolConfig)
          .then((result) => {
            // 计算请求耗时
            const endTime = Date.now();
            const duration = endTime - startTime;

            // 将原始API响应和计算的时间添加到结果中
            const fullResult = {
              ...result,
              time: duration
            };
            
            setDebugResult(fullResult);
            console.log('HTTP调试完成:', result);
            
            if (!(result.success === true || result.code === 200)) {
              console.error('HTTP调试失败:', result);
            }
            setDebugging(false);
          })
          .catch((error) => {
            // 处理网络错误或其他异常
            const endTime = Date.now();
            const duration = endTime - startTime;
            
            const fullResult = {
              success: false,
              data: error.message || '请求失败',
              code: 500,
              message: error.message || '请求失败',
              time: duration,
            };
            setDebugResult(fullResult);
            console.error('HTTP调试错误:', error);
            setDebugging(false);
          });
    } catch (error) {
      setLoading(false);
      setDebugging(false);
      console.error('Validation failed:', error);
      app.message.error('请填写请求方法和请求 URL 后再发送');
    }
  };

  // 从工具配置加载HTTP配置数据
  useEffect(() => {
    if (toolConfigs && toolConfigs.length > 0) {
      const httpConfig = toolConfigs[0]; // 使用第一个配置（当从左侧菜单选择时，传入的toolConfigs数组只包含一个元素）
      if (httpConfig) {
        setHttpConfigData(httpConfig);

        // 从toolValue解析HTTP请求参数
        let httpParams = {};
        if (httpConfig.toolValue) {
          try {
            httpParams = JSON.parse(httpConfig.toolValue);
          } catch (e) {
            console.error('Error parsing HTTP params from toolValue:', e);
          }
        }

        // 解析params（从JSON格式）到paramsList
        const paramsListFromJson = [];
        if (httpParams.requestParams) {
          // 检查是否是字符串格式（旧格式）还是数组格式（新格式）
          if (typeof httpParams.requestParams === 'string') {
            // 旧格式：字符串格式，需要解析
            const paramsArray = httpParams.requestParams.split('\n');
            for (const param of paramsArray) {
              if (param.trim()) {
                const [key, ...valueParts] = param.split('=');
                if (key && valueParts.length > 0) {
                  paramsListFromJson.push({
                    key: key.trim(),
                    value: valueParts.join('=').trim(),
                    description: ''
                  });
                }
              }
            }
          } else if (Array.isArray(httpParams.requestParams)) {
            // 新格式：数组格式，直接使用
            paramsListFromJson.push(...httpParams.requestParams);
          }
        }

        // 解析headers（从JSON格式）到headersList
        const headersListFromJson = [];
        if (httpParams.headers) {
          // 检查是否是字符串格式（旧格式）还是数组格式（新格式）
          if (typeof httpParams.headers === 'string') {
            // 旧格式：字符串格式，需要解析
            const headersArray = httpParams.headers.split('\n');
            for (const header of headersArray) {
              if (header.trim()) {
                const [key, ...valueParts] = header.split(':');
                if (key && valueParts.length > 0) {
                  headersListFromJson.push({
                    key: key.trim(),
                    value: valueParts.join(':').trim(),
                    description: ''
                  });
                }
              }
            }
          } else if (Array.isArray(httpParams.headers)) {
            // 新格式：数组格式，直接使用
            headersListFromJson.push(...httpParams.headers);
          }
        }

        // 设置参数和头部列表
        setParamsList(paramsListFromJson);
        setHeadersList(headersListFromJson);

        // 将配置数据设置到表单(参数和头部现在以表格格式管理)
        const formValues = { ...httpParams };
        delete formValues.requestParams;
        delete formValues.headers;
        // 回显 requestBody：优先使用已持久化的 rawData/bodyData；
        // 仅当两者为空时（旧数据），再按 bodyType 从 requestBody 回填，保证向后兼容
        const hasRawData = formValues.rawData !== undefined && formValues.rawData !== '';
        const hasBodyData = formValues.bodyData !== undefined && formValues.bodyData !== '';
        if (!hasRawData && !hasBodyData && httpParams.requestBody !== undefined && httpParams.requestBody !== null) {
          const bodyStr = typeof httpParams.requestBody === 'string'
              ? httpParams.requestBody
              : JSON.stringify(httpParams.requestBody, null, 2);
          if (httpParams.bodyType === 'raw') {
            formValues.rawData = bodyStr;
          } else {
            formValues.bodyData = bodyStr;
          }
        }
        delete formValues.requestBody;
        formValues.displayName = httpConfig.displayName || '';
        formValues.toolInstanceName = httpConfig.toolInstanceName || 'HTTP请求';
        formValues.toolInstanceDesc = httpConfig.toolInstanceDesc || 'HTTP请求工具描述';

        form.setFieldsValue(formValues);

        // 设置输入和输出参数
        if (httpConfig.inputTemplate) {
          try {
            setInputParams(JSON.parse(httpConfig.inputTemplate));
          } catch (e) {
            console.error('Error parsing input template:', e);
          }
        }

        if (httpConfig.outTemplate) {
          try {
            setOutputParams(JSON.parse(httpConfig.outTemplate));
          } catch (e) {
            console.error('Error parsing output template:', e);
          }
        }
      }
    } else {
      // 如果没有工具配置，则设置默认值
      form.setFieldsValue({
        displayName: '',
        toolInstanceName: 'HTTP请求',
        toolInstanceDesc: 'HTTP请求工具描述',
        method: 'GET',
        url: '',
        bodyType: 'none',
        rawDataType: 'json',
        rawData: '',
        bodyData: '',
        connectTimeout: 5000,
        readTimeout: 10000,
        timeout: 15000,
        authType: 'none',
        bearerToken: '',
        username: '',
        password: '',
        apiKeyName: '',
        apiKeyValue: '',
      });

      // 将基本参数和头部设置为空值
      setParamsList([]);
      setHeadersList([]);

      // 设置基本输入和输出参数
      setInputParams([
        { name: 'id', type: 'string', description: '请求参数', required: true, defaultValue: '1' }
      ]);
      setOutputParams([
        { name: 'result', type: 'object', description: '响应结果', required: true }
      ]);
    }
  }, [toolConfigs, form]);


  return (
      <Card
          title="HTTP请求配置"
          size="small"
          extra={
            <Space>
              <Button type="primary" ghost size="small" icon={<ImportOutlined />} onClick={() => setCurlModalOpen(true)}>
                导入 cURL
              </Button>
              <Button type="primary" size="small" onClick={handleSave}>
                保存
              </Button>
            </Space>
          }
      >
          <Modal
              title="导入 cURL"
              open={curlModalOpen}
              onOk={applyCurl}
              onCancel={() => setCurlModalOpen(false)}
              okText="解析并填充"
              cancelText="取消"
              width={680}
              destroyOnClose
          >
            <Alert
                type="info"
                showIcon
                message="粘贴完整的 cURL 命令，自动解析请求方法、URL、Headers、Query 参数与 Body。"
                style={{ marginBottom: 12 }}
            />
            <Input.TextArea
                value={curlText}
                onChange={(e) => setCurlText(e.target.value)}
                rows={10}
                placeholder={`例如:\ncurl -X POST 'https://api.example.com/users?page=1' \\\n  -H 'Content-Type: application/json' \\\n  -H 'Authorization: Bearer xxx' \\\n  -d '{"name":"tom"}'`}
            />
          </Modal>
          <Tabs defaultActiveKey="params">
            <TabPane tab="参数配置" key="params">
                  <Form form={form} layout="vertical" size="small" onFinish={handleSubmit}>
                    <Row gutter={16}>
                      <Col span={12}>
                        <Form.Item
                            name="displayName"
                            label="工具名称"
                            tooltip="仅用于左侧工具列表展示，不参与工具调用"
                            rules={[{ required: true, message: '请输入工具名称' }]}
                            style={{ marginBottom: 12 }}
                        >
                          <Input placeholder="例如: 贵金属价格查询" />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item
                            name="toolInstanceName"
                            label="工具实例名称"
                            rules={[
                              { required: true, message: '请输入工具实例名称' },
                              { pattern: /^[A-Za-z0-9_-]+$/, message: '只能输入英文字母、数字、下划线或连字符' },
                            ]}
                            style={{ marginBottom: 12 }}
                        >
                          <Input
                            placeholder="例如: queryPreciousMetalsPriceByDate"
                            onChange={(e) => {
                              const filtered = e.target.value.replace(/[^A-Za-z0-9_-]/g, '');
                              form.setFieldValue('toolInstanceName', filtered);
                            }}
                          />
                        </Form.Item>
                      </Col>
                    </Row>
                    <Form.Item
                        name="toolInstanceDesc"
                        label="工具实例描述"
                        rules={[{ required: true, message: '请输入工具实例描述' }]}
                        style={{ marginBottom: 12 }}
                    >
                      <Input.TextArea placeholder="请输入工具实例描述" autoSize={{ minRows: 1, maxRows: 3 }} />
                    </Form.Item>

                  <Row gutter={16}>
                    <Col span={6}>
                      <Form.Item
                          name="method"
                          label="请求方法"
                          initialValue="GET"
                      >
                        <Select>
                          <Select.Option value="GET">GET</Select.Option>
                          <Select.Option value="POST">POST</Select.Option>
                          <Select.Option value="PUT">PUT</Select.Option>
                          <Select.Option value="PATCH">PATCH</Select.Option>
                          <Select.Option value="DELETE">DELETE</Select.Option>
                          <Select.Option value="HEAD">HEAD</Select.Option>
                          <Select.Option value="OPTIONS">OPTIONS</Select.Option>
                        </Select>
                      </Form.Item>
                    </Col>
                    <Col span={15}>
                      <Form.Item
                          name="url"
                          label="请求URL"
                          rules={[{ required: true, message: '请输入请求URL' }]}
                      >
                        <Input placeholder="例如: https://api.example.com/users" />
                      </Form.Item>
                    </Col>
                    <Col span={3}>
                      <Form.Item label="&nbsp;">
                        <Button
                            type="primary"
                            onClick={handleDebug}
                            loading={debugging}
                            style={{ width: '100%' }}
                        >
                          发送
                        </Button>
                      </Form.Item>
                    </Col>
                  </Row>

                  <Tabs defaultActiveKey="auth" activeKey={activeTab} onChange={setActiveTab}>
                    <TabPane tab="认证配置" key="auth">
                        <Form.Item
                            name="authType"
                            label="认证方法"
                        >
                          <Select placeholder="选择认证方法">
                            <Select.Option value="none">无认证</Select.Option>
                            <Select.Option value="bearer">Bearer Token</Select.Option>
                            <Select.Option value="basic">Basic Auth</Select.Option>
                            <Select.Option value="apikey">API Key</Select.Option>
                          </Select>
                        </Form.Item>

                        <Form.Item
                            noStyle
                            shouldUpdate={(prevValues, currentValues) => prevValues.authType !== currentValues.authType}
                        >
                          {({ getFieldValue }) => {
                            const authType = getFieldValue('authType');
                            if (authType === 'bearer') {
                              return (
                                  <Form.Item
                                      name="bearerToken"
                                      label="Bearer Token"
                                      rules={[{ required: true, message: '请输入Bearer Token' }]}
                                  >
                                    <Input placeholder="输入Bearer Token" />
                                  </Form.Item>
                              );
                            }

                            if (authType === 'basic') {
                              return (
                                  <>
                                    <Form.Item
                                        name="username"
                                        label="用户名"
                                        rules={[{ required: true, message: '请输入用户名' }]}
                                    >
                                      <Input placeholder="输入用户名" />
                                    </Form.Item>

                                    <Form.Item
                                        name="password"
                                        label="密码"
                                        rules={[{ required: true, message: '请输入密码' }]}
                                    >
                                      <Input.Password placeholder="输入密码" />
                                    </Form.Item>
                                  </>
                              );
                            }

                            if (authType === 'apikey') {
                              return (
                                  <>
                                    <Form.Item
                                        name="apiKeyName"
                                        label="API Key名称"
                                        rules={[{ required: true, message: '请输入API Key名称' }]}
                                    >
                                      <Input placeholder="例如: X-API-Key" />
                                    </Form.Item>

                                    <Form.Item
                                        name="apiKeyValue"
                                        label="API Key值"
                                        rules={[{ required: true, message: '请输入API Key值' }]}
                                    >
                                      <Input placeholder="输入API Key值" />
                                    </Form.Item>
                                  </>
                              );
                            }

                            return null;
                          }}
                        </Form.Item>
                    </TabPane>
                    <TabPane tab="Params" key="params">
                      <Table
                          columns={paramColumns}
                          dataSource={paramsList}
                          pagination={false}
                          size="small"
                      />
                      <Button type="dashed" onClick={addParam} icon={<PlusOutlined />} block style={{ marginTop: 8 }}>
                        添加参数
                      </Button>
                    </TabPane>

                    <TabPane tab="Headers" key="headers">
                      <Table
                          columns={headerColumns}
                          dataSource={headersList}
                          pagination={false}
                          size="small"
                      />
                      <Button type="dashed" onClick={addHeader} icon={<PlusOutlined />} block style={{ marginTop: 8 }}>
                        添加请求头
                      </Button>
                    </TabPane>

                    <TabPane tab="Body" key="body">
                      <Form.Item
                          name="bodyType"
                          label="Body类型"
                          initialValue="none"
                      >
                        <Select>
                          <Select.Option value="none">none</Select.Option>
                          <Select.Option value="form-data">form-data</Select.Option>
                          <Select.Option value="x-www-form-urlencoded">x-www-form-urlencoded</Select.Option>
                          <Select.Option value="raw">raw</Select.Option>
                          <Select.Option value="binary">binary</Select.Option>
                        </Select>
                      </Form.Item>

                      <Form.Item
                          noStyle
                          shouldUpdate={(prevValues, currentValues) => prevValues.bodyType !== currentValues.bodyType}
                      >
                        {({ getFieldValue }) => {
                          const bodyType = getFieldValue('bodyType');
                          if (bodyType === 'raw') {
                            return (
                                <>
                                  <Form.Item
                                      name="rawDataType"
                                      label="数据类型"
                                  >
                                    <Select style={{ width: 120 }}>
                                      <Select.Option value="text">Text</Select.Option>
                                      <Select.Option value="json">JSON</Select.Option>
                                      <Select.Option value="xml">XML</Select.Option>
                                      <Select.Option value="html">HTML</Select.Option>
                                    </Select>
                                  </Form.Item>
                                  <Form.Item
                                      name="rawData"
                                      label="数据内容"
                                  >
                                    <Input.TextArea rows={6} placeholder="输入请求正文内容" />
                                  </Form.Item>
                                </>
                            );
                          }

                          if (bodyType === 'x-www-form-urlencoded' || bodyType === 'form-data') {
                            return (
                                <Form.Item
                                    name="bodyData"
                                    label="键值数据"
                                >
                                  <Input.TextArea
                                      rows={4}
                                      placeholder='例如:
key1=value1
key2=value2'
                                  />
                                </Form.Item>
                            );
                          }

                          return null;
                        }}
                      </Form.Item>
                    </TabPane>
                    
                    <TabPane tab="Settings" key="settings">
                      <Form.Item
                          name="connectTimeout"
                          label="连接超时(ms)"
                          initialValue={5000}
                      >
                        <Input type="number" placeholder="例如: 5000" />
                      </Form.Item>
                      <Form.Item
                          name="readTimeout"
                          label="返回超时(ms)"
                          initialValue={10000}
                      >
                        <Input type="number" placeholder="例如: 10000" />
                      </Form.Item>
                      <Form.Item
                          name="timeout"
                          label="请求超时(ms)"
                          initialValue={15000}
                      >
                        <Input type="number" placeholder="例如: 15000" />
                      </Form.Item>
                    </TabPane>
                  </Tabs>


                </Form>

            {/* 调试结果展示区域 */}
            <DebugResult debugResult={debugResult} loading={debugging} title="HTTP调试结果" />
          </TabPane>

          <TabPane tab="通用模板配置" key="template">
            <CommonTemplateConfig
                inputParams={inputParams}
                outputParams={outputParams}
                onInputParamsChange={setInputParams}
                onOutputParamsChange={setOutputParams}
            />
          </TabPane>
        </Tabs>
      </Card>
  );

};

export default HTTPConfig;