import React, { useState, useEffect } from 'react';
import {
  Modal,
  List,
  Card,
  Tag,
  Button,
  Input,
  Empty,
  Spin,
  Tabs,
  Tooltip,
  Radio,
  message
} from 'antd';
import { PlusOutlined, SearchOutlined, StarOutlined, CloudServerOutlined, DatabaseOutlined } from '@ant-design/icons';
import { skillApi } from '../../../api/SkillApi';

const { TabPane } = Tabs;

interface SkillItem {
  id: number;
  skillName: string;
  skillDisplayName: string;
  skillDescription: string;
  skillType: string;
  skillCategory: string;
  skillIcon: string;
  skillVersion: string;
  skillProvider: string;
  skillCapabilities: string[];
  status: string;
}

interface SkillSelectorProps {
  visible: boolean;
  agentId?: number;
  onCancel: () => void;
  onSuccess: () => void;
}

const SkillSelector: React.FC<SkillSelectorProps> = ({
  visible,
  agentId,
  onCancel,
  onSuccess
}) => {
  const [officialSkills, setOfficialSkills] = useState<SkillItem[]>([]);
  const [mySkills, setMySkills] = useState<SkillItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [binding, setBinding] = useState<number | null>(null);
  const [searchText, setSearchText] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const [resourceMode, setResourceMode] = useState<'official' | 'my'>('official');

  // 加载官方 Skill 列表
  const loadOfficialSkills = async () => {
    setLoading(true);
    try {
      const response = await skillApi.getOfficialSkills();
      if (response.success) {
        setOfficialSkills(response.data || []);
      } else {
        message.error(`加载官方 Skill 列表失败: ${response.message}`);
      }
    } catch (error) {
      console.error('加载官方 Skill 列表失败:', error);
      message.error('加载官方 Skill 列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 加载我的 Skill 列表
  const loadMySkills = async () => {
    setLoading(true);
    try {
      const response = await skillApi.getSkillConfigByUserId();
      if (response.success) {
        setMySkills(response.data || []);
      } else {
        message.error(`加载我的 Skill 列表失败: ${response.message}`);
      }
    } catch (error) {
      console.error('加载我的 Skill 列表失败:', error);
      message.error('加载我的 Skill 列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 处理资源模式切换
  const handleResourceModeChange = (mode: 'official' | 'my') => {
    setResourceMode(mode);
    if (mode === 'official') {
      loadOfficialSkills();
    } else {
      loadMySkills();
    }
  };

  // 当模态框打开时加载数据
  useEffect(() => {
    if (visible) {
      setResourceMode('official');
      loadOfficialSkills();
    }
  }, [visible]);

  // 绑定 Skill 到 Agent
  const handleBind = async (skill: SkillItem) => {
    if (!agentId) {
      message.error('无法绑定：缺少 Agent ID');
      return;
    }

    setBinding(skill.id);
    try {
      const request = {
        agentId: agentId.toString(),
        skillConfigId: skill.id
      };

      const response = await skillApi.bindSkill(request);
      if (response.success) {
        message.success(`已绑定 Skill: ${skill.skillDisplayName}`);
        onSuccess();
      } else {
        message.error(`绑定失败: ${response.message}`);
      }
    } catch (error) {
      console.error('绑定 Skill 失败:', error);
      message.error('绑定 Skill 失败');
    } finally {
      setBinding(null);
    }
  };

  // 获取分类标签颜色
  const getCategoryColor = (category: string) => {
    const categoryColors: Record<string, string> = {
      'general': 'blue',
      'development': 'purple',
      'data': 'cyan',
      'media': 'magenta'
    };
    return categoryColors[category] || 'default';
  };

  // 获取分类文本
  const getCategoryText = (category: string) => {
    const categoryTexts: Record<string, string> = {
      'general': '通用',
      'development': '开发',
      'data': '数据',
      'media': '媒体'
    };
    return categoryTexts[category] || category;
  };

  // 获取当前显示的技能列表
  const currentSkills = resourceMode === 'official' ? officialSkills : mySkills;

  // 过滤技能列表
  const filteredSkills = currentSkills.filter(skill => {
    // 搜索过滤
    const matchesSearch = searchText === '' ||
      skill.skillName.toLowerCase().includes(searchText.toLowerCase()) ||
      skill.skillDisplayName.toLowerCase().includes(searchText.toLowerCase()) ||
      skill.skillDescription?.toLowerCase().includes(searchText.toLowerCase());

    // 分类过滤
    const matchesCategory = activeCategory === 'all' || skill.skillCategory === activeCategory;

    return matchesSearch && matchesCategory;
  });

  // 获取所有分类
  const categories = ['all', ...new Set(currentSkills.map(s => s.skillCategory))];

  // 渲染能力标签
  const renderCapabilities = (capabilities: string[]) => {
    return (
      <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
        {capabilities.slice(0, 3).map((cap, index) => (
          <Tag key={index} color="blue" style={{ fontSize: '10px', padding: '0 4px' }}>
            {cap}
          </Tag>
        ))}
        {capabilities.length > 3 && (
          <Tag style={{ fontSize: '10px', padding: '0 4px' }}>
            +{capabilities.length - 3}
          </Tag>
        )}
      </div>
    );
  };

  return (
    <Modal
      title="选择 Skill 技能"
      open={visible}
      onCancel={onCancel}
      width={800}
      footer={null}
    >
      <div style={{ marginBottom: '16px' }}>
        <Radio.Group
          value={resourceMode}
          onChange={(e) => handleResourceModeChange(e.target.value)}
          style={{ width: '100%', marginBottom: '16px' }}
        >
          <Radio.Button value="official" style={{ width: '50%', textAlign: 'center' }}>
            <CloudServerOutlined /> 官方 Skill
          </Radio.Button>
          <Radio.Button value="my" style={{ width: '50%', textAlign: 'center' }}>
            <DatabaseOutlined /> 我的 Skill
          </Radio.Button>
        </Radio.Group>

        <Input
          placeholder="搜索技能..."
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ marginBottom: '16px' }}
        />

        <Tabs activeKey={activeCategory} onChange={setActiveCategory}>
          <TabPane tab="全部" key="all" />
          {categories.filter(c => c !== 'all').map(category => (
            <TabPane
              tab={getCategoryText(category)}
              key={category}
            />
          ))}
        </Tabs>
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '40px 0' }}>
          <Spin tip={`加载 ${resourceMode === 'official' ? '官方' : '我的'} Skill 列表...`} />
        </div>
      ) : filteredSkills.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={resourceMode === 'official' ? '暂无官方技能' : '暂无我的技能，请先前往 Skill 市场安装'}
          style={{ margin: '40px 0' }}
        />
      ) : (
        <List
          dataSource={filteredSkills}
          renderItem={(item) => (
            <List.Item>
              <Card
                size="small"
                style={{ width: '100%' }}
                bodyStyle={{ padding: '12px' }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                      <span style={{ fontSize: '20px', marginRight: '8px' }}>{item.skillIcon}</span>
                      <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600 }}>
                        {item.skillDisplayName}
                      </h4>
                      <Tag
                        color={getCategoryColor(item.skillCategory)}
                        style={{ marginLeft: '8px', fontSize: '11px' }}
                      >
                        {getCategoryText(item.skillCategory)}
                      </Tag>
                      <Tag style={{ marginLeft: '4px', fontSize: '11px' }}>
                        v{item.skillVersion}
                      </Tag>
                    </div>

                    <p style={{
                      margin: '0 0 8px 0',
                      fontSize: '12px',
                      color: '#666',
                      lineHeight: '1.4'
                    }}>
                      {item.skillDescription}
                    </p>

                    {renderCapabilities(item.skillCapabilities)}

                    <div style={{ marginTop: '8px', fontSize: '11px', color: '#999' }}>
                      提供者: {item.skillProvider}
                    </div>
                  </div>

                  <div style={{ marginLeft: '12px' }}>
                    <Button
                      type="primary"
                      icon={<PlusOutlined />}
                      size="small"
                      loading={binding === item.id}
                      onClick={() => handleBind(item)}
                    >
                      绑定
                    </Button>
                  </div>
                </div>
              </Card>
            </List.Item>
          )}
        />
      )}
    </Modal>
  );
};

export default SkillSelector;