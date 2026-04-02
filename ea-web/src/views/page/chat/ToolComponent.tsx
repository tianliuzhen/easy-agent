import React, { useState } from 'react';
import { Collapse, Typography, Space, Tag, Button, Tooltip, Card, Row, Col, Modal, Descriptions } from 'antd';
import {
  DownOutlined,
  RightOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ToolOutlined,
  PlayCircleOutlined,
  EyeOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import type { ToolCall } from './types';

const { Text, Paragraph } = Typography;

interface ToolComponentProps {
  tools?: ToolCall[];
  defaultExpanded?: boolean;
  onRetry?: (toolId: string) => void;
  onViewParams?: (tool: ToolCall) => void;
}

/**
 * 工具调用展示组件
 * 支持工具执行状态展示、参数查看、重新执行
 */
const ToolComponent: React.FC<ToolComponentProps> = ({
  tools,
  defaultExpanded = false,
  onRetry,
  onViewParams
}) => {
  const [expandedTools, setExpandedTools] = useState<Set<string>>(new Set());
  const [selectedTool, setSelectedTool] = useState<ToolCall | null>(null);
  const [paramsModalVisible, setParamsModalVisible] = useState(false);

  if (!tools || tools.length === 0) {
    return null;
  }

  const toggleTool = (toolId: string) => {
    setExpandedTools(prev => {
      const newSet = new Set(prev);
      if (newSet.has(toolId)) {
        newSet.delete(toolId);
      } else {
        newSet.add(toolId);
      }
      return newSet;
    });
  };

  const handleViewParams = (tool: ToolCall) => {
    setSelectedTool(tool);
    setParamsModalVisible(true);
    if (onViewParams) {
      onViewParams(tool);
    }
  };

  const handleRetry = (toolId: string) => {
    if (onRetry) {
      onRetry(toolId);
    }
  };

  const getStatusIcon = (status: ToolCall['status']) => {
    switch (status) {
      case 'pending':
        return <LoadingOutlined spin style={{ color: '#faad14' }} />;
      case 'running':
        return <LoadingOutlined spin style={{ color: '#1890ff' }} />;
      case 'success':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'error':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
      default:
        return <LoadingOutlined spin style={{ color: '#faad14' }} />;
    }
  };

  const getStatusTag = (status: ToolCall['status']) => {
    switch (status) {
      case 'pending':
        return <Tag color="warning">待执行</Tag>;
      case 'running':
        return <Tag color="processing">执行中</Tag>;
      case 'success':
        return <Tag color="success">成功</Tag>;
      case 'error':
        return <Tag color="error">失败</Tag>;
      default:
        return <Tag>未知</Tag>;
    }
  };

  const formatDuration = (duration?: number) => {
    if (!duration) return null;
    if (duration < 1000) return `${duration}ms`;
    return `${(duration / 1000).toFixed(2)}s`;
  };

  return (
    <div style={{ marginBottom: 12 }}>
      {/* 工具列表头部 */}
      <div
        style={{
          background: '#f5f5f5',
          border: '1px solid #d9d9d9',
          borderRadius: 12,
          padding: '12px 16px',
          marginBottom: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}
      >
        <Space>
          <ToolOutlined style={{ color: '#95a5a6', fontSize: 16 }} />
          <Text strong style={{ color: '#5d6d7e', fontSize: 14 }}>
            工具调用
          </Text>
          <Tag style={{ fontSize: 11 }}>{tools.length} 个</Tag>
        </Space>
        <Space>
          {tools.some(t => t.status === 'running') && (
            <Tag color="processing" icon={<LoadingOutlined spin />}>
              执行中
            </Tag>
          )}
        </Space>
      </div>

      {/* 工具列表 */}
      {tools.map((tool, index) => {
        const isExpanded = expandedTools.has(tool.id);
        const hasError = tool.status === 'error' && tool.error;

        return (
          <Card
            key={tool.id}
            size="small"
            style={{
              marginBottom: 8,
              border: hasError ? '1px solid #ff4d4f' : '1px solid #e8e8e8',
              borderRadius: 8,
              background: hasError ? '#fff2f0' : '#fafafa'
            }}
            bodyStyle={{ padding: 12 }}
          >
            {/* 工具头部 */}
            <div
              onClick={() => toggleTool(tool.id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                cursor: 'pointer',
                userSelect: 'none'
              }}
            >
              <Space>
                {getStatusIcon(tool.status)}
                <Text strong style={{ fontSize: 13 }}>
                  {tool.name}
                </Text>
                {getStatusTag(tool.status)}
                {tool.duration && (
                  <Tag icon={<ClockCircleOutlined />} style={{ fontSize: 11 }}>
                    {formatDuration(tool.duration)}
                  </Tag>
                )}
              </Space>
              <Space>
                {isExpanded ? (
                  <DownOutlined style={{ color: '#95a5a6', fontSize: 12 }} />
                ) : (
                  <RightOutlined style={{ color: '#95a5a6', fontSize: 12 }} />
                )}
              </Space>
            </div>

            {/* 工具详情 */}
            {isExpanded && (
              <div style={{ marginTop: 12 }}>
                {/* 参数和操作按钮 */}
                <Row gutter={[8, 8]} style={{ marginBottom: 12 }}>
                  <Col>
                    <Button
                      size="small"
                      icon={<EyeOutlined />}
                      onClick={() => handleViewParams(tool)}
                    >
                      查看参数
                    </Button>
                  </Col>
                  {tool.status === 'error' && onRetry && (
                    <Col>
                      <Button
                        size="small"
                        type="primary"
                        icon={<PlayCircleOutlined />}
                        danger
                        onClick={() => handleRetry(tool.id)}
                      >
                        重新执行
                      </Button>
                    </Col>
                  )}
                </Row>

                {/* 执行结果 */}
                {tool.result && (
                  <div
                    style={{
                      background: '#fff',
                      padding: 12,
                      borderRadius: 6,
                      border: '1px solid #e8e8e8',
                      fontSize: 12,
                      color: '#5d6d7e',
                      whiteSpace: 'pre-wrap',
                      maxHeight: 200,
                      overflow: 'auto'
                    }}
                  >
                    <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 4 }}>
                      执行结果:
                    </Text>
                    {tool.result}
                  </div>
                )}

                {/* 错误信息 */}
                {hasError && (
                  <div
                    style={{
                      background: '#fff2f0',
                      padding: 12,
                      borderRadius: 6,
                      border: '1px solid #ffccc7',
                      fontSize: 12,
                      color: '#ff4d4f',
                      marginTop: 8
                    }}
                  >
                    <Text type="danger" style={{ fontSize: 11, display: 'block', marginBottom: 4 }}>
                      错误信息:
                    </Text>
                    {tool.error}
                  </div>
                )}
              </div>
            )}
          </Card>
        );
      })}

      {/* 参数查看弹窗 */}
      <Modal
        title="工具参数"
        open={paramsModalVisible}
        onCancel={() => setParamsModalVisible(false)}
        footer={null}
        width={600}
      >
        {selectedTool && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="工具名称">
              {selectedTool.name}
            </Descriptions.Item>
            <Descriptions.Item label="工具ID">
              <Text copyable>{selectedTool.id}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="执行状态">
              {getStatusTag(selectedTool.status)}
            </Descriptions.Item>
            {selectedTool.duration && (
              <Descriptions.Item label="执行耗时">
                {formatDuration(selectedTool.duration)}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="参数">
              <pre
                style={{
                  background: '#f5f5f5',
                  padding: 12,
                  borderRadius: 4,
                  fontSize: 12,
                  overflow: 'auto',
                  maxHeight: 300
                }}
              >
                {JSON.stringify(selectedTool.params, null, 2)}
              </pre>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default React.memo(ToolComponent);
