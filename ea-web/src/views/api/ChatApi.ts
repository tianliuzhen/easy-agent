const API_BASE_URL = 'http://localhost:8080'; // 替换为你的后端地址

/**
 * 发送消息到 SSE 服务端，并接收流式响应
 * @param message 用户输入的消息
 * @param sessionId 会话 ID
 * @param agentId 代理 ID
 * @param onLog 每次接收到日志数据时的回调
 * @param onFinalAnswer 每次接收到最终答案数据时的回调（替换原来的 onData）
 * @param onThink 每次接收到思考过程数据时的回调
 * @param onData 每次接收到数据消息时的回调
 * @param onTool 每次接收到工具执行数据时的回调
 * @param onDone 流式响应结束时的回调（后端关闭连接时触发）
 * @param onError 发生错误时的回调（如解析失败）
 * @returns 返回 EventSource 对象，可用于手动关闭连接
 */
export const sendMessage = (
  message: string,
  sessionId: string,
  agentId: string,
  onLog: (log: string) => void,
  onFinalAnswer: (data: string) => void,
  onThink: (think: string) => void,
  onData: (data: string) => void,
  onTool: (tool: string) => void,
  onDone: () => void,
  onError: (error: string) => void
): EventSource => {
  const encodedMsg = encodeURIComponent(message);
  const eventSource = new EventSource(`${API_BASE_URL}/eaAgent/ai/streamChatWith?msg=${encodedMsg}&sessionId=${sessionId}&agentId=${agentId}`);

  eventSource.onmessage = (event) => {
    try {
      console.log('SSE 数据:', event.data);

      // EventSource 已经自动处理了 "data:" 前缀，我们直接解析 JSON 数据
      const jsonData = JSON.parse(event.data);

      if (jsonData.type === 'log') {
        onLog(jsonData.content);
      } else if (jsonData.type === 'finalAnswer') {
        onFinalAnswer(jsonData.content);
      } else if (jsonData.type === 'think') {
        onThink(jsonData.content);
      } else if (jsonData.type === 'data') {
        onData(jsonData.content);
      } else if (jsonData.type === 'tool') {
        onTool(jsonData.content);
      }
    } catch (error) {
      console.error('SSE 数据解析失败:', error);
      console.error('接收到的数据:', event.data);
      onError('服务器返回的数据格式无效');
    }
  };

  eventSource.onerror = () => {
    // 注意：后端关闭连接时，会触发 onerror，此处视为正常结束
    console.log('SSE 连接已关闭（服务端主动终止）');
    eventSource.close();
    onDone(); // 调用结束回调
  };

  return eventSource;
};

/**
 * 手动关闭 SSE 连接（可选）
 * @param eventSource 由 sendMessage() 返回的 EventSource 对象
 */
export const closeConnection = (eventSource: EventSource | null) => {
  if (eventSource) {
    eventSource.close();
  }
};
