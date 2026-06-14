import React, {useState, useEffect, useImperativeHandle, forwardRef, useRef} from 'react';
import {Card, Typography, Divider, Switch, message, InputNumber, Tooltip} from 'antd';
import {SettingOutlined, ThunderboltOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import {eaAgentApi} from '../../../api/EaAgentApi';

const {Text} = Typography;

const topPTooltip = (
    <div style={{maxWidth: '480px', lineHeight: 1.6}}>
        <p style={{margin: '0 0 6px 0', fontWeight: 600}}>Top P（核采样）</p>
        <p style={{margin: '0 0 6px 0'}}>
            模型按概率从高到低累加候选词，直到累加值达到 Top P 阈值，只在这个范围内随机选择。
        </p>
        <p style={{margin: '0 0 6px 0'}}>
            <strong>示例：</strong>候选词 A(0.4)、B(0.3)、C(0.2)、D(0.1)
        </p>
        <ul style={{margin: '0 0 6px 0', paddingLeft: '20px'}}>
            <li>Top P = 0.9 → 累加到 A+B+C = 0.9，从 A/B/C 中选</li>
            <li>Top P = 0.5 → 累加到 A+B = 0.7 {'>'} 0.5，从 A/B 中选</li>
        </ul>
        <p style={{margin: '0'}}><strong>建议：</strong>越低输出越聚焦确定，越高输出越多样创意。一般对话 0.9，创意写作 0.95，代码生成 0.8。</p>
    </div>
);

const topKTooltip = (
    <div style={{maxWidth: '480px', lineHeight: 1.6}}>
        <p style={{margin: '0 0 6px 0', fontWeight: 600}}>Top K（候选词数量限制）</p>
        <p style={{margin: '0 0 6px 0'}}>
            模型只保留概率最高的 K 个候选词，将其概率重新归一化后从中随机选择，其余词全部排除。
        </p>
        <p style={{margin: '0 0 6px 0'}}>
            <strong>示例：</strong>候选词 A(0.4)、B(0.3)、C(0.15)、D(0.1)、E(0.05)
        </p>
        <ul style={{margin: '0 0 6px 0', paddingLeft: '20px'}}>
            <li>Top K = 3 → 只从 A/B/C 中选，D/E 被排除</li>
            <li>Top K = 1 → 固定选 A，无随机性</li>
        </ul>
        <p style={{margin: '0'}}><strong>建议：</strong>越低输出越确定，越高输出越多样。一般对话 10，创意写作 20，代码生成 5。</p>
    </div>
);

export interface AgentSettingsData {
    streamEnabled: boolean;
    topP?: number;
    topK?: number;
}

export interface AgentSettingsRef {
    getSettings: () => AgentSettingsData;
}

/**
 * Agent 设置组件
 * 包含：流式输出开关、Top P、Top K 等模型参数设置
 */
const AgentSettings = forwardRef<AgentSettingsRef, {agentId?: number}>(
    ({agentId}, ref) => {
        // 流式输出
        const [streamEnabled, setStreamEnabled] = useState<boolean>(true);

        // 采样参数
        const [topP, setTopP] = useState<number | undefined>(undefined);
        const [topK, setTopK] = useState<number | undefined>(undefined);

        // 保存完整的 modelConfig，避免覆盖其他字段
        const [fullModelConfig, setFullModelConfig] = useState<any>({});

        // 保存状态
        const [saving, setSaving] = useState<boolean>(false);
        const saveTimerRef = useRef<NodeJS.Timeout>();

        // 加载设置
        useEffect(() => {
            if (agentId) {
                eaAgentApi.queryAgent(agentId).then(result => {
                    if (result && result.data && result.data.modelConfig) {
                        try {
                            const config = JSON.parse(result.data.modelConfig);
                            setFullModelConfig(config);
                            if (config.streamEnabled !== undefined) {
                                setStreamEnabled(config.streamEnabled);
                            }
                            if (config.topP !== undefined) {
                                setTopP(config.topP);
                            }
                            if (config.topK !== undefined) {
                                setTopK(config.topK);
                            }
                        } catch (e) {
                            console.error('解析模型配置失败:', e);
                        }
                    }
                }).catch(err => {
                    console.error('加载设置失败:', err);
                });
            }
        }, [agentId]);

        // 保存设置（带防抖）
        const saveSettings = (overrideConfig?: Partial<AgentSettingsData>) => {
            if (!agentId) return;

            // 清除之前的定时器
            if (saveTimerRef.current) {
                clearTimeout(saveTimerRef.current);
            }

            // 设置新的定时器，延迟 500ms 保存
            saveTimerRef.current = setTimeout(async () => {
                setSaving(true);

                const configToSave = overrideConfig || {};
                const modelConfig = {
                    ...fullModelConfig,
                    streamEnabled: configToSave.streamEnabled !== undefined ? configToSave.streamEnabled : streamEnabled,
                    topP: configToSave.topP !== undefined ? configToSave.topP : topP,
                    topK: configToSave.topK !== undefined ? configToSave.topK : topK,
                };

                try {
                    const agentData = {
                        id: agentId,
                        modelConfig: JSON.stringify(modelConfig)
                    };
                    await eaAgentApi.saveAgent(agentData);
                    // 更新本地状态
                    setFullModelConfig(modelConfig);
                    setStreamEnabled(modelConfig.streamEnabled);
                    setTopP(modelConfig.topP);
                    setTopK(modelConfig.topK);
                    console.log('设置保存成功:', modelConfig);
                    message.success('已保存', 1);
                } catch (error) {
                    console.error('保存设置失败:', error);
                    message.error('保存失败');
                } finally {
                    setSaving(false);
                }
            }, 500);
        };

        // 暴露获取配置的方法给父组件
        useImperativeHandle(ref, () => ({
            getSettings: () => ({
                streamEnabled,
                topP,
                topK
            })
        }));

        return (
            <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                {/* 输出设置 */}
                <Card
                    size="small"
                    style={{borderRadius: '6px'}}
                    bodyStyle={{padding: '16px'}}
                >
                    <Text style={{
                        fontSize: '14px',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px'
                    }}>
                        <SettingOutlined style={{fontSize: '14px', color: '#1890ff'}}/>
                        输出设置
                    </Text>

                    <Divider style={{margin: '12px 0'}}/>

                    {/* 流式输出开关 */}
                    <div style={{
                        display: 'flex',
                        alignItems: 'baseline',
                        gap: '12px',
                        justifyContent: 'space-between'
                    }}>
                        <div>
                            <Text style={{fontSize: '13px', whiteSpace: 'nowrap'}}>流式输出</Text>
                            <Text type="secondary" style={{fontSize: '12px', marginLeft: '8px'}}>
                                关闭后等待全部回答完成后一次性显示结果
                            </Text>
                        </div>
                        <Switch
                            checked={streamEnabled}
                            onChange={(checked) => {
                                setStreamEnabled(checked);
                                saveSettings({streamEnabled: checked});
                            }}
                            size="small"
                            loading={saving}
                        />
                    </div>
                </Card>

                {/* 采样参数 */}
                <Card
                    size="small"
                    style={{borderRadius: '6px'}}
                    bodyStyle={{padding: '16px'}}
                >
                    <Text style={{
                        fontSize: '14px',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px'
                    }}>
                        <ThunderboltOutlined style={{fontSize: '14px', color: '#faad14'}}/>
                        采样参数
                    </Text>

                    <Divider style={{margin: '12px 0'}}/>

                    {/* Top P */}
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px',
                        marginBottom: '16px'
                    }}>
                        <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                            <Text style={{
                                fontSize: '13px',
                                color: '#666',
                                whiteSpace: 'nowrap',
                                minWidth: '64px'
                            }}>
                                Top P
                            </Text>
                            <Tooltip title={topPTooltip} placement="right" overlayInnerStyle={{padding: '12px 16px'}}>
                                <QuestionCircleOutlined style={{color: '#999', fontSize: '14px', cursor: 'pointer'}}/>
                            </Tooltip>
                        </div>
                        <InputNumber<number>
                            min={0}
                            max={1}
                            step={0.01}
                            precision={2}
                            value={topP}
                            onChange={(val) => {
                                setTopP(val ?? undefined);
                                saveSettings({topP: val ?? undefined});
                            }}
                            style={{width: '100px'}}
                            size="small"
                            placeholder="0.9"
                        />
                        <Text type="secondary" style={{fontSize: '12px'}}>
                            概率累计阈值，建议 0.8~0.95
                        </Text>
                    </div>

                    {/* Top K */}
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px'
                    }}>
                        <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                            <Text style={{
                                fontSize: '13px',
                                color: '#666',
                                whiteSpace: 'nowrap',
                                minWidth: '64px'
                            }}>
                                Top K
                            </Text>
                            <Tooltip title={topKTooltip} placement="right" overlayInnerStyle={{padding: '12px 16px'}}>
                                <QuestionCircleOutlined style={{color: '#999', fontSize: '14px', cursor: 'pointer'}}/>
                            </Tooltip>
                        </div>
                        <InputNumber<number>
                            min={1}
                            max={100}
                            step={1}
                            value={topK}
                            onChange={(val) => {
                                setTopK(val ?? undefined);
                                saveSettings({topK: val ?? undefined});
                            }}
                            style={{width: '100px'}}
                            size="small"
                            placeholder="10"
                        />
                        <Text type="secondary" style={{fontSize: '12px'}}>
                            候选词数量，建议 1~50
                        </Text>
                    </div>
                </Card>
            </div>
        );
    });

export default AgentSettings;
