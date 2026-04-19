import React from 'react';
import { Bubble } from '@ant-design/x';
import XMarkdown from '@ant-design/x-markdown';
import { Typography, Space, Tag } from 'antd';
import {
  RobotOutlined,
  UserOutlined,
  CheckCircleOutlined,
  LoadingOutlined,
  CloseCircleOutlined
} from '@ant-design/icons';
import ThinkComponent from './ThinkComponent';
import ToolComponent from './ToolComponent';
import LogComponent from './LogComponent';
import type { ChatMessage, ToolCall, LogEntry, ThinkingProcess } from './types';

const { Text } = Typography;

// XMarkdown 自定义组件配置
export const xMarkdownComponents = {
  // 思考过程组件
  think: ({ children, status }: { children: string; status?: string }) => {
    const thinking: ThinkingProcess = {
      content: children,
      status: (status as any) || 'done'
    };
    return <ThinkComponent thinking={thinking} />;
  },

  // 工具组件
  tool: ({ children, name, status, id }: { children: string; name?: string; status?: string; id?: string }) => {
    const tool: ToolCall = {
      id: id || crypto.randomUUID(),
      name: name || '未知工具',
      params: {},
      result: children,
      status: (status as any) || 'success'
    };
    return <ToolComponent tools={[tool]} />;
  },

  // 日志组件
  log: ({ children, level }: { children: string; level?: string }) => {
    const log: LogEntry = {
      id: crypto.randomUUID(),
      content: children,
      timestamp: Date.now(),
      level: (level as any) || 'info'
    };
    return <LogComponent logs={[log]} />;
  }
};

interface MessageBubbleProps {
  message: ChatMessage;
  isCurrentAnswering?: boolean;
  onRetryTool?: (toolId: string) => void;
}

/**
 * 消息气泡组件
 * 使用 Bubble + XMarkdown 组合渲染消息
 */
export const MessageBubble: React.FC<MessageBubbleProps> = ({
  message,
  isCurrentAnswering = false,
  onRetryTool
}) => {
  // 构建 XMarkdown 内容
  const buildMarkdownContent = () => {
    const parts: string[] = [];

    // 添加思考过程
    if (message.thinking?.content) {
      parts.push(`<think status="${message.thinking.status}">${message.thinking.content}</think>`);
    }

    // 添加工具调用
    if (message.tools && message.tools.length > 0) {
      message.tools.forEach(tool => {
        parts.push(`<tool id="${tool.id}" name="${tool.name}" status="${tool.status}">${tool.result || ''}</tool>`);
      });
    }

    // 添加日志
    if (message.logs && message.logs.length > 0) {
      message.logs.forEach(log => {
        parts.push(`<log level="${log.level}">${log.content}</log>`);
      });
    }

    // 添加最终答案
    if (message.text) {
      parts.push(message.text);
    }

    return parts.join('\n\n');
  };

  // 获取状态图标
  const getStatusIcon = () => {
    if (isCurrentAnswering) {
      return <LoadingOutlined spin style={{ color: '#fa8c16' }} />;
    }
    switch (message.streamStatus) {
      case 'streaming':
        return <LoadingOutlined spin style={{ color: '#fa8c16' }} />;
      case 'done':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'error':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
      default:
        return null;
    }
  };

  // 头像渲染
  const avatarRender = () => {
    if (message.isUser) {
      return (
        <div style={{
          width: 40,
          height: 40,
          borderRadius: 12,
          background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
        }}>
          <UserOutlined style={{ color: 'white', fontSize: 18 }} />
        </div>
      );
    }
    return (
      <div style={{
        width: 40,
        height: 40,
        borderRadius: 12,
        background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
      }}>
        <RobotOutlined style={{ color: 'white', fontSize: 18 }} />
      </div>
    );
  };

  // 如果消息正在流式传输且没有内容，显示思考指示器
  if (!message.isUser && message.streamStatus === 'streaming' && !message.text && !message.thinking?.content) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'flex-start',
        marginBottom: 24,
        animation: 'fadeIn 0.3s ease'
      }}>
        {avatarRender()}
        <div style={{
          marginLeft: 16,
          background: 'var(--ea-theme-background)',
          padding: '16px 20px',
          borderRadius: 18,
          boxShadow: '0 4px 15px rgba(0, 0, 0, 0.08)',
          display: 'flex',
          alignItems: 'center',
          gap: 8
        }}>
          <LoadingOutlined spin style={{ color: '#fa8c16' }} />
          <Text style={{ color: '#666' }}>思考中...</Text>
        </div>
      </div>
    );
  }

  // 构建消息内容
  const hasMetaContent = message.thinking?.content || (message.tools && message.tools.length > 0) || (message.logs && message.logs.length > 0);

  return (
    <div style={{
      display: 'flex',
      justifyContent: message.isUser ? 'flex-end' : 'flex-start',
      marginBottom: 24,
      animation: 'fadeIn 0.3s ease'
    }}>
      {!message.isUser && (
        <div style={{ marginRight: 16, marginTop: 4 }}>
          {avatarRender()}
        </div>
      )}

      <div style={{ maxWidth: '70%', minWidth: 120 }}>
        {/* 思考过程、工具、日志 */}
        {hasMetaContent && (
          <div style={{ marginBottom: 12 }}>
            {message.thinking?.content && (
              <ThinkComponent thinking={message.thinking} />
            )}
            {message.tools && message.tools.length > 0 && (
              <ToolComponent
                tools={message.tools}
                onRetry={onRetryTool}
              />
            )}
            {message.logs && message.logs.length > 0 && (
              <LogComponent logs={message.logs} />
            )}
          </div>
        )}

        {/* 消息气泡 */}
        {message.text && (
          <div
            style={{
              backgroundColor: message.isUser ? 'var(--ea-theme-background)' : 'var(--ea-theme-background)',
              color: '#000000',
              padding: '16px 20px',
              borderRadius: 18,
              wordBreak: 'break-word',
              lineHeight: 1.6,
              fontSize: 14,
              boxShadow: message.isUser
                ? '0 4px 15px rgba(102, 126, 234, 0.3)'
                : '0 4px 15px rgba(0, 0, 0, 0.08)',
              border: `1px solid ${message.isUser ? 'rgba(255, 255, 255, 0.2)' : '#f0f0f0'}`,
              position: 'relative'
            }}
          >
            {/* 状态指示器 */}
            {!message.isUser && getStatusIcon() && (
              <span style={{
                position: 'absolute',
                top: -8,
                right: -8,
                background: 'white',
                borderRadius: '50%',
                padding: 2,
                boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
              }}>
                {getStatusIcon()}
              </span>
            )}
            <div style={{ whiteSpace: 'pre-wrap' }}>
              {message.text}
            </div>
          </div>
        )}
      </div>

      {message.isUser && (
        <div style={{ marginLeft: 16, marginTop: 4 }}>
          {avatarRender()}
        </div>
      )}
    </div>
  );
};

/**
 * 使用 XMarkdown 渲染消息内容
 */
export const MessageBubbleWithXMarkdown: React.FC<MessageBubbleProps> = ({
  message,
  isCurrentAnswering = false
}) => {
  // 头像渲染
  const avatarRender = () => {
    if (message.isUser) {
      return (
        <div style={{
          width: 40,
          height: 40,
          borderRadius: 12,
          background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
        }}>
          <UserOutlined style={{ color: 'white', fontSize: 18 }} />
        </div>
      );
    }
    return (
      <div style={{
        width: 40,
        height: 40,
        borderRadius: 12,
        background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
      }}>
        <RobotOutlined style={{ color: 'white', fontSize: 18 }} />
      </div>
    );
  };

  // 构建 XMarkdown 内容
  const buildContent = () => {
    const parts: string[] = [];

    if (message.thinking?.content) {
      parts.push(`<think status="${message.thinking.status}">${message.thinking.content}</think>`);
    }

    if (message.tools && message.tools.length > 0) {
      message.tools.forEach(tool => {
        parts.push(`<tool id="${tool.id}" name="${tool.name}" status="${tool.status}">${tool.result || ''}</tool>`);
      });
    }

    if (message.logs && message.logs.length > 0) {
      message.logs.forEach(log => {
        parts.push(`<log level="${log.level}">${log.content}</log>`);
      });
    }

    if (message.text) {
      parts.push(message.text);
    }

    return parts.join('\n\n');
  };

  return (
    <Bubble
      content={buildContent()}
      avatar={avatarRender()}
      placement={message.isUser ? 'end' : 'start'}
      loading={isCurrentAnswering && !message.text}
      messageRender={(content) => (
        <XMarkdown
          components={xMarkdownComponents}
          paragraphTag="div"
        >
          {content}
        </XMarkdown>
      )}
      variant={message.isUser ? 'shadow' : 'filled'}
      style={{
        maxWidth: '70%'
      }}
    />
  );
};

export default MessageBubble;
