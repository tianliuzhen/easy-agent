import React, { useState, useEffect } from 'react';
import { Think } from '@ant-design/x';
import type { ThinkingProcess } from './types';

interface ThinkComponentProps {
  thinking: ThinkingProcess;
}

/**
 * 思考过程展示组件
 * 使用 @ant-design/x 的 Think 组件
 */
const ThinkComponent: React.FC<ThinkComponentProps> = ({ thinking }) => {
  const [title, setTitle] = useState('思考中...');
  const [loading, setLoading] = useState(true);
  const [expand, setExpand] = useState(true);

  useEffect(() => {
    if (thinking.status === 'done') {
      setTitle('思考完成');
      setLoading(false);
      setExpand(true);  // 完成后保持展开
    } else if (thinking.status === 'error') {
      setTitle('思考出错');
      setLoading(false);
    } else if (thinking.status === 'streaming') {
      setTitle('思考中...');
      setLoading(true);
      setExpand(true);   // 流式过程中保持展开
    }
  }, [thinking.status]);

  return (
    <Think
      title={title}
      loading={loading}
      expanded={expand}
      onClick={() => setExpand(!expand)}
    >
      {thinking.content}
    </Think>
  );
};

export default ThinkComponent;
