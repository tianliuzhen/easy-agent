import React, {useCallback} from 'react';
import {message} from 'antd';
import {
    createConversation,
    getFullChatHistory,
    type ChatConversation,
} from '../api/ChatConversationApi';
import {ChatRightPanel} from './chat/ChatComponents';
import {ConversationSidebar} from './chat/ConversationSidebar';
import {historyToChatMessages} from './chat/chatContextUtils';
import {useChatSession} from './chat/hooks/useChatSession';
import {useConversations} from './chat/hooks/useConversations';
import {useChatStream} from './chat/hooks/useChatStream';

const CURRENT_USER_ID = '1';

const ChatDemo: React.FC = () => {
    const session = useChatSession();
    const convos = useConversations({
        userId: CURRENT_USER_ID,
        agentId: session.agentId,
        flowId: session.flowId,
    });
    const stream = useChatStream();

    // 打开某个会话：同步 conversationId（确保后续回复归属该会话）并加载历史
    const openConversation = useCallback(
        async (conversation: ChatConversation, index: number) => {
            convos.setSelectedChatIndex(index);
            session.setConversationId(conversation.id);
            session.syncSessionIdToUrl(conversation.id);
            try {
                const history = await getFullChatHistory(conversation.id);
                const {messages, thinkingLogs} = historyToChatMessages(history);
                stream.loadHistory(messages, thinkingLogs);
            } catch (error) {
                console.error('加载聊天记录失败:', error);
                message.error('加载聊天记录失败');
            }
        },
        [convos, session, stream]
    );

    const handleSelectChat = useCallback(
        (index: number) => {
            const conversation = convos.conversations[index];
            if (conversation) {
                openConversation(conversation, index).catch((err) => console.error('加载会话失败:', err));
            }
        },
        [convos.conversations, openConversation]
    );

    const handleNewChat = useCallback(() => {
        session.setConversationId(null);
        session.clearSessionIdFromUrl();
        convos.setSelectedChatIndex(-1);
        stream.reset();
    }, [session, convos, stream]);

    const handleDeleteChat = useCallback(
        async (index: number) => {
            const wasSelected = index === convos.selectedChatIndex;
            const remaining = await convos.remove(index);
            if (wasSelected) {
                if (remaining.length > 0) {
                    await openConversation(remaining[0], 0);
                } else {
                    handleNewChat();
                }
            }
        },
        [convos, openConversation, handleNewChat]
    );

    const handleSend = useCallback(async () => {
        if (stream.input.trim() === '' && !stream.selectedImage) return;

        let conversationId = session.conversationId;
        if (!conversationId) {
            try {
                conversationId = await createConversation({
                    agentId: session.agentId,
                    flowId: session.flowId,
                    userId: CURRENT_USER_ID,
                    status: 'active',
                });
                session.setConversationId(conversationId);
                session.syncSessionIdToUrl(conversationId);
            } catch (error) {
                console.error('创建会话失败:', error);
                message.error('创建会话失败');
                return;
            }
        }

        stream.send({
            sessionId: conversationId.toString(),
            agentId: session.agentId.toString(),
            flowId: session.flowId,
            onDone: () => {
                convos.reload().catch((err) => console.error('刷新会话列表失败:', err));
            },
        });
    }, [stream, session, convos]);

    const handleKeyPress = useCallback(
        (e: React.KeyboardEvent) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
            }
        },
        [handleSend]
    );

    return (
        <div
            style={{
                display: 'flex',
                height: '100vh',
                background: 'var(--ea-theme-background)',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
            }}
        >
            <ConversationSidebar
                conversations={convos.conversations}
                selectedChatIndex={convos.selectedChatIndex}
                loading={convos.loading}
                displayName={session.display.name}
                displayDesc={session.display.desc}
                onSelect={handleSelectChat}
                onDelete={handleDeleteChat}
                onNewChat={handleNewChat}
            />

            <div
                style={{
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                    position: 'relative',
                }}
            >
                {session.modelVersion && (
                    <div
                        style={{
                            position: 'absolute',
                            top: '12px',
                            left: '12px',
                            background: 'rgba(255, 255, 255, 0.9)',
                            border: '1px solid #d9e6f2',
                            borderRadius: '16px',
                            padding: '6px 12px',
                            fontSize: '11px',
                            color: '#666',
                            fontWeight: 500,
                            zIndex: 10,
                            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
                            backdropFilter: 'blur(10px)',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '6px',
                        }}
                    >
                        {session.modelIcon && (
                            <img
                                src={session.modelIcon}
                                alt="model icon"
                                style={{width: '16px', height: '16px', objectFit: 'contain'}}
                            />
                        )}
                        模型版本：{session.modelVersion}
                    </div>
                )}

                <ChatRightPanel
                    messages={stream.messages}
                    messageThinkingLogs={stream.thinkingLogs}
                    isThinking={stream.isThinking}
                    currentAnsweringMsgId={stream.currentAnsweringMsgId}
                    input={stream.input}
                    onInputChange={stream.setInput}
                    onSend={handleSend}
                    onKeyPress={handleKeyPress}
                    onToggleThinkingLog={stream.toggleThinkingLog}
                    agentName={session.display.name}
                    conversationId={session.conversationId}
                    error={stream.error}
                    welcomeMessage={session.display.welcome}
                    avatar={session.display.avatar}
                    selectedImage={stream.selectedImage}
                    onImageChange={stream.setSelectedImage}
                    quickPrompts={session.quickPrompts}
                    onQuickQuestion={stream.setInput}
                />
            </div>
        </div>
    );
};

export default ChatDemo;
