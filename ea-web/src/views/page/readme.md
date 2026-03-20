## 使用中文进行回答我
需要对 [AgentConfig.tsx](AgentConfig.tsx) 进行页面重构
页面分为三块：使用下面组件进行布局
```
import React, { useState } from 'react';
import { Flex, Splitter, Typography } from 'antd';

const defaultSizes = ['30%', '40%', '30%'];

const Desc: React.FC<Readonly<{ text?: string | number }>> = (props) => (
  <Flex justify="center" align="center" style={{ height: '100%' }}>
    <Typography.Title type="secondary" level={5} style={{ whiteSpace: 'nowrap' }}>
      Panel {props.text}
    </Typography.Title>
  </Flex>
);

const App: React.FC = () => {
  const [sizes, setSizes] = useState<(number | string)[]>(defaultSizes);

  const handleDoubleClick = () => {
    setSizes(defaultSizes);
  };

  return (
    <Splitter
      style={{ height: 200, boxShadow: '0 0 10px rgba(0, 0, 0, 0.1)' }}
      onResize={setSizes}
      onDraggerDoubleClick={handleDoubleClick}
    >
      <Splitter.Panel size={sizes[0]}>
        <Desc text={1} />
      </Splitter.Panel>

      <Splitter.Panel size={sizes[1]}>
        <Desc text={2} />
      </Splitter.Panel>

      <Splitter.Panel size={sizes[2]}>
        <Desc text={3} />
      </Splitter.Panel>
    </Splitter>
  );
};

export default App;
```
### 最左展示提示词：
一个大的文本域，用于输入提示词

### 中间展示绑定的知识库、工具（要求展示的时候）
使用这个组件进行展示，分别展示  【知识库 工具 mcp skills】默认展开，展示一个 + 引导用户去添加
```
import React from 'react';
import type { CollapseProps } from 'antd';
import { Collapse } from 'antd';

const text = `
  A dog is a type of domesticated animal.
  Known for its loyalty and faithfulness,
  it can be found as a welcome guest in many households across the world.
`;

const items: CollapseProps['items'] = [
  {
    key: '1',
    label: 'This is panel header 1',
    children: <p>{text}</p>,
  },
  {
    key: '2',
    label: 'This is panel header 2',
    children: <p>{text}</p>,
  },
  {
    key: '3',
    label: 'This is panel header 3',
    children: <p>{text}</p>,
  },
];

const App: React.FC = () => {
  const onChange = (key: string | string[]) => {
    console.log(key);
  };

  return <Collapse items={items} defaultActiveKey={['1']} onChange={onChange} />;
};

export default App;
```

### 最右侧展示 
对话调试 需要支持 [ChatDemo.tsx](ChatDemo.tsx) 的功能，但是不需要展示左侧菜单，直接就是对话调试
