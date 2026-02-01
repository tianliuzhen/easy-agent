import React, { useState, useEffect, useRef } from 'react';
import { sendMessage } from '..//api/ChatApi';
import { useLocation } from 'react-router-dom';
import { SendOutlined, RobotOutlined, UserOutlined, LoadingOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { Button, Input, Spin } from 'antd';

const { TextArea } = Input;

interface ChatMessage {
  text: string;
  isUser: boolean;
  type: 'data' | 'log';
  id: string; // 添加唯一ID
}

const ChatDemo: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    { text: "您好！我是EasyAgent智能助手，有什么我可以帮您的吗？", isUser: false, type: 'data', id: crypto.randomUUID() }
  ]);
  const [thinkingLog, setThinkingLog] = useState<string>(''); // 思考过程
  const [input, setInput] = useState('');
  const [isThinking, setIsThinking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showThinking, setShowThinking] = useState(false); // 是否显示思考过程
  const eventSourceRef = useRef<EventSource | null>(null);
  const chatMessagesEndRef = useRef<HTMLDivElement>(null);
  const [uuid, setUuid] = useState('');
  const location = useLocation();
  // 用于跟踪当前正在回答的消息ID
  const currentAnsweringMsgIdRef = useRef<string | null>(null);
  // 用于存储每个AI消息的思考过程
  const [messageThinkingLogs, setMessageThinkingLogs] = useState<Record<string, string>>({});

  useEffect(() => {
    chatMessagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, thinkingLog, isThinking]);

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  useEffect(() => {
    const newUuid = crypto.randomUUID();
    setUuid(newUuid);
  }, []);

  const handleSendMessage = async () => {
    if (input.trim() === '') return;

    setError(null);
    // 添加用户消息
    const userMessageId = crypto.randomUUID();
    setMessages(prev => [...prev, {
      text: input,
      isUser: true,
      type: 'data',
      id: userMessageId
    }]);
    setInput('');
    setIsThinking(true);
    setShowThinking(true); // 默认显示思考过程

    // 为即将到来的AI回答创建消息ID
    const aiMessageId = crypto.randomUUID();

    // 设置当前正在回答的消息ID
    currentAnsweringMsgIdRef.current = aiMessageId;

    // 为该AI消息初始化思考过程
    setMessageThinkingLogs(prev => ({
      ...prev,
      [aiMessageId]: ""
    }));

    // 清空全局thinkingLog，现在使用基于消息的思考过程
    setThinkingLog('');

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const urlParams = new URLSearchParams(location.search);
    const agentId = urlParams.get('agentId');

    const eventSource = sendMessage(
        input,
        uuid,
        (log: string) => {
          // 更新对应消息的思考过程
          const currentAiMessageId = currentAnsweringMsgIdRef.current;
          if (currentAiMessageId) {
            setMessageThinkingLogs(prev => {
              const currentLog = prev[currentAiMessageId] || "";
              return {
                ...prev,
                [currentAiMessageId]: currentLog ? currentLog + '\n' + log : log
              };
            });
          }
        },
        (data: string) => {
          // 接收到data数据时，添加或更新AI消息内容
          const currentAiMessageId = currentAnsweringMsgIdRef.current;
          if (currentAiMessageId) {
            setMessages(prev => {
              // 检查是否已经存在该AI消息
              const existingMsgIndex = prev.findIndex(msg => msg.id === currentAiMessageId);

              if (existingMsgIndex >= 0) {
                // 如果消息已存在，更新它
                return prev.map((msg, index) => {
                  if (index === existingMsgIndex) {
                    return {
                      ...msg,
                      text: msg.text + data
                    };
                  }
                  return msg;
                });
              } else {
                // 如果消息不存在，添加新的AI消息
                return [...prev, {
                  text: data,
                  isUser: false,
                  type: 'data',
                  id: currentAiMessageId
                }];
              }
            });
          }
        },
        () => {
          // 完成回调
          setIsThinking(false);
          currentAnsweringMsgIdRef.current = null;
        },
        (errorMessage: string) => {
          // 错误处理
          const currentAiMessageId = currentAnsweringMsgIdRef.current;
          if (currentAiMessageId) {
            setMessageThinkingLogs(prev => {
              const currentLog = prev[currentAiMessageId] || "";
              return {
                ...prev,
                [currentAiMessageId]: currentLog + '\n' + `思考过程中出现错误: ${errorMessage}`
              };
            });
          }
          setError(errorMessage);
          setIsThinking(false);
          currentAnsweringMsgIdRef.current = null;
        }
    );

    eventSourceRef.current = eventSource;
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
      <div style={{
        maxWidth: '800px',
        margin: '0 auto',
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        background: '#d6e6f5',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
        paddingBottom: '20px'
      }}>
        {/* 顶部标题栏 */}
        <div style={{
          background: 'rgba(255, 255, 255, 0.1)',
          backdropFilter: 'blur(10px)',
          padding: '16px 24px',
          borderBottom: '1px solid rgba(255, 255, 255, 0.2)',
          display: 'flex',
          alignItems: 'center',
          flexShrink: 0,
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)'
        }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginRight: '16px',
            boxShadow: '0 4px 15px rgba(0, 0, 0, 0.2)'
          }}>
            <RobotOutlined style={{ color: '#667eea', fontSize: '20px' }} />
          </div>
          <div>
            <div style={{ fontSize: '18px', fontWeight: 600, color: 'white', textShadow: '0 2px 4px rgba(0,0,0,0.3)' }}>EasyAgent智能助手</div>
            <div style={{ fontSize: '12px', color: 'rgba(255, 255, 255, 0.8)' }}>在线 · 随时为您服务</div>
          </div>
        </div>

        {/* 聊天消息区域 */}
        <div style={{
          flex: 1,
          overflowY: 'auto',
          padding: '24px',
          background: 'rgba(255, 255, 255, 0.1)',
          backdropFilter: 'blur(10px)'
        }}>
          {/* 先渲染所有历史消息 */}
          {messages.map((msg, index) => {
            // 获取该消息的思考过程
            const msgThinkingLog = messageThinkingLogs[msg.id] || '';
            const hasThinking = msgThinkingLog.length > 0;

            return (
                <React.Fragment key={`msg-${msg.id}`}>
                  {/* 如果是AI消息且有思考过程，在AI消息之前显示思考过程 */}
                  {!msg.isUser && hasThinking && (
                      <div style={{ marginBottom: '8px', marginLeft: '48px' }}>
                        {/* 思考过程折叠部分 */}
                        <div
                            style={{
                              background: '#e6f0ff',
                              borderRadius: '12px',
                              padding: '12px',
                              marginBottom: '12px',
                              cursor: 'pointer',
                              border: '1px solid #b3d1ff',
                              boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                              maxWidth: showThinking ? '70%' : 'fit-content',
                              width: showThinking ? 'fit-content' : 'auto',
                              minWidth: showThinking ? '10%' : '10%'
                            }}
                            onClick={() => setShowThinking(!showThinking)}
                        >
                          <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            fontSize: '12px',
                            color: '#666'
                          }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <span>思考过程</span>
                              <span style={{
                                fontSize: '12px',
                                color: '#666',
                                backgroundColor: '#cce0ff',
                                padding: '2px 6px',
                                borderRadius: '10px',
                                display: 'inline-block',
                                width: 'fit-content'
                              }}>
                            已完成
                          </span>
                            </div>
                            <div onClick={(e) => {
                              e.stopPropagation();
                              setShowThinking(!showThinking);
                            }} style={{ cursor: 'pointer' }}>
                              {showThinking ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                            </div>
                          </div>

                          {showThinking && msgThinkingLog && (
                              <div style={{
                                marginTop: '12px',
                                padding: '12px',
                                background: '#e6f0ff',
                                borderRadius: '8px',
                                border: '1px solid #b3d1ff',
                                fontSize: '12px',
                                lineHeight: 1.6,
                                color: '#666',
                                whiteSpace: 'pre-wrap',
                                maxHeight: '200px',
                                overflowY: 'auto'
                              }}>
                                {msgThinkingLog}
                              </div>
                          )}
                        </div>
                      </div>
                  )}

                  {/* 渲染消息本身 */}
                  <div
                      style={{
                        display: 'flex',
                        justifyContent: msg.isUser ? 'flex-end' : 'flex-start',
                        marginBottom: '20px',
                        animation: 'fadeIn 0.3s ease'
                      }}
                  >
                    {!msg.isUser && (
                        <div style={{
                          width: '40px',
                          height: '40px',
                          borderRadius: '50%',
                          background: 'linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                          marginRight: '16px',
                          marginTop: '4px',
                          boxShadow: '0 4px 15px rgba(0, 0, 0, 0.2)'
                        }}>
                          <RobotOutlined style={{ color: '#667eea', fontSize: '18px' }} />
                        </div>
                    )}

                    <div
                        style={{
                          backgroundColor: msg.isUser ? 'rgba(255, 255, 255, 0.9)' : 'rgba(255, 255, 255, 0.9)',
                          color: msg.isUser ? '#333' : '#333',
                          padding: '14px 18px',
                          borderRadius: '20px',
                          maxWidth: '70%',
                          width: 'fit-content',
                          minWidth: '10%',
                          wordBreak: 'break-word',
                          lineHeight: 1.6,
                          fontSize: '15px',
                          position: 'relative',
                          boxShadow: '0 4px 15px rgba(0, 0, 0, 0.1)',
                          border: '1px solid rgba(255, 255, 255, 0.2)',
                          backdropFilter: 'blur(5px)',
                          transition: 'all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1)',
                          transform: 'translateY(0)'
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.transform = 'translateY(-4px) scale(1.02)';
                          e.currentTarget.style.boxShadow = '0 8px 25px rgba(0, 0, 0, 0.15)';
                          e.currentTarget.style.background = 'rgba(255, 255, 255, 0.95)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.transform = 'translateY(0) scale(1)';
                          e.currentTarget.style.boxShadow = '0 4px 15px rgba(0, 0, 0, 0.1)';
                          e.currentTarget.style.background = 'rgba(255, 255, 255, 0.9)';
                        }}
                    >
                      <div style={{ whiteSpace: 'pre-wrap' }}>
                        {msg.text.replace(/^\n+|\n+$/g, '')}
                      </div>
                      {msg.isUser && (
                          <div style={{
                            position: 'absolute',
                            right: '-8px',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            width: 0,
                            height: 0,
                            borderTop: '8px solid transparent',
                            borderBottom: '8px solid transparent',
                            borderLeft: '8px solid rgba(255, 255, 255, 0.9)'
                          }} />
                      )}
                      {!msg.isUser && (
                          <div style={{
                            position: 'absolute',
                            left: '-8px',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            width: 0,
                            height: 0,
                            borderTop: '8px solid transparent',
                            borderBottom: '8px solid transparent',
                            borderRight: '8px solid rgba(255, 255, 255, 0.9)'
                          }} />
                      )}
                    </div>

                    {msg.isUser && (
                        <div style={{
                          width: '40px',
                          height: '40px',
                          borderRadius: '50%',
                          background: 'linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                          marginLeft: '16px',
                          marginTop: '4px',
                          boxShadow: '0 4px 15px rgba(0, 0, 0, 0.2)'
                        }}>
                          <UserOutlined style={{ color: '#667eea', fontSize: '18px' }} />
                        </div>
                    )}
                  </div>
                </React.Fragment>
            );
          })}

          {/* 在历史消息之后，显示当前正在思考的内容 */}
          {isThinking && currentAnsweringMsgIdRef.current && (
              <div style={{ marginBottom: '8px', marginLeft: '48px' }}>
                {/* 当前正在思考的过程 */}
                <div
                    style={{
                      background: '#e6f0ff',
                      borderRadius: '12px',
                      padding: '12px',
                      marginBottom: '12px',
                      cursor: 'pointer',
                      border: '1px solid #b3d1ff',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                      maxWidth: showThinking ? '70%' : 'fit-content',
                      width: showThinking ? 'fit-content' : 'auto',
                      minWidth: showThinking ? '10%' : '10%'
                    }}
                    onClick={() => setShowThinking(!showThinking)}
                >
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontSize: '12px',
                    color: '#666'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Spin
                          indicator={<LoadingOutlined style={{ fontSize: 14, color: '#1890ff' }} spin />}
                      />
                      <span>AI正在思考中...</span>
                      <span style={{
                        fontSize: '12px',
                        color: '#666',
                        backgroundColor: '#cce0ff',
                        padding: '2px 6px',
                        borderRadius: '10px',
                        display: 'inline-block',
                        width: 'fit-content'
                      }}>
                      思考中...
                    </span>
                    </div>
                    <div onClick={(e) => {
                      e.stopPropagation();
                      setShowThinking(!showThinking);
                    }} style={{ cursor: 'pointer' }}>
                      {showThinking ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                    </div>
                  </div>

                  {showThinking && messageThinkingLogs[currentAnsweringMsgIdRef.current] && (
                      <div style={{
                        marginTop: '12px',
                        padding: '12px',
                        background: '#e6f0ff',
                        borderRadius: '8px',
                        border: '1px solid #b3d1ff',
                        fontSize: '12px',
                        lineHeight: 1.6,
                        color: '#666',
                        whiteSpace: 'pre-wrap',
                        maxHeight: '200px',
                        overflowY: 'auto'
                      }}>
                        {messageThinkingLogs[currentAnsweringMsgIdRef.current]}
                      </div>
                  )}
                </div>

                {/* AI思考中的占位气泡 */}
                <div style={{
                  display: 'flex',
                  justifyContent: 'flex-start',
                  marginBottom: '12px',
                  opacity: 0.6
                }}>
                  <div style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    marginRight: '16px',
                    marginTop: '4px',
                    boxShadow: '0 4px 15px rgba(0, 0, 0, 0.2)'
                  }}>
                    <RobotOutlined style={{ color: '#667eea', fontSize: '18px' }} />
                  </div>
                  <div
                      style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.9)',
                        color: '#666',
                        padding: '14px 18px',
                        borderRadius: '20px',
                        maxWidth: '70%',
                        width: 'fit-content',
                        minWidth: '10%',
                        wordBreak: 'break-word',
                        lineHeight: 1.6,
                        fontSize: '15px',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        backdropFilter: 'blur(5px)',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        position: 'relative'
                      }}
                  >
                    <Spin size="small" />
                    <span>正在生成回答...</span>
                    <div style={{
                      position: 'absolute',
                      left: '-8px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      width: 0,
                      height: 0,
                      borderTop: '8px solid transparent',
                      borderBottom: '8px solid transparent',
                      borderRight: '8px solid rgba(255, 255, 255, 0.9)'
                    }} />
                  </div>
                </div>
              </div>
          )}

          <div ref={chatMessagesEndRef} />
        </div>

        {/* 错误提示 */}
        {error && (
            <div style={{
              background: '#ffe6e6',
              color: '#ff4d4f',
              padding: '8px 20px',
              fontSize: '14px',
              textAlign: 'center',
              flexShrink: 0,
              borderTop: '1px solid #ffccc7'
            }}>
              {error}
            </div>
        )}

        {/* 输入区域 */}
        <div style={{
          background: 'rgba(255, 255, 255, 0.1)',
          backdropFilter: 'blur(10px)',
          padding: '16px 24px',
          borderTop: '1px solid rgba(255, 255, 255, 0.2)',
          flexShrink: 0,
          marginBottom: '16px',
          borderRadius: '12px',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)'
        }}>
          <div style={{
            display: 'flex',
            gap: '12px',
            alignItems: 'flex-end',
            padding: '4px 0'
          }}>
            <TextArea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="输入消息..."
                autoSize={{ minRows: 1, maxRows: 4 }}
                style={{
                  flex: 1,
                  borderRadius: '24px',
                  border: '1px solid rgba(255, 255, 255, 0.3)',
                  fontSize: '15px',
                  padding: '14px 18px',
                  resize: 'none',
                  background: 'rgba(255, 255, 255, 0.9)',
                  boxShadow: '0 2px 10px rgba(0, 0, 0, 0.1)',
                  transition: 'all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1)'
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.8)';
                  e.currentTarget.style.boxShadow = '0 6px 20px rgba(0, 0, 0, 0.15)';
                  e.currentTarget.style.transform = 'translateY(-1px)';
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.95)';
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.3)';
                  e.currentTarget.style.boxShadow = '0 2px 10px rgba(0, 0, 0, 0.1)';
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.9)';
                }}
            />
            <Button
                type="primary"
                onClick={handleSendMessage}
                disabled={isThinking || input.trim() === ''}
                style={{
                  height: '44px',
                  borderRadius: '22px',
                  padding: '0 24px',
                  background: 'linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.9) 100%)',
                  border: '1px solid rgba(255, 255, 255, 0.3)',
                  color: '#667eea',
                  fontWeight: 600,
                  boxShadow: '0 4px 15px rgba(0, 0, 0, 0.1)',
                  transition: 'all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-3px) scale(1.05)';
                  e.currentTarget.style.boxShadow = '0 8px 25px rgba(0, 0, 0, 0.2)';
                  e.currentTarget.style.background = 'linear-gradient(135deg, #f8f9ff 0%, #ffffff 100%)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0) scale(1)';
                  e.currentTarget.style.boxShadow = '0 4px 15px rgba(0, 0, 0, 0.1)';
                  e.currentTarget.style.background = 'linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.9) 100%)';
                }}
                icon={<SendOutlined style={{ fontSize: '16px', color: '#667eea' }} />}
            >
              发送
            </Button>
          </div>
        </div>

        <style>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        
        @keyframes pulse {
          0% {
            box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.4);
          }
          70% {
            box-shadow: 0 0 0 10px rgba(102, 126, 234, 0);
          }
          100% {
            box-shadow: 0 0 0 0 rgba(102, 126, 234, 0);
          }
        }
        
        ::-webkit-scrollbar {
          width: 8px;
        }
        
        ::-webkit-scrollbar-track {
          background: rgba(255, 255, 255, 0.1);
          border-radius: 4px;
        }
        
        ::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.3);
          border-radius: 4px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
          background: rgba(255, 255, 255, 0.5);
        }
      `}</style>
      </div>
  );
};

export default ChatDemo;