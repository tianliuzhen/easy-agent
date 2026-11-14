import React from 'react';
import {XMarkdown} from '@ant-design/x-markdown';
import './chatMessage.css';

interface MarkdownContentProps {
    content: string;
    /** 内容是否仍在流式追加；为 false 时 XMarkdown 会把未闭合语法刷成最终结果 */
    streaming?: boolean;
    /** 追加的类名，用于区分回答区与过程区的字号 */
    className?: string;
}

/**
 * 聊天场景统一的 Markdown 渲染出口。
 * <p>
 * 所有消息内容都经由这里渲染，保证排版（.ea-md）与安全选项一致：
 * - escapeRawHtml：模型输出里的原始 HTML 只按文本展示，不解析执行
 * - openLinksInNewTab：外链在新标签打开
 * - streaming.hasNextChunk：流式结束时必须为 false，否则半截语法不会收敛
 * <p>
 * 关于流式稳定性：这里刻意关闭 enableAnimation 与 tail。
 * - enableAnimation 会把每个 chunk 包成一个带淡入动画的 span，span 数量随输出
 *   线性增长，每帧都要重排，中文逐字输出时抖动明显；
 * - tail 把光标字符接在正文末尾，当末行接近换行点时会让末尾文字来回换行。
 * 生成状态改由 ChatMessageItem 的独立提示行表达，不参与正文排版。
 */
const MarkdownContent: React.FC<MarkdownContentProps> = ({content, streaming = false, className}) => (
    <XMarkdown
        content={content}
        className={className ? `ea-md ${className}` : 'ea-md'}
        openLinksInNewTab
        escapeRawHtml
        streaming={{
            hasNextChunk: streaming,
            enableAnimation: false,
            tail: false,
        }}
    />
);

export default React.memo(MarkdownContent);
