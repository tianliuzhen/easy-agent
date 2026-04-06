import React, { useState, useEffect } from 'react';
import {
  Card,
  List,
  Tag,
  Button,
  Input,
  Space,
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
  PlusOutlined,
  EyeOutlined,
  DownloadOutlined,
  StarOutlined,
  StarFilled,
  GlobalOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import { skillApi } from '../../api/SkillApi';

const { Search } = Input;

interface MarketSkillItem {
  id: string;
  skillName: string;
  skillDisplayName: string;
  skillDescription: string;
  skillProvider: string;
  skillVersion: string;
  skillIcon?: string;
  skillCategory: string;
  skillCapabilities: string[];
  installCount: number;
  rating: number;
  isOfficial: boolean;
  isInstalled: boolean;
  skillType: string;
  executionMode: string;
  documentation?: string;
}

const SkillMarket: React.FC = () => {
  const [data, setData] = useState<MarketSkillItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [installModalVisible, setInstallModalVisible] = useState(false);
  const [viewingItem, setViewingItem] = useState<MarketSkillItem | null>(null);
  const [installingItem, setInstallingItem] = useState<MarketSkillItem | null>(null);
  const [form] = Form.useForm();
  const [installLoading, setInstallLoading] = useState(false);

  // 加载市场数据（官方 Skill）
  const loadData = async () => {
    setLoading(true);
    try {
      // 调用 API 获取官方 Skill
      const response = await skillApi.getOfficialSkills();
      if (response.success) {
        // 转换后端数据为前端格式
        const marketData: MarketSkillItem[] = (response.data || []).map((item: any) => ({
          id: item.id?.toString() || '',
          skillName: item.skillName,
          skillDisplayName: item.skillDisplayName || item.skillName,
          skillDescription: item.skillDescription || '',
          skillProvider: item.skillProvider || 'System',
          skillVersion: item.skillVersion || '1.0.0',
          skillIcon: item.skillIcon,
          skillCategory: item.skillCategory || 'general',
          skillCapabilities: item.skillCapabilities || [],
          installCount: item.installCount || 0,
          rating: item.rating || 4.5,
          isOfficial: item.userId === 0,
          isInstalled: false,
          skillType: item.skillType || 'INTERNAL',
          executionMode: item.executionMode || 'sync',
          documentation: item.documentation
        }));
        setData(marketData);
      } else {
        message.error(response.message || '加载失败');
      }
    } catch (error) {
      console.error('加载 Skill 市场数据失败:', error);
      message.error('加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // 查看详情
  const handleView = (item: MarketSkillItem) => {
    setViewingItem(item);
    setDetailModalVisible(true);
  };

  // 打开安装模态框
  const handleInstall = (item: MarketSkillItem) => {
    setInstallingItem(item);
    form.resetFields();
    form.setFieldsValue({
      skillName: item.skillName,
      skillDisplayName: item.skillDisplayName,
      skillDescription: item.skillDescription,
      skillType: item.skillType,
      skillCategory: item.skillCategory,
      skillIcon: item.skillIcon,
      skillVersion: item.skillVersion,
      skillProvider: item.skillProvider,
      executionMode: item.executionMode,
      timeout: 30,
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

      // 调用安装接口，传入官方 Skill ID
      const skillConfigId = parseInt(installingItem?.id || '0');
      const customConfig = JSON.stringify(values.skillConfig || {});
      const response = await skillApi.installSkill(skillConfigId, customConfig);

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

  // 获取分类标签
  const getCategoryTag = (category: string) => {
    const categoryMap: Record<string, { color: string; text: string }> = {
      general: { color: 'blue', text: '通用' },
      development: { color: 'purple', text: '开发' },
      data: { color: 'cyan', text: '数据' },
      media: { color: 'magenta', text: '媒体' }
    };
    const { color, text } = categoryMap[category] || categoryMap.general;
    return <Tag color={color}>{text}</Tag>;
  };

  // 过滤数据
  const filteredData = data.filter(item =>
    item.skillName.toLowerCase().includes(searchText.toLowerCase()) ||
    item.skillDisplayName.toLowerCase().includes(searchText.toLowerCase()) ||
    item.skillDescription.toLowerCase().includes(searchText.toLowerCase())
  );

  return (
    <div>
      <Card
        title={
          <Space>
            <GlobalOutlined />
            <span>Skill 市场</span>
            <Tag color="blue">{data.length} 个可用</Tag>
          </Space>
        }
        extra={
          <Search
            placeholder="搜索 Skill..."
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
                description="暂无匹配的 Skill"
              />
            )
          }}
          renderItem={(item) => (
            <List.Item>
              <Card
                hoverable
                size="small"
                className={item.isInstalled ? 'skill-installed' : ''}
                title={
                  <Space>
                    <span style={{ fontSize: '20px' }}>{item.skillIcon || '🔧'}</span>
                    <span style={{ fontWeight: 500 }}>{item.skillDisplayName}</span>
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
                    {item.skillDescription}
                  </p>

                  <div style={{ marginBottom: 8 }}>
                    {getCategoryTag(item.skillCategory)}
                  </div>

                  <div style={{ marginBottom: 8 }}>
                    <Space wrap size={[4, 4]}>
                      {item.skillCapabilities.slice(0, 4).map(cap => (
                        <Tag key={cap} color="blue" size="small" style={{ fontSize: 10 }}>
                          {cap}
                        </Tag>
                      ))}
                      {item.skillCapabilities.length > 4 && (
                        <Tag size="small" style={{ fontSize: 10 }}>
                          +{item.skillCapabilities.length - 4}
                        </Tag>
                      )}
                    </Space>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Space direction="vertical" size={0}>
                      <span style={{ fontSize: 12, color: '#999' }}>
                        提供者: {item.skillProvider}
                      </span>
                      <span style={{ fontSize: 12, color: '#999' }}>
                        版本: {item.skillVersion}
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
            <span style={{ fontSize: '24px' }}>{viewingItem?.skillIcon || '🔧'}</span>
            <span>Skill 详情</span>
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
                {viewingItem.skillDisplayName}
              </Descriptions.Item>
              <Descriptions.Item label="标识">
                <code>{viewingItem.skillName}</code>
              </Descriptions.Item>
              <Descriptions.Item label="版本">
                {viewingItem.skillVersion}
              </Descriptions.Item>
              <Descriptions.Item label="提供者">
                {viewingItem.isOfficial ? (
                  <Space>
                    {viewingItem.skillProvider}
                    <Tag color="success">官方</Tag>
                  </Space>
                ) : (
                  <Space>
                    {viewingItem.skillProvider}
                    <Tag>社区</Tag>
                  </Space>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="分类">
                {getCategoryTag(viewingItem.skillCategory)}
              </Descriptions.Item>
              <Descriptions.Item label="执行模式">
                {viewingItem.executionMode === 'sync' ? '同步' : '异步'}
              </Descriptions.Item>
              <Descriptions.Item label="能力" span={2}>
                <Space wrap>
                  {viewingItem.skillCapabilities.map(cap => (
                    <Tag key={cap} color="blue">{cap}</Tag>
                  ))}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>
                {viewingItem.skillDescription}
              </Descriptions.Item>
              {viewingItem.documentation && (
                <Descriptions.Item label="文档" span={2}>
                  <a href={viewingItem.documentation} target="_blank" rel="noopener noreferrer">
                    {viewingItem.documentation}
                  </a>
                </Descriptions.Item>
              )}
            </Descriptions>
          </>
        )}
      </Modal>

      {/* 安装模态框 */}
      <Modal
        title={
          <Space>
            <DownloadOutlined />
            <span>安装 Skill: {installingItem?.skillDisplayName}</span>
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
            name="skillName"
            label="技能名称"
            rules={[{ required: true, message: '请输入技能名称' }]}
          >
            <Input disabled />
          </Form.Item>

          <Form.Item
            name="skillDisplayName"
            label="显示名称"
            rules={[{ required: true, message: '请输入显示名称' }]}
          >
            <Input placeholder="用于展示的技能名称" />
          </Form.Item>

          <Form.Item
            name="skillDescription"
            label="描述"
          >
            <Input.TextArea rows={2} placeholder="技能的功能描述" />
          </Form.Item>

          <Divider />

          <Form.Item
            name="skillConfig"
            label="技能配置(JSON)"
            extra="可选：配置技能的执行参数"
          >
            <Input.TextArea
              rows={4}
              placeholder={`{
  "timeout": 60,
  "customParam": "value"
}`}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SkillMarket;
