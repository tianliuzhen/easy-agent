import React from 'react';
import { Divider, Typography } from 'antd';
import { CheckCircleTwoTone, InfoCircleTwoTone } from '@ant-design/icons';
import JsonView from '@uiw/react-json-view';
import { githubLightTheme } from '@uiw/react-json-view/githubLight';

interface DebugResultProps {
  debugResult: any; // 接收原始API响应
  loading?: boolean;
  title?: string;
}

const DebugResult: React.FC<DebugResultProps> = ({ debugResult, loading = false, title = '调试结果' }) => {
  // 处理API响应，构建调试结果对象
  const processApiResponse = (response: any) => {
    if (!response) return null;

    const isSuccess = response.success === true || response.code === 200;
    const status = response.code || (isSuccess ? 200 : 500);
    const statusText = response.statusText || (isSuccess ? 'OK' : 'Error');

    return {
      data: response.data !== undefined ? response.data : (response.message || 'No data returned'),
      status,
      statusText,
      time: response.time || 0,
    };
  };

  const processedResult = debugResult ? processApiResponse(debugResult) : null;

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

  const isOk = processedResult.status >= 200 && processedResult.status < 300;

  return (
    <div style={{ marginTop: 20 }}>
      <Divider>{title}</Divider>
      <div style={{ display: 'flex', alignItems: 'center', padding: '12px', backgroundColor: isOk ? '#f6ffed' : '#fff2f0', border: '1px solid', borderColor: isOk ? '#b7eb8f' : '#ffccc7', borderRadius: '6px 6px 0 0', marginBottom: 0 }}>
        <span style={{ fontWeight: 'bold', fontSize: '14px', color: isOk ? '#52c41a' : '#ff4d4f', display: 'flex', alignItems: 'center' }}>
          {isOk ? (
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
            ? <div style={{ fontFamily: 'monospace', backgroundColor: '#f6f6f6', padding: '12px', borderRadius: '4px', maxHeight: '500px', overflow: 'auto', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {processedResult.data}
              </div>
            : <div style={{ maxHeight: '600px', overflow: 'auto', border: '1px solid #f0f0f0', borderRadius: '4px', padding: '12px' }}>
                <JsonView
                  value={processedResult.data}
                  style={{ ...githubLightTheme, fontSize: '13px' }}
                  displayDataTypes={false}
                  displayObjectSize
                  enableClipboard
                  collapsed={2}
                />
              </div>
          }
        </div>
      </div>
    </div>
  );
};

export default DebugResult;
