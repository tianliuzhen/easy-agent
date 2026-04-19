import React, { useState, useEffect } from 'react';
import {
  Card,
  List,
  Tag,
  Button,
  Input,
  Space,
  Badge,
  Tooltip,
  Modal,
  Form,
  message,
  Descriptions,
  Divider,
  Empty,
  Row,
  Col,
  Statistic
} from 'antd';
import {
  CloudServerOutlined,
  PlusOutlined,
  EyeOutlined,
  DownloadOutlined,
  StarOutlined,
  StarFilled,
  GlobalOutlined,
  CodeOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { mcpApi } from '../../api/McpApi';

const { Search } = Input;

interface MarketMCPItem {
  id: string;
  name: string;
  displayName: string;
  description: string;
  provider: string;
  version: string;
  icon?: string;
  tags: string[];
  capabilities: string[];
  installCount: number;
  rating: number;
  isOfficial: boolean;
  isInstalled: boolean;
  serverUrl?: string;
  transportType: string;
  command?: string;
  documentation?: string;
  repository?: string;
}

const MCPMarket: React.FC = () => {
  const [data, setData] = useState<MarketMCPItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [installModalVisible, setInstallModalVisible] = useState(false);
  const [viewingItem, setViewingItem] = useState<MarketMCPItem | null>(null);
  const [installingItem, setInstallingItem] = useState<MarketMCPItem | null>(null);
  const [form] = Form.useForm();
  const [installLoading, setInstallLoading] = useState(false);

  // 模拟加载市场数据
  const loadData = async () => {
    setLoading(true);
    try {
      // 模拟市场数据
      const mockData: MarketMCPItem[] = [
        {
          id: '1',
          name: 'filesystem',
          displayName: '文件系统操作',
          description: '提供文件读写、目录遍历、文件搜索等功能，支持多种文件格式',
          provider: 'Anthropic',
          version: '1.2.0',
          tags: ['文件', 'IO', '本地'],
          capabilities: ['read', 'write', 'list', 'delete', 'search'],
          installCount: 12580,
          rating: 4.8,
          isOfficial: true,
          isInstalled: false,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-filesystem /allowed/path',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem',
          repository: 'https://github.com/modelcontextprotocol/servers'
        },
        {
          id: '2',
          name: 'github',
          displayName: 'GitHub 操作',
          description: 'GitHub API 集成，支持仓库管理、Issue 操作、PR 管理等功能',
          provider: 'GitHub',
          version: '2.1.0',
          tags: ['Git', '代码', '协作'],
          capabilities: ['clone', 'commit', 'pull', 'push', 'branch', 'issue', 'pr'],
          installCount: 8920,
          rating: 4.6,
          isOfficial: true,
          isInstalled: true,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-github',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/github',
          repository: 'https://github.com/modelcontextprotocol/servers'
        },
        {
          id: '3',
          name: 'postgres',
          displayName: 'PostgreSQL 数据库',
          description: 'PostgreSQL 数据库操作，支持查询、插入、更新、删除等操作',
          provider: 'Community',
          version: '1.0.3',
          tags: ['数据库', 'SQL', 'PostgreSQL'],
          capabilities: ['query', 'insert', 'update', 'delete', 'schema'],
          installCount: 5620,
          rating: 4.5,
          isOfficial: false,
          isInstalled: false,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-postgres postgres://localhost/mydb',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/postgres',
          repository: 'https://github.com/modelcontextprotocol/servers'
        },
        {
          id: '4',
          name: 'brave-search',
          displayName: 'Brave 搜索',
          description: '实时网页搜索和信息提取，基于 Brave Search API',
          provider: 'Brave',
          version: '1.1.0',
          tags: ['搜索', '网络', 'API'],
          capabilities: ['web_search', 'image_search', 'news_search'],
          installCount: 7890,
          rating: 4.7,
          isOfficial: true,
          isInstalled: false,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-brave-search',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search',
          repository: 'https://github.com/modelcontextprotocol/servers'
        },
        {
          id: '5',
          name: 'sqlite',
          displayName: 'SQLite 数据库',
          description: 'SQLite 数据库操作，轻量级本地数据库支持',
          provider: 'Community',
          version: '1.0.5',
          tags: ['数据库', 'SQL', 'SQLite'],
          capabilities: ['query', 'insert', 'update', 'delete', 'schema'],
          installCount: 4230,
          rating: 4.4,
          isOfficial: false,
          isInstalled: false,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-sqlite /path/to/database.db',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/sqlite',
          repository: 'https://github.com/modelcontextprotocol/servers'
        },
        {
          id: '6',
          name: 'fetch',
          displayName: '网页获取',
          description: '获取网页内容，支持 HTML 解析和内容提取',
          provider: 'Community',
          version: '1.0.2',
          tags: ['网络', 'HTTP', '解析'],
          capabilities: ['fetch', 'parse', 'extract'],
          installCount: 3560,
          rating: 4.3,
          isOfficial: false,
          isInstalled: false,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-fetch',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/fetch',
          repository: 'https://github.com/modelcontextprotocol/servers'
        },
        {
          id: '7',
          name: 'puppeteer',
          displayName: '浏览器自动化',
          description: '基于 Puppeteer 的浏览器自动化，支持页面操作和截图',
          provider: 'Community',
          version: '1.0.8',
          tags: ['浏览器', '自动化', '截图'],
          capabilities: ['navigate', 'click', 'type', 'screenshot', 'evaluate'],
          installCount: 2890,
          rating: 4.2,
          isOfficial: false,
          isInstalled: false,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-puppeteer',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/puppeteer',
          repository: 'https://github.com/modelcontextprotocol/servers'
        },
        {
          id: '8',
          name: 'google-maps',
          displayName: 'Google 地图',
          description: 'Google Maps API 集成，支持地理编码、路线规划等功能',
          provider: 'Google',
          version: '1.0.0',
          tags: ['地图', '地理', 'API'],
          capabilities: ['geocode', 'directions', 'places', 'distance'],
          installCount: 2150,
          rating: 4.1,
          isOfficial: true,
          isInstalled: false,
          transportType: 'STDIO',
          command: 'npx -y @modelcontextprotocol/server-google-maps',
          documentation: 'https://github.com/modelcontextprotocol/servers/tree/main/src/google-maps',
          repository: 'https://github.com/modelcontextprotocol/servers'
        }
      ];

      // 模拟延迟
      setTimeout(() => {
        setData(mockData);
        setLoading(false);
      }, 500);
    } catch (error) {
      console.error('加载 MCP 市场数据失败:', error);
      message.error('加载失败');
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // 查看详情
  const handleView = (item: MarketMCPItem) => {
    setViewingItem(item);
    setDetailModalVisible(true);
  };

  // 打开安装模态框
  const handleInstall = (item: MarketMCPItem) => {
    setInstallingItem(item);
    form.resetFields();
    form.setFieldsValue({
      serverName: item.displayName,
      transportType: item.transportType,
      command: item.command,
      connectionTimeout: 30,
      maxRetries: 3,
      status: 'active'
    });
    setInstallModalVisible(true);
  };

  // 提交安装
  const handleInstallSubmit = async () => {
    try {
      const values = await form.validateFields();
      setInstallLoading(true);

      // 处理环境变量
      if (values.envVars) {
        values.envVars = values.envVars.split('\n').filter((line: string) => line.trim());
      }

      const response = await mcpApi.createConfig({
        ...values,
        toolName: installingItem?.name,
        toolDisplayName: installingItem?.displayName,
        toolDescription: installingItem?.description
      });

      if (response.success) {
        message.success('安装成功');
        setInstallModalVisible(false);
        // 更新本地状态
        setData(prev => prev.map(item =>
          item.id === installingItem?.id ? { ...item, isInstalled: true } : item
        ));
      } else {
        message.error(response.message || '安装失败');
      }
    } catch (error) {
      console.error('安装失败:', error);
      message.error('安装失败');
    } finally {
      setInstallLoading(false);
    }
  };

  // 渲染评分星星
  const renderRating = (rating: number) => {
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      if (i <= rating) {
        stars.push(<StarFilled key={i} style={{ color: '#faad14' }} />);
      } else {
        stars.push(<StarOutlined key={i} style={{ color: '#d9d9d9' }} />);
      }
    }
    return <Space size={2}>{stars}</Space>;
  };

  // 过滤数据
  const filteredData = data.filter(item =>
    item.name.toLowerCase().includes(searchText.toLowerCase()) ||
    item.displayName.toLowerCase().includes(searchText.toLowerCase()) ||
    item.description.toLowerCase().includes(searchText.toLowerCase()) ||
    item.tags.some(tag => tag.toLowerCase().includes(searchText.toLowerCase()))
  );

  return (
    <div>
      <Card
        title={
          <Space>
            <GlobalOutlined />
            <span>MCP 市场</span>
            <Tag color="blue">{data.length} 个可用</Tag>
          </Space>
        }
        extra={
          <Search
            placeholder="搜索 MCP..."
            allowClear
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 300 }}
          />
        }
      >
        <List
          grid={{ gutter: 16, xs: 1, sm: 1, md: 2, lg: 2, xl: 3, xxl: 4 }}
          dataSource={filteredData}
          loading={loading}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无匹配的 MCP"
              />
            )
          }}
          renderItem={(item) => (
            <List.Item>
              <Card
                hoverable
                size="small"
                className={item.isInstalled ? 'mcp-installed' : ''}
                title={
                  <Space>
                    <CloudServerOutlined style={{ color: '#722ed1' }} />
                    <span style={{ fontWeight: 500 }}>{item.displayName}</span>
                    {item.isOfficial && (
                      <Tooltip title="官方认证">
                        <CheckCircleOutlined style={{ color: '#52c41a' }} />
                      </Tooltip>
                    )}
                    {item.isInstalled && (
                      <Tag color="success" size="small">已安装</Tag>
                    )}
                  </Space>
                }
                actions={[
                  <Tooltip title="查看详情">
                    <Button
                      type="text"
                      icon={<EyeOutlined />}
                      onClick={() => handleView(item)}
                    >
                      详情
                    </Button>
                  </Tooltip>,
                  item.isInstalled ? (
                    <Button
                      type="text"
                      disabled
                      icon={<DownloadOutlined />}
                    >
                      已安装
                    </Button>
                  ) : (
                    <Button
                      type="primary"
                      icon={<PlusOutlined />}
                      onClick={() => handleInstall(item)}
                    >
                      安装
                    </Button>
                  )
                ]}
              >
                <div style={{ minHeight: 120 }}>
                  <p style={{ color: '#666', marginBottom: 12, minHeight: 40 }}>
                    {item.description}
                  </p>

                  <Space wrap size={[4, 4]} style={{ marginBottom: 12 }}>
                    {item.tags.map(tag => (
                      <Tag key={tag} size="small">{tag}</Tag>
                    ))}
                  </Space>

                  <div style={{ marginBottom: 8 }}>
                    <Space wrap size={[4, 4]}>
                      {item.capabilities.slice(0, 4).map(cap => (
                        <Tag key={cap} color="blue" size="small" style={{ fontSize: 10 }}>
                          {cap}
                        </Tag>
                      ))}
                      {item.capabilities.length > 4 && (
                        <Tag size="small" style={{ fontSize: 10 }}>
                          +{item.capabilities.length - 4}
                        </Tag>
                      )}
                    </Space>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Space direction="vertical" size={0}>
                      <span style={{ fontSize: 12, color: '#999' }}>
                        提供者: {item.provider}
                      </span>
                      <span style={{ fontSize: 12, color: '#999' }}>
                        版本: {item.version}
                      </span>
                    </Space>
                    <Space direction="vertical" size={0} align="end">
                      {renderRating(item.rating)}
                      <span style={{ fontSize: 12, color: '#999' }}>
                        {item.installCount.toLocaleString()} 次安装
                      </span>
                    </Space>
                  </div>
                </div>
              </Card>
            </List.Item>
          )}
        />
      </Card>

      {/* 详情模态框 */}
      <Modal
        title={
          <Space>
            <CloudServerOutlined style={{ color: '#722ed1' }} />
            <span>MCP 详情</span>
          </Space>
        }
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
          viewingItem && !viewingItem.isInstalled && (
            <Button
              key="install"
              type="primary"
              icon={<DownloadOutlined />}
              onClick={() => {
                setDetailModalVisible(false);
                handleInstall(viewingItem);
              }}
            >
              安装
            </Button>
          )
        ]}
        width={700}
      >
        {viewingItem && (
          <>
            <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
              <Col span={12}>
                <Statistic
                  title="评分"
                  value={viewingItem.rating}
                  prefix={<StarFilled style={{ color: '#faad14' }} />}
                  suffix="/ 5"
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="安装次数"
                  value={viewingItem.installCount}
                  prefix={<DownloadOutlined />}
                />
              </Col>
            </Row>

            <Descriptions bordered column={2}>
              <Descriptions.Item label="名称" span={2}>
                {viewingItem.displayName}
              </Descriptions.Item>
              <Descriptions.Item label="标识">
                <code>{viewingItem.name}</code>
              </Descriptions.Item>
              <Descriptions.Item label="版本">
                {viewingItem.version}
              </Descriptions.Item>
              <Descriptions.Item label="提供者">
                {viewingItem.isOfficial ? (
                  <Space>
                    {viewingItem.provider}
                    <Tag color="success">官方</Tag>
                  </Space>
                ) : (
                  <Space>
                    {viewingItem.provider}
                    <Tag>社区</Tag>
                  </Space>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="传输类型">
                <Tag color="purple">{viewingItem.transportType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="标签" span={2}>
                <Space wrap>
                  {viewingItem.tags.map(tag => (
                    <Tag key={tag}>{tag}</Tag>
                  ))}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="能力" span={2}>
                <Space wrap>
                  {viewingItem.capabilities.map(cap => (
                    <Tag key={cap} color="blue">{cap}</Tag>
                  ))}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>
                {viewingItem.description}
              </Descriptions.Item>
              {viewingItem.documentation && (
                <Descriptions.Item label="文档" span={2}>
                  <a href={viewingItem.documentation} target="_blank" rel="noopener noreferrer">
                    {viewingItem.documentation}
                  </a>
                </Descriptions.Item>
              )}
              {viewingItem.repository && (
                <Descriptions.Item label="仓库" span={2}>
                  <a href={viewingItem.repository} target="_blank" rel="noopener noreferrer">
                    {viewingItem.repository}
                  </a>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="启动命令" span={2}>
                <code style={{ wordBreak: 'break-all', background: '#f5f5f5', padding: 8, display: 'block', borderRadius: 4 }}>
                  {viewingItem.command}
                </code>
              </Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Modal>

      {/* 安装模态框 */}
      <Modal
        title={
          <Space>
            <DownloadOutlined />
            <span>安装 MCP: {installingItem?.displayName}</span>
          </Space>
        }
        open={installModalVisible}
        onOk={handleInstallSubmit}
        onCancel={() => setInstallModalVisible(false)}
        confirmLoading={installLoading}
        width={600}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
        >
          <Form.Item
            name="serverName"
            label="服务器名称"
            rules={[{ required: true, message: '请输入服务器名称' }]}
          >
            <Input placeholder="用于标识此 MCP Server" />
          </Form.Item>

          <Form.Item
            name="command"
            label="启动命令"
            rules={[{ required: true, message: '请输入启动命令' }]}
          >
            <Input.TextArea
              rows={2}
              placeholder="启动 MCP Server 的命令"
            />
          </Form.Item>

          <Form.Item
            name="envVars"
            label="环境变量"
            extra="每行一个，格式：KEY=value。如需 API Key 请在此设置"
          >
            <Input.TextArea
              rows={3}
              placeholder={`BRAVE_API_KEY=your_api_key_here
DEBUG=true`}
            />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={2} placeholder="可选描述信息" />
          </Form.Item>

          <Divider />

          <Space size="large">
            <Form.Item
              name="connectionTimeout"
              label="连接超时(秒)"
            >
              <Input type="number" min={1} max={300} style={{ width: 100 }} />
            </Form.Item>

            <Form.Item
              name="maxRetries"
              label="最大重试次数"
            >
              <Input type="number" min={0} max={10} style={{ width: 100 }} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
};

export default MCPMarket;
