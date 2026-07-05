import React from 'react';
import {Button, Card, Spin, Tooltip} from 'antd';
import {
    RobotOutlined,
    MessageOutlined,
    ClockCircleOutlined,
    DeleteOutlined,
    PlusCircleOutlined,
} from '@ant-design/icons';
import type {ChatConversation} from '../../api/ChatConversationApi';

export interface ConversationSidebarProps {
    conversations: ChatConversation[];
    selectedChatIndex: number;
    loading: boolean;
    displayName: string;
    displayDesc?: string;
    onSelect: (index: number) => void;
    onDelete: (index: number) => void;
    onNewChat: () => void;
}

const ACCENT_GRADIENT = 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)';

const getChatTitle = (conversation: ChatConversation, truncate = true): string => {
    const title = conversation.title || '新对话';
    if (!truncate) return title;
    return title.substring(0, 30) + (title.length > 30 ? '...' : '');
};

export const ConversationSidebar: React.FC<ConversationSidebarProps> = ({
    conversations,
    selectedChatIndex,
    loading,
    displayName,
    displayDesc,
    onSelect,
    onDelete,
    onNewChat,
}) => {
    return (
        <div
            style={{
                width: '300px',
                background: 'var(--ea-theme-background)',
                borderRight: '1px solid #d9e6f2',
                display: 'flex',
                flexDirection: 'column',
                padding: '24px 0',
                minWidth: 0,
            }}
        >
            <div style={{padding: '0 16px 20px', borderBottom: '1px solid #d9e6f2'}}>
                <div style={{display: 'flex', alignItems: 'center', marginBottom: '20px'}}>
                    <div
                        style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: '12px',
                            background: ACCENT_GRADIENT,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            marginRight: '12px',
                            boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)',
                        }}
                    >
                        <RobotOutlined style={{color: 'white', fontSize: '20px'}}/>
                    </div>
                    <div>
                        <div style={{fontSize: '16px', fontWeight: 600, color: '#333', marginBottom: '4px'}}>
                            {displayName}
                        </div>
                        <div style={{fontSize: '12px', color: '#666'}}>{displayDesc}</div>
                    </div>
                </div>

                <Button
                    type="primary"
                    onClick={onNewChat}
                    style={{
                        width: '100%',
                        height: '44px',
                        background: ACCENT_GRADIENT,
                        border: 'none',
                        borderRadius: '12px',
                        color: 'white',
                        fontWeight: 500,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                    }}
                >
                    <PlusCircleOutlined style={{marginRight: '-2px'}}/>
                    开始新对话
                </Button>
            </div>

            <div style={{flex: 1, overflowY: 'auto', padding: '20px 12px', minWidth: 0, position: 'relative'}}>
                <Tooltip title="聊天历史">
                    <div
                        style={{
                            fontSize: '12px',
                            color: '#999',
                            fontWeight: 500,
                            marginBottom: '12px',
                            paddingLeft: '8px',
                        }}
                    >
                        聊天历史
                    </div>
                </Tooltip>

                {loading ? (
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: '20px',
                            color: '#999',
                        }}
                    >
                        <Spin size="small"/> 加载会话中...
                    </div>
                ) : conversations.length === 0 ? (
                    <div style={{textAlign: 'center', padding: '40px 20px', color: '#999'}}>
                        <MessageOutlined style={{fontSize: '48px', marginBottom: '16px', opacity: 0.3}}/>
                        <div style={{fontSize: '14px'}}>暂无会话记录</div>
                    </div>
                ) : (
                    conversations.map((conversation, index) => {
                        const selected = selectedChatIndex === index;
                        return (
                            <Card
                                key={conversation.id}
                                onClick={() => onSelect(index)}
                                style={{
                                    marginBottom: '12px',
                                    background: selected ? ACCENT_GRADIENT : 'var(--ea-theme-background)',
                                    border: selected ? '1px solid #5c74a8' : '1px solid #d9e6f2',
                                    borderRadius: '12px',
                                    cursor: 'pointer',
                                    color: selected ? 'white' : '#333',
                                }}
                                styles={{
                                    body: {
                                        padding: '12px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                    },
                                }}
                            >
                                <div style={{display: 'flex', alignItems: 'center', flex: 1}}>
                                    <div style={{flex: 1, minWidth: 0}}>
                                        <Tooltip title={getChatTitle(conversation, false)}>
                                            <div
                                                style={{
                                                    fontSize: '14px',
                                                    color: selected ? 'white' : '#333',
                                                    marginBottom: '4px',
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                }}
                                            >
                                                {getChatTitle(conversation)}
                                            </div>
                                        </Tooltip>
                                        <div
                                            style={{
                                                fontSize: '11px',
                                                color: selected ? 'rgba(255, 255, 255, 0.8)' : '#999',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '4px',
                                            }}
                                        >
                                            <ClockCircleOutlined/>
                                            {conversation.lastMessageTime || conversation.createdAt}
                                        </div>
                                    </div>
                                </div>

                                {conversations.length > 1 && (
                                    <Tooltip title="删除对话">
                                        <Button
                                            type="text"
                                            icon={
                                                <DeleteOutlined
                                                    style={{color: selected ? 'rgba(255, 255, 255, 0.7)' : '#999'}}
                                                />
                                            }
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                onDelete(index);
                                            }}
                                        />
                                    </Tooltip>
                                )}
                            </Card>
                        );
                    })
                )}
            </div>
        </div>
    );
};

export default ConversationSidebar;
