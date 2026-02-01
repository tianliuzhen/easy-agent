// src/pages/EaAgentPage.tsx
import React, { useEffect, useState, useRef } from 'react';
import { Button, Table, Modal, Form, Input, Space, Popconfirm, Select, Card, List, Row, Col, Divider } from 'antd';
import { App } from 'antd';
import { Link, useNavigate } from 'react-router-dom';

import type { ColumnsType } from 'antd/es/table';
import { eaAgentApi } from '../api/EaAgentApi';
import { EditOutlined, SettingOutlined, DeleteOutlined, MessageOutlined, PlusOutlined, ApiOutlined, ThunderboltOutlined } from '@ant-design/icons';

// 添加动态渐变动画的CSS
const styles = `
  @keyframes gradientShift {
    0% {
      background-position: 0% 50%;
    }
    50% {
      background-position: 100% 50%;
    }
    100% {
      background-position: 0% 50%;
    }
  }
  
  @keyframes handIconPulse {
    0% {
      transform: scale(1);
      opacity: 0.8;
    }
    50% {
      transform: scale(1.2);
      opacity: 1;
    }
    100% {
      transform: scale(1);
      opacity: 0.8;
    }
  }
`;

const EaAgentPage: React.FC = () => {
  const { message,modal } = App.useApp();
  const navigate = useNavigate();

  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [analysisModels, setAnalysisModels] = useState<string[]>([]);
  const [searchText, setSearchText] = useState('');
  // 添加 modelConfigFields 状态用于管理字段数据
  const [modelConfigFields, setModelConfigFields] = useState<any[]>([
    { fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API密钥' },
    { fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础URL' },
    { fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本' },
    { fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径' },
  ]);

  // 添加默认图标推荐状态
  const [showDefaultIcons, setShowDefaultIcons] = useState(false);
  const iconSelectorRef = useRef<HTMLDivElement>(null);
  const iconInputRef = useRef<any>(null);

  // 定义默认图标数组
  const defaultIcons = [
    '🤖', '🧠', '🚀', '⚡', '🎯', '💡', '🔮', '🎓', '🎩', '👁️',
    '⚙️', '🌐', '🎨', '🎭', '🎬', '🎤', '🎧', '🎹', '🥁', '🎷',
    '🎸', '🎺', '🎻', '📱', '📲', '🔋', '🔌', '💻', '⌨️', '🖱️',
    '🖨️', '💾', '💿', '📀', '📼', '📷', '📸', '📹', '🎥', '📽️',
    '🎞️', '📞', '📟', '📺', '📻', '🎙️', '🎚️', '🎛️', '🧭', '🧱',
    '❤️', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️',
    '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️',
    '☪️', '🕉️', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎',
    '♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑',
    '♒', '♓', '🆔', '⚛️', '🉑', '☢️', '☣️', '📴', '📳', '🈶'
  ];

  // 处理图标选择 - 修复版本
  const handleIconSelect = (icon: string) => {
    console.log('图标被选中:', icon);

    // 直接更新表单值
    form.setFieldsValue({
      avatar: icon
    });

    // 隐藏图标选择器
    setShowDefaultIcons(false);

    // 确保输入框获得焦点以显示图标
    setTimeout(() => {
      if (iconInputRef.current) {
        const inputElement = iconInputRef.current.input;
        if (inputElement) {
          inputElement.focus();
          // 设置光标位置到末尾
          inputElement.setSelectionRange(icon.length, icon.length);
        }
      }
    }, 100);
  };

  // 处理文档点击，用于关闭图标选择器
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;

      // 如果图标选择器存在且点击事件不在选择器内部，则关闭选择器
      if (showDefaultIcons && iconSelectorRef.current &&
          !iconSelectorRef.current.contains(target) &&
          !iconInputRef.current?.input?.contains(target)) {
        setShowDefaultIcons(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showDefaultIcons]);

  // 处理图标输入框聚焦 - 显示图标选择器
  const handleIconFocus = () => {
    console.log('输入框聚焦，显示图标选择器');
    setShowDefaultIcons(true);
  };

  // 处理图标输入框失焦 - 延迟隐藏
  const handleIconBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    console.log('输入框失焦事件');
    // 延迟隐藏，让用户有时间点击图标
    setTimeout(() => {
      // 检查当前激活的元素是否在图标选择器内
      const activeElement = document.activeElement;
      const isFocusInSelector = iconSelectorRef.current?.contains(activeElement as Node);

      if (!isFocusInSelector) {
        setShowDefaultIcons(false);
      }
    }, 300);
  };

  // 加载数据
  const loadData = async (agentName?: string) => {
    setLoading(true);
    try {
      const req = agentName ? { agentName } : {};
      const result = await eaAgentApi.listAgent(req);
      setData(result?.data);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }

    eaAgentApi.queryChatModelTypeList().then((response) => {
      // 处理后端返回的Map<String, HashMap<String, String>>格式
      const modelData = response?.data;
      if (modelData && typeof modelData === 'object') {
        // 提取所有模型的键作为可选项
        setAnalysisModels(Object.keys(modelData));
      } else {
        setAnalysisModels([]);
      }
    }).catch((error) => {
      console.error("Failed to fetch model type list:", error);
      setAnalysisModels([]); // 设置默认值
    });
  };

  // 搜索功能
  const handleSearch = () => {
    loadData(searchText);
  };

  // 重置搜索
  const handleReset = () => {
    setSearchText('');
    loadData();
  };

  useEffect(() => {
    loadData();
  }, []);

  // 显示添加对话框
  const showAddModal = () => {
    form.resetFields();
    // 设置默认值
    form.setFieldsValue({
      toolRunMode: 'ReAct',  // 默认选择 ReAct
      avatar: '🤖'        // 默认图标
    });
    setEditingId(null);
    // 初始化模型配置字段
    setModelConfigFields([
      { fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API密钥' },
      { fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础URL' },
      { fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本' },
      { fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径' },
    ]);
    setIsModalOpen(true);
  };

  // 显示编辑对话框
  const handleEdit = (record: any) => {
    console.log('编辑记录:', record);

    // 处理 record 中的 modelPlatform 或 analysisModel 字段
    // 如果 record 中有 agentIcon 字段，将其映射到 avatar 字段
    const formData = {
      ...record,
      avatar: record.avatar || record.agentIcon || '🤖', // 优先使用 avatar，其次使用 agentIcon，最后使用默认值
      modelPlatform: record.modelPlatform || record.analysisModel, // 优先使用 modelPlatform，否则使用 analysisModel
      toolRunMode: record.toolRunMode || 'ReAct', // 如果没有设置则默认为 ReAct
    };

    console.log('设置表单数据:', formData);
    form.setFieldsValue(formData);

    // 设置 modelConfig 字段数据
    if (record.modelConfig) {
      try {
        const configObj = typeof record.modelConfig === 'string' ?
            JSON.parse(record.modelConfig) : record.modelConfig;

        const updatedFields = modelConfigFields.map(field => ({
          ...field,
          fieldValue: configObj[field.fieldName] || ''
        }));
        setModelConfigFields(updatedFields);
      } catch (e) {
        console.error('解析 modelConfig 失败:', e);
        // 如果解析失败，保持默认值
        setModelConfigFields([
          { fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API密钥' },
          { fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础URL' },
          { fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本' },
          { fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径' },
        ]);
      }
    } else {
      // 如果没有 modelConfig，使用默认值
      setModelConfigFields([
        { fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API密钥' },
        { fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础URL' },
        { fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本' },
        { fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径' },
      ]);
    }

    setEditingId(record.id);
    setIsModalOpen(true);


  };

  // 配置记录
  const handleConfigure = (record: any) => {
    window.open(`/pageTool/AgentConfig?agentId=${record.id}`, '_blank');
  };

  // 删除记录
  const handleDelete = async (record: any) => {
    modal.confirm({
      title: '确认删除',
      content: '确定要删除这条记录吗？',
      onOk: async () => {
        try {
          await eaAgentApi.delAgent(record);
          message.success('删除成功');
          loadData();
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  // 处理模型配置字段的变更
  const handleModelConfigFieldChange = (fieldName: string, value: string) => {
    const newFields = modelConfigFields.map(field =>
        field.fieldName === fieldName ? { ...field, fieldValue: value } : field
    );
    setModelConfigFields(newFields);
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      console.log('提交的表单数据:', values);

      // 将字段数据转换为 JSON 对象
      const modelConfigObj: any = {};
      modelConfigFields.forEach(field => {
        if (field.fieldName) {
          modelConfigObj[field.fieldName] = field.fieldValue;
        }
      });

      const agentData = {
        ...values,
        id: editingId || undefined,
        // 使用 modelPlatform 作为主要字段，同时保留 analysisModel 以确保兼容性
        modelPlatform: values.modelPlatform,
        analysisModel: values.modelPlatform, // 同时赋值给 analysisModel 以保持兼容性
        // 将字段数据转换为 JSON 字符串存储
        modelConfig: JSON.stringify(modelConfigObj),
      };

      console.log('提交的Agent数据:', agentData);
      await eaAgentApi.saveAgent(agentData);
      message.success(editingId ? '更新成功' : '添加成功');
      setIsModalOpen(false);
      // 重置字段数据
      setModelConfigFields([
        { fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API密钥' },
        { fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础URL' },
        { fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本' },
        { fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径' },
      ]);
      loadData();
    } catch (error) {
      message.error('提交失败:');
    }
  };

  return (
      <>
        <style>{styles}</style>
        <div style={{ padding: 0, margin: 0 }}>
          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
            <div style={{ display: 'flex', gap: 8 }}>
              <Input
                  placeholder="输入Agent名称搜索"
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  style={{ width: 200 }}
                  onPressEnter={handleSearch}
              />
              <Button type="primary" onClick={handleSearch}>
                搜索
              </Button>
              <Button onClick={handleReset}>
                重置
              </Button>
            </div>
            <Button type="primary" onClick={showAddModal}>
              添加Agent
            </Button>
          </div>

          <List
              grid={{
                gutter: 16,
                xs: 1,
                sm: 2,
                md: 3,
                lg: 3,
                xl: 4,
                xxl: 4,
              }}
              loading={loading}
              dataSource={data}
              renderItem={(item) => (
                  <List.Item>
                    <Card
                        size="small"
                        title={
                          <>
                            <div style={{ display: 'flex', alignItems: 'center', fontSize: '16px', fontWeight: 'bold', color: 'white' }}>
                      <span style={{ marginRight: '8px', marginTop: '1px', fontSize: '18px' }}>
                        {item.avatar || item.agentIcon || '🤖'}
                      </span>
                              <Link to={`/home/chatdemo?agentId=${item.id}`} style={{ color: 'black', display: 'flex', alignItems: 'center', fontSize: '16px', fontWeight: 'bold' }}>
                                {item.agentName}
                              </Link>
                              <Link to={`/home/chatdemo?agentId=${item.id}`} style={{ marginLeft: '8px', color: 'rgba(255, 255, 255, 0.85)' }} title="开始对话">
                                <MessageOutlined />
                              </Link>
                            </div>
                            {item.agentDesc && (
                                <div style={{ fontSize: '12px', marginTop: '4px', color: 'rgba(255, 255, 255, 0.7)', fontStyle: 'italic' }}>
                                  {item.agentDesc}
                                </div>
                            )}
                          </>
                        }
                        extra={
                          <Space>
                            <Button
                                type="text"
                                size="small"
                                onClick={(e) => {
                                  e.stopPropagation(); // 阻止事件冒泡到卡片
                                  handleEdit(item);
                                }}
                                title="编辑"
                                style={{ color: 'black', padding: '2px 4px' }}
                            >
                              <EditOutlined style={{ fontSize: '16px' }} />
                            </Button>
                            <Button
                                type="text"
                                size="small"
                                onClick={(e) => {
                                  e.stopPropagation(); // 阻止事件冒泡到卡片
                                  handleConfigure(item);
                                }}
                                title="配置"
                                style={{ color: '#1890ff', padding: '2px 4px' }}
                            >
                              <SettingOutlined style={{ fontSize: '16px' }} />
                            </Button>
                            <Popconfirm
                                title="确认删除"
                                description="确定要删除这条记录吗？"
                                onConfirm={(e) => {
                                  e?.stopPropagation(); // 阻止事件冒泡
                                  handleDelete(item);
                                }}
                                okText="确定"
                                cancelText="取消"
                            >
                              <Button
                                  type="text"
                                  size="small"
                                  title="删除"
                                  style={{ color: '#ff4d4f', padding: '2px 4px' }}
                                  onClick={(e) => e.stopPropagation()} // 防止点击删除按钮也触发卡片跳转
                              >
                                <DeleteOutlined style={{ fontSize: '16px' }} />
                              </Button>
                            </Popconfirm>
                          </Space>
                        }
                        style={{
                          background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                          backgroundSize: '300% 300%',
                          animation: 'gradientShift 8s ease infinite',
                          color: 'white',
                          transition: 'all 0.3s ease-in-out',
                          cursor: 'pointer',
                          height: '200px',
                          display: 'flex',
                          flexDirection: 'column',
                          border: '1px solid rgba(255, 255, 255, 0.2)'
                        }}
                        onClick={() => {
                          window.open(`/home/chatdemo?agentId=${item.id}`, '_blank');
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.transform = 'translateY(-5px)';
                          e.currentTarget.style.boxShadow = '0 6px 16px rgba(0, 0, 0, 0.15)';
                          e.currentTarget.style.borderColor = '#1890ff';
                          e.currentTarget.style.animationPlayState = 'paused';

                          // 添加动态手型图标
                          const handIcon = e.currentTarget.querySelector('.hand-icon');
                          if(handIcon) {
                            handIcon.style.animation = 'handIconPulse 1s infinite';
                          }
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.transform = 'translateY(0)';
                          e.currentTarget.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.1)';
                          e.currentTarget.style.borderColor = '#f0f0f0';
                          e.currentTarget.style.animationPlayState = 'running';

                          // 移除动态手型图标动画
                          const handIcon = e.currentTarget.querySelector('.hand-icon');
                          if(handIcon) {
                            handIcon.style.animation = 'none';
                          }
                        }}
                    >
                      <div style={{
                        flex: 1,
                        overflow: 'hidden',
                        padding: '8px 0',
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                        position: 'relative'
                      }}>
                        <div style={{
                          marginBottom: 6,
                          color: 'rgba(255, 255, 255, 0.9)',
                          fontSize: '14px',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          display: 'flex',
                          alignItems: 'center'
                        }}>
                          <ThunderboltOutlined style={{ marginRight: 6, fontSize: '12px' }} />
                          {item.modelPlatform}
                        </div>
                        {item.modelConfig && (() => {
                          try {
                            const config = typeof item.modelConfig === 'string' ?
                                JSON.parse(item.modelConfig) : item.modelConfig;
                            return config.modelVersion ? (
                                <div style={{
                                  marginBottom: 6,
                                  color: 'rgba(255, 255, 255, 0.85)',
                                  fontSize: '13px',
                                  whiteSpace: 'nowrap',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  display: 'flex',
                                  alignItems: 'center'
                                }}>
                                  <ApiOutlined style={{ marginRight: 6, fontSize: '12px' }} />
                                  {config.modelVersion}
                                </div>
                            ) : null;
                          } catch (e) {
                            return null;
                          }
                        })()}
                        {item.toolRunMode && (
                            <div style={{
                              marginBottom: 6,
                              color: 'rgba(255, 255, 255, 0.8)',
                              fontSize: '13px',
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              display: 'flex',
                              alignItems: 'center'
                            }}>
                              <ThunderboltOutlined style={{ marginRight: 6, fontSize: '12px', color: '#fa8c16' }} />
                              {item.toolRunMode}
                            </div>
                        )}
                      </div>
                      <div className="hand-icon" style={{
                        position: 'absolute',
                        bottom: '10px',
                        right: '10px',
                        fontSize: '16px',
                        color: '#1890ff',
                        opacity: 0.7,
                        zIndex: 1
                      }}>
                        <MessageOutlined />
                      </div>
                    </Card>
                  </List.Item>
              )}
          />

          <Modal
              title={editingId ? '编辑Agent' : '添加Agent'}
              open={isModalOpen}
              onOk={handleSubmit}
              onCancel={() => {
                setIsModalOpen(false);
                setModelConfigFields([
                  { fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API密钥' },
                  { fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础URL' },
                  { fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本' },
                  { fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径' },
                ]); // 关闭时重置字段数据
              }}
              destroyOnClose
              width={1000}
          >
            <Form form={form} layout="vertical">
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                      name="agentName"
                      label="Agent名称"
                      rules={[{ required: true, message: '请输入Agent名称' }]}
                  >
                    <Input placeholder="请输入Agent名称" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                      name="modelPlatform"
                      label="模型平台"
                      rules={[{ required: true, message: '请选择模型平台' }]}
                  >
                    <Select placeholder="请选择模型平台">
                      {analysisModels?.map(model => (
                          <Select.Option key={model} value={model}>
                            {model}
                          </Select.Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                      name="toolRunMode"
                      label="运行模式"
                      rules={[{ required: true, message: '请选择运行模式' }]}
                  >
                    <Select placeholder="请选择运行模式">
                      <Select.Option value="ReAct">ReAct</Select.Option>
                      <Select.Option value="Tool">Tool</Select.Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="avatar" label="Agent图标">
                    <div className="agent-icon-input-wrapper" style={{ position: 'relative' }}>
                      <Input
                          ref={iconInputRef}
                          placeholder="请输入图标Unicode或emoji，例如: 🤖"
                          onFocus={handleIconFocus}
                          onBlur={handleIconBlur}
                          style={{ width: '100%' }}
                          value={form.getFieldValue('avatar') || ''}
                          onChange={(e) => form.setFieldsValue({ avatar: e.target.value })}
                      />
                      {showDefaultIcons && (
                          <div
                              ref={iconSelectorRef}
                              className="agent-icon-selector-content"
                              style={{
                                position: 'absolute',
                                top: '100%',
                                left: 0,
                                right: 0,
                                backgroundColor: 'white',
                                border: '1px solid #d9d9d9',
                                borderRadius: '4px',
                                padding: '8px',
                                zIndex: 9999,
                                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
                                maxHeight: '200px',
                                overflowY: 'auto'
                              }}
                          >
                            <div style={{ marginBottom: '8px', fontSize: '12px', color: '#666' }}>点击选择默认图标:</div>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                              {defaultIcons.map((icon, index) => (
                                  <span
                                      key={index}
                                      onClick={(e) => {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        console.log('点击图标:', icon);
                                        handleIconSelect(icon);
                                      }}
                                      style={{
                                        cursor: 'pointer',
                                        fontSize: '18px',
                                        padding: '4px',
                                        borderRadius: '4px',
                                        transition: 'background-color 0.2s',
                                      }}
                                      onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f0f0f0'}
                                      onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                                  >
                              {icon}
                            </span>
                              ))}
                            </div>
                          </div>
                      )}
                    </div>
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item label="模型配置">
                <div style={{ border: '1px solid #e8e8e8', borderRadius: '4px', padding: '16px' }}>
                  <h4 style={{ margin: '0 0 16px 0', fontWeight: 'bold' }}>模型配置参数</h4>

                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item
                          label="API密钥"
                      >
                        <Input
                            value={modelConfigFields.find(f => f.fieldName === 'apiKey')?.fieldValue}
                            onChange={(e) => handleModelConfigFieldChange('apiKey', e.target.value)}
                            placeholder="请输入API密钥"
                            type="password"
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                          label="基础URL"
                      >
                        <Input
                            value={modelConfigFields.find(f => f.fieldName === 'baseUrl')?.fieldValue}
                            onChange={(e) => handleModelConfigFieldChange('baseUrl', e.target.value)}
                            placeholder="请输入基础URL"
                        />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item
                          label="模型版本"
                      >
                        <Input
                            value={modelConfigFields.find(f => f.fieldName === 'modelVersion')?.fieldValue}
                            onChange={(e) => handleModelConfigFieldChange('modelVersion', e.target.value)}
                            placeholder="请输入模型版本"
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                          label="补全路径"
                      >
                        <Input
                            value={modelConfigFields.find(f => f.fieldName === 'completionsPath')?.fieldValue}
                            onChange={(e) => handleModelConfigFieldChange('completionsPath', e.target.value)}
                            placeholder="请输入补全路径"
                        />
                      </Form.Item>
                    </Col>
                  </Row>
                </div>
              </Form.Item>

              <Form.Item name="agentDesc" label="备注">
                <Input.TextArea placeholder="请输入备注信息" rows={4} />
              </Form.Item>
            </Form>
          </Modal>
        </div>
      </>
  );
};

// Entry component
export default () => (
    <App>
      <EaAgentPage />
    </App>
);