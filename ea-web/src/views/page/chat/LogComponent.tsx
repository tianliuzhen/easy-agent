import React, { useState } from 'react';
import { Collapse, Typography, Space, Tag, Button, List, Badge } from 'antd';
import {
  DownOutlined,
  RightOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import type { LogEntry } from './types';

const { Text } = Typography;

interface LogComponentProps {
  logs?: LogEntry[];
  defaultExpanded?: boolean;
  maxDisplayCount?: number;
  onClear?: () => void;
}

/**
 * 日志展示组件
 * 支持不同级别的日志展示、折叠、清空
 */
const LogComponent: React.FC<LogComponentProps> = ({
  logs,
  defaultExpanded = false,
  maxDisplayCount = 50,
  onClear
}) => {
  const [expanded, setExpanded] = useState(defaultExpanded);

  if (!logs || logs.length === 0) {
    return null;
  }

  // 只显示最近的日志
  const displayLogs = logs.slice(-maxDisplayCount);
  const hasMore = logs.length > maxDisplayCount;

  const getLevelIcon = (level: LogEntry['level']) => {
    switch (level) {
      case 'info':
        return <InfoCircleOutlined style={{ color: '#1890ff' }} />;
      case 'warn':
        return <WarningOutlined style={{ color: '#faad14' }} />;
      case 'error':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
      default:
        return <InfoCircleOutlined style={{ color: '#1890ff' }} />;
    }
  };

  const getLevelColor = (level: LogEntry['level']) => {
    switch (level) {
      case 'info':
        return 'blue';
      case 'warn':
        return 'orange';
      case 'error':
        return 'red';
      default:
        return 'blue';
    }
  };

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  };

  const errorCount = logs.filter(l => l.level === 'error').length;
  const warnCount = logs.filter(l => l.level === 'warn').length;

  return (
    <div
      style={{
        background: '#fafafa',
        border: '1px solid #e8e8e8',
        borderRadius: 12,
        padding: '12px 16px',
        marginBottom: 12
      }}
    >
      {/* 头部 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          userSelect: 'none'
        }}
        onClick={() => setExpanded(!expanded)}
      >
        <Space>
          <FileTextOutlined style={{ color: '#78909c', fontSize: 16 }} />
          <Text strong style={{ color: '#78909c', fontSize: 14 }}>
            日志
          </Text>
          <Badge count={logs.length} style={{ backgroundColor: '#78909c' }} />
          {errorCount > 0 && (
            <Badge count={errorCount} style={{ backgroundColor: '#ff4d4f' }} />
          )}
          {warnCount > 0 && (
            <Badge count={warnCount} style={{ backgroundColor: '#faad14' }} />
          )}
        </Space>
        <Space>
          {onClear && (
            <Button
              size="small"
              type="text"
              icon={<DeleteOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                onClear();
              }}
            >
              清空
            </Button>
          )}
          {expanded ? (
            <DownOutlined style={{ color: '#78909c', fontSize: 12 }} />
          ) : (
            <RightOutlined style={{ color: '#78909c', fontSize: 12 }} />
          )}
        </Space>
      </div>

      {/* 日志列表 */}
      {expanded && (
        <div style={{ marginTop: 12 }}>
          {hasMore && (
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 8 }}>
              显示最近 {maxDisplayCount} 条日志，共 {logs.length} 条
            </Text>
          )}
          <List
            size="small"
            dataSource={displayLogs}
            renderItem={(log) => (
              <List.Item
                style={{
                  padding: '4px 0',
                  borderBottom: '1px solid #f0f0f0'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', width: '100%' }}>
                  <span style={{ marginRight: 8, marginTop: 2 }}>
                    {getLevelIcon(log.level)}
                  </span>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                      <Tag color={getLevelColor(log.level)} style={{ fontSize: 10, lineHeight: '16px' }}>
                        {log.level.toUpperCase()}
                      </Tag>
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        {formatTime(log.timestamp)}
                      </Text>
                    </div>
                    <Text style={{ fontSize: 12, color: '#555' }}>
                      {log.content}
                    </Text>
                  </div>
                </div>
              </List.Item>
            )}
            style={{
              maxHeight: 300,
              overflow: 'auto',
              background: '#fff',
              borderRadius: 6,
              padding: '0 8px'
            }}
          />
        </div>
      )}
    </div>
  );
};

export default React.memo(LogComponent);
