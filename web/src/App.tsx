// App.tsx
import React from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom';
import IndexLayout from './views/IndexLayout';
import ChatDemo from './views/page/ChatDemo';
import AgentConfig from './views/page/AgentConfig';
import SQLConfig from './views/page/agent/tool/SQLConfig';
import HTTPConfig from './views/page/agent/tool/HTTPConfig';
import MCPConfig from './views/page/agent/tool/MCPConfig';
import GRPCConfig from './views/page/agent/tool/GRPCConfig';
import { App as AntdApp, Button, ConfigProvider, Flex } from 'antd';

function App() {
  return (
    <BrowserRouter>
      <ConfigProvider getPopupContainer={() => document.body}>
        <AntdApp>
          <AppContent />
        </AntdApp>
      </ConfigProvider>
    </BrowserRouter>
  );
}
function AppContent() {
  const location = useLocation();

  // 如果是 /home 路径，渲染带导航栏的布局
  if (location.pathname.startsWith('/home')) {
    return (
      <>
        <nav>
          <Flex wrap gap="small" className="site-button-ghost-wrapper">

            <Button type="primary" >
              <Link to="/">后台管理</Link>
            </Button>

            <Button type="primary" >
              <Link to="/home/chatdemo">开始聊天</Link>
            </Button>
          </Flex>

        </nav>
        <Routes>
          <Route path="/home/chatdemo" element={<ChatDemo />} />
        </Routes>
      </>
    );
  }

  if (location.pathname.startsWith('/pageTool')) {
    return (
        <>
          <Routes>
            <Route path="/pageTool/AgentConfig/*" element={<AgentConfig />}>
              {/*  在当前代码中，tool/sql ... 嵌套路由的定义并没有实际作用，因为组件内部通过程序化方式处理了路径匹配和组件渲染*/}
              <Route path="tool/sql" element={<SQLConfig />} /> /
              <Route path="tool/http" element={<HTTPConfig />} />
              <Route path="tool/mcp" element={<MCPConfig />} />
              <Route path="tool/grpc" element={<GRPCConfig />} />
            </Route>
          </Routes>
        </>
    );
  }

  // 默认路径 (/)，只渲染 IndexLayout 
  return (
    <IndexLayout />
  );
}

export default App;