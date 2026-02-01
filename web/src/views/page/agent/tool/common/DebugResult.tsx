import React, { useEffect, useRef } from 'react';
import { Divider, Typography } from 'antd';
import { CheckCircleTwoTone, InfoCircleTwoTone } from '@ant-design/icons';
import JSONEditor from 'jsoneditor';
import 'jsoneditor/dist/jsoneditor.min.css';

interface DebugResultProps {
  debugResult: any; // 接收原始API响应
  loading?: boolean;
  title?: string;
}

const DebugResult: React.FC<DebugResultProps> = ({ debugResult, loading = false, title = '调试结果' }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<any>(null);

  // 处理API响应，构建调试结果对象
  const processApiResponse = (response: any) => {
    if (!response) return null;
    
    // 根据API响应格式确定成功状态
    const isSuccess = response.success === true || response.code === 200;
    // 状态码优先使用response.code，否则根据成功状态决定
    const status = response.code || (isSuccess ? 200 : 500);
    // 状态文本优先使用response.statusText，否则根据成功状态决定
    const statusText = response.statusText || (isSuccess ? 'OK' : 'Error');

    return {
      data: response.data !== undefined ? response.data : (response.message || 'No data returned'),
      status: status,
      statusText: statusText,
      time: response.time || 0, // 执行时间
    };
  };

  const processedResult = debugResult ? processApiResponse(debugResult) : null;

  useEffect(() => {
    const options = {
      mode: 'code',           // 默认模式：tree/code/text
      modes: ['tree', 'code', 'form', 'text'], // 允许的模式
      search: true,           // 启用搜索功能
      history: true,          // 启用撤销重做
      mainMenuBar: true,      // 显示主菜单栏
      statusBar: true,        // 显示状态栏
      navigationBar: true,    // 显示导航栏
      onChange: function() {
        // JSON内容变化时的回调
        console.log('内容已更改');
      },
      onValidationError: function(errors) {
        // 验证错误时的回调
        console.warn('验证错误:', errors);
      }
    };

    if (containerRef.current && processedResult && processedResult.data && typeof processedResult.data !== 'string') {
      // 创建 JSONEditor 实例
      editorRef.current = new JSONEditor(containerRef.current, options);

      // 设置数据
      editorRef.current.set(processedResult.data);
    }
    
    // 清理函数
    return () => {
      if (editorRef.current) {
        editorRef.current.destroy();
        editorRef.current = null;
      }
    };
  }, [processedResult]);

  if (!processedResult || loading) {
    if (loading) {
      return (
        <div style={{ marginTop: 20 }}>
          <Divider>{title}</Divider>
          <div style={{ padding: '16px', textAlign: 'center', backgroundColor: '#f0f0f0' }}>
            正在加载调试结果...
          </div>
        </div>
      );
    }
    return null;
  }

  return (
    <div style={{ marginTop: 20 }}>
      <Divider>{title}</Divider>
      <div style={{ display: 'flex', alignItems: 'center', padding: '12px', backgroundColor: processedResult.status >= 200 && processedResult.status < 300 ? '#f6ffed' : '#fff2f0', border: '1px solid', borderColor: processedResult.status >= 200 && processedResult.status < 300 ? '#b7eb8f' : '#ffccc7', borderRadius: '6px 6px 0 0', marginBottom: 0 }}>
        <span style={{ fontWeight: 'bold', fontSize: '14px', color: processedResult.status >= 200 && processedResult.status < 300 ? '#52c41a' : '#ff4d4f', display: 'flex', alignItems: 'center' }}>
          {processedResult.status >= 200 && processedResult.status < 300 ? (
            <>
              <CheckCircleTwoTone twoToneColor="#52c41a" style={{ fontSize: '16px', marginRight: '6px' }} />
              Status: {processedResult.status} {processedResult.statusText}
            </>
          ) : (
            <>
              <InfoCircleTwoTone twoToneColor="#ff4d4f" style={{ fontSize: '16px', marginRight: '6px' }} />
              Status: {processedResult.status} {processedResult.statusText}
            </>
          )}
        </span>
        <span style={{ marginLeft: 'auto', fontSize: '13px', color: '#666' }}>
          耗时: {processedResult.time} ms
        </span>
      </div>
      <div style={{ padding: '16px', border: '1px solid #d9d9d9', borderTop: 'none', borderRadius: '0 0 6px 6px' }}>
        <Typography.Text strong>响应数据:</Typography.Text>
        <div style={{ marginTop: 8 }}>
          {typeof processedResult.data === 'string'
            ? <div style={{ fontFamily: 'monospace', backgroundColor: '#f6f6f6', padding: '12px', borderRadius: '4px', maxHeight: '300px', overflow: 'auto' }}>
                {processedResult.data}
              </div>
            : <div ref={containerRef} style={{ minHeight: '400px', maxHeight: '800px', overflow: 'auto' }} />
          }
        </div>
      </div>
      <style>{`
        .jsoneditor {
          height: 400px !important;
        }
        .jsoneditor, .jsoneditor-ace, .jsoneditor-wrapper, .jsoneditor-frame {
          height: 400px !important;
        }
        .ace_editor.ace-jsoneditor {
          min-height: 500px;
          height: 500px;
        }
      `}</style>
    </div>
  );
};

export default DebugResult;