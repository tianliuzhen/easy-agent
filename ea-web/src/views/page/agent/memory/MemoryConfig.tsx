import React, {useState, useImperativeHandle, forwardRef, useEffect, useRef} from 'react';
import {Card, InputNumber, Slider, Select, Typography, Divider, Switch, message, Input} from 'antd';
import {
    AlertOutlined,
    HistoryOutlined,
    ThunderboltOutlined,
    PartitionOutlined,
    ScissorOutlined,
    ControlOutlined
} from '@ant-design/icons';
import {eaAgentApi} from '../../../api/EaAgentApi';

const {Text} = Typography;

export interface MemoryConfigData {
    roundLimit: number;
    roundLimitEnabled: boolean;
    triggerThreshold: number;
    overflowStrategy: string;
    windowStrategyEnabled: boolean;
    keepRounds: number;
    toolTrimEnabled: boolean;
    // 压缩相关配置
    safeMessageCount?: number;  // 安全区域消息数
    customCompressPrompt?: string;  // 自定义压缩指令
}

export interface MemoryConfigRef {
    getMemoryConfig: () => MemoryConfigData;
}

/**
 * 记忆配置组件
 *
 * 包含：参数说明、长期记忆（预留）、上下文窗口控制、工具结果修剪
 */
const MemoryConfig = forwardRef<MemoryConfigRef, { agentId?: number }>(
    ({agentId}, ref) => {
        // 上下文轮数限制
        const [roundLimit, setRoundLimit] = useState<number>(50);
        const [roundLimitEnabled, setRoundLimitEnabled] = useState<boolean>(true);

        // 上下文窗口策略 - 触发阈值
        const [triggerThreshold, setTriggerThreshold] = useState<number>(0.82);
        const [overflowStrategy, setOverflowStrategy] = useState<'sliding' | 'compression'>('compression');
        const [windowStrategyEnabled, setWindowStrategyEnabled] = useState<boolean>(true);

        // 工具结果修剪 - 保留轮数
        const [keepRounds, setKeepRounds] = useState<number>(2);
        const [toolTrimEnabled, setToolTrimEnabled] = useState<boolean>(true);

        // 压缩相关配置
        const [safeMessageCount, setSafeMessageCount] = useState<number>(5);
        const [customCompressPrompt, setCustomCompressPrompt] = useState<string>('');

        // 保存状态
        const [saving, setSaving] = useState<boolean>(false);
        const saveTimerRef = useRef<NodeJS.Timeout>();

        // 加载记忆配置
        useEffect(() => {
            if (agentId) {
                eaAgentApi.queryAgent(agentId).then(result => {
                    if (result && result.data && result.data.memoryConfig) {
                        try {
                            const config = JSON.parse(result.data.memoryConfig);
                            if (config.roundLimit !== undefined) setRoundLimit(config.roundLimit);
                            if (config.roundLimitEnabled !== undefined) setRoundLimitEnabled(config.roundLimitEnabled);
                            if (config.triggerThreshold !== undefined) setTriggerThreshold(config.triggerThreshold);
                            if (config.overflowStrategy) setOverflowStrategy(config.overflowStrategy);
                            if (config.windowStrategyEnabled !== undefined) setWindowStrategyEnabled(config.windowStrategyEnabled);
                            if (config.keepRounds !== undefined) setKeepRounds(config.keepRounds);
                            if (config.toolTrimEnabled !== undefined) setToolTrimEnabled(config.toolTrimEnabled);
                            if (config.safeMessageCount !== undefined) setSafeMessageCount(config.safeMessageCount);
                            if (config.customCompressPrompt) setCustomCompressPrompt(config.customCompressPrompt);
                        } catch (e) {
                            console.error('解析记忆配置失败:', e);
                        }
                    }
                }).catch(err => {
                    console.error('加载记忆配置失败:', err);
                });
            }
        }, [agentId]);

        // 保存记忆配置（带防抖）
        const saveMemoryConfig = (overrideConfig?: Partial<MemoryConfigData>) => {
            if (!agentId) return;

            // 清除之前的定时器
            if (saveTimerRef.current) {
                clearTimeout(saveTimerRef.current);
            }

            // 设置新的定时器，延迟 500ms 保存
            saveTimerRef.current = setTimeout(async () => {
                setSaving(true);

                // 使用传入的配置覆盖，否则使用当前状态
                const configToSave = overrideConfig || {};
                const memoryConfig = {
                    roundLimit: configToSave.roundLimit ?? roundLimit,
                    roundLimitEnabled: configToSave.roundLimitEnabled !== undefined ? configToSave.roundLimitEnabled : roundLimitEnabled,
                    triggerThreshold: configToSave.triggerThreshold ?? triggerThreshold,
                    overflowStrategy: configToSave.overflowStrategy ?? overflowStrategy,
                    windowStrategyEnabled: configToSave.windowStrategyEnabled !== undefined ? configToSave.windowStrategyEnabled : windowStrategyEnabled,
                    keepRounds: configToSave.keepRounds ?? keepRounds,
                    toolTrimEnabled: configToSave.toolTrimEnabled !== undefined ? configToSave.toolTrimEnabled : toolTrimEnabled,
                    safeMessageCount: overflowStrategy === 'compression' ? safeMessageCount : undefined,
                    customCompressPrompt: overflowStrategy === 'compression' ? customCompressPrompt : undefined
                };

                try {
                    const agentData = {
                        id: agentId,
                        memoryConfig: JSON.stringify(memoryConfig)
                    };
                    await eaAgentApi.saveAgent(agentData);
                    console.log('记忆配置保存成功:', memoryConfig);
                    message.success('已保存', 1);
                } catch (error) {
                    console.error('保存记忆配置失败:', error);
                    message.error('保存失败');
                } finally {
                    setSaving(false);
                }
            }, 500);
        };

        // 溢出策略选项
        const strategyOptions = [
            {value: 'sliding', label: '滑动窗口'},
            {value: 'compression', label: '压缩上下文'}
        ];

        // 暴露获取配置的方法给父组件
        useImperativeHandle(ref, () => ({
            getMemoryConfig: () => ({
                roundLimit,
                triggerThreshold,
                overflowStrategy,
                keepRounds
            })
        }));

        return (
            <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                {/* 区块1：参数说明 */}
                {/*<Card*/}
                {/*    size="small"*/}
                {/*    style={{*/}
                {/*        borderRadius: '6px',*/}
                {/*        background: '#fafafa',*/}
                {/*        border: 'none'*/}
                {/*    }}*/}
                {/*    bodyStyle={{padding: '12px 16px'}}*/}
                {/*>*/}
                {/*    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>*/}
                {/*        <AlertOutlined style={{color: '#1890ff', fontSize: '14px'}}/>*/}
                {/*        <Text strong style={{fontSize: '14px'}}>参数</Text>*/}
                {/*    </div>*/}
                {/*    <div style={{marginTop: '8px', paddingLeft: '22px'}}>*/}
                {/*        <Text type="secondary" style={{fontSize: '13px', lineHeight: 1.6}}>*/}
                {/*            可通过工具/工作流赋值，或者从外部传入*/}
                {/*        </Text>*/}
                {/*    </div>*/}
                {/*</Card>*/}

                {/* 区块2：长期记忆（预留） */}
                {/*<Card*/}
                {/*    size="small"*/}
                {/*    style={{*/}
                {/*        borderRadius: '6px',*/}
                {/*        border: '1px dashed #d9d9d9',*/}
                {/*        background: '#fafafa'*/}
                {/*    }}*/}
                {/*    bodyStyle={{padding: '12px 16px'}}*/}
                {/*>*/}
                {/*    <Text strong style={{fontSize: '14px'}}>长期记忆</Text>*/}
                {/*    <div style={{*/}
                {/*        marginTop: '8px',*/}
                {/*        padding: '32px 0',*/}
                {/*        display: 'flex',*/}
                {/*        justifyContent: 'center',*/}
                {/*        alignItems: 'center'*/}
                {/*    }}>*/}
                {/*        <Text type="secondary" style={{fontSize: '13px'}}>*/}
                {/*            预留扩展区域*/}
                {/*        </Text>*/}
                {/*    </div>*/}
                {/*</Card>*/}

                {/* 区块3：上下文窗口控制 */}
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
                        <PartitionOutlined style={{fontSize: '14px', color: '#4ac41a'}}/>
                        上下文窗口控制
                    </Text>

                    {/* 3.1 上下文轮数限制 */}
                    <div style={{marginTop: '16px'}}>
                        <div style={{
                            display: 'flex',
                            alignItems: 'baseline',
                            gap: '12px',
                            justifyContent: 'space-between'
                        }}>
                            <div>
                                <Text style={{fontSize: '13px', whiteSpace: 'nowrap'}}>上下文轮数限制</Text>
                                <Text type="secondary" style={{fontSize: '12px', marginLeft: '8px'}}>
                                    控制多轮对话中保留的上下文轮数
                                </Text>
                            </div>
                            <Switch
                                checked={roundLimitEnabled}
                                onChange={(checked) => {
                                    setRoundLimitEnabled(checked);
                                    saveMemoryConfig({ roundLimitEnabled: checked });
                                }}
                                size="small"
                                loading={saving}
                            />
                        </div>
                        <div style={{marginTop: '8px', display: 'flex', alignItems: 'center', gap: '12px'}}>
                            <Text style={{
                                fontSize: '13px',
                                color: '#666',
                                whiteSpace: 'nowrap',
                                minWidth: '64px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '4px'
                            }}>
                                <HistoryOutlined style={{fontSize: '12px', color: '#1890ff'}}/>
                                轮数上限
                            </Text>
                            <Slider
                                min={1}
                                max={50}
                                step={1}
                                value={roundLimit}
                                disabled={!roundLimitEnabled}
                                onChange={(value) => {
                                    setRoundLimit(value);
                                    saveMemoryConfig();
                                }}
                                style={{flex: 1}}
                            />
                            <InputNumber<number>
                                min={1}
                                max={50}
                                value={roundLimit}
                                disabled={!roundLimitEnabled}
                                onChange={(val) => {
                                    setRoundLimit(val ?? 20);
                                    saveMemoryConfig();
                                }}
                                style={{width: '80px'}}
                                size="small"
                            />
                        </div>
                    </div>

                    <Divider style={{margin: '16px 0'}}/>

                    {/* 3.2 上下文窗口策略 */}
                    <div>
                        <div style={{
                            display: 'flex',
                            alignItems: 'baseline',
                            gap: '12px',
                            justifyContent: 'space-between'
                        }}>
                            <div>
                                <Text style={{fontSize: '13px', whiteSpace: 'nowrap'}}>上下文窗口策略</Text>
                                <Text type="secondary" style={{fontSize: '12px', marginLeft: '8px'}}>
                                    当上下文接近窗口上限时，按策略处理（滑动/压缩）
                                </Text>
                            </div>
                            <Switch
                                checked={windowStrategyEnabled}
                                onChange={(checked) => {
                                    setWindowStrategyEnabled(checked);
                                    saveMemoryConfig({ windowStrategyEnabled: checked });
                                }}
                                size="small"
                                loading={saving}
                            />
                        </div>

                        {/* 触发阈值：Slider + InputNumber */}
                        <div style={{marginTop: '12px', display: 'flex', alignItems: 'center', gap: '12px'}}>
                            <Text style={{
                                fontSize: '13px',
                                color: '#666',
                                whiteSpace: 'nowrap',
                                minWidth: '64px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '4px'
                            }}>
                                <ThunderboltOutlined style={{fontSize: '12px', color: '#faad14'}}/>
                                触发阈值
                            </Text>
                            <Slider
                                min={0}
                                max={1}
                                step={0.01}
                                value={triggerThreshold}
                                disabled={!windowStrategyEnabled}
                                onChange={(value) => {
                                    setTriggerThreshold(value);
                                    saveMemoryConfig();
                                }}
                                style={{flex: 1}}
                            />
                            <InputNumber<number>
                                min={0}
                                max={1}
                                step={0.01}
                                value={triggerThreshold}
                                disabled={!windowStrategyEnabled}
                                onChange={(val) => {
                                    setTriggerThreshold(val ?? 0.82);
                                    saveMemoryConfig();
                                }}
                                style={{width: '80px'}}
                                size="small"
                            />
                        </div>

                        {/* 溢出处理策略：Select */}
                        <div style={{marginTop: '12px', display: 'flex', alignItems: 'center', gap: '12px'}}>
                            <Text style={{
                                fontSize: '13px',
                                color: '#666',
                                whiteSpace: 'nowrap',
                                minWidth: '64px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '4px'
                            }}>
                                <ControlOutlined style={{fontSize: '12px', color: '#fa8c16'}}/>
                                溢出处理策略
                            </Text>
                            <Select
                                value={overflowStrategy}
                                disabled={!windowStrategyEnabled}
                                onChange={(value) => {
                                    setOverflowStrategy(value);
                                    saveMemoryConfig();
                                }}
                                options={strategyOptions}
                                style={{width: '120px'}}
                                size="small"
                            />
                        </div>

                        {/* 压缩策略专属配置 */}
                        {overflowStrategy === 'compression' && (
                            <div style={{marginTop: '16px', paddingLeft: '12px', borderLeft: '2px solid #e8e8e8'}}>
                                {/* 安全区域消息数 */}
                                <div style={{marginTop: '12px', display: 'flex', alignItems: 'center', gap: '12px'}}>
                                    <Text style={{
                                        fontSize: '13px',
                                        color: '#666',
                                        whiteSpace: 'nowrap',
                                        minWidth: '140px'
                                    }}>
                                        保留最近消息数
                                    </Text>
                                    <InputNumber<number>
                                        min={1}
                                        max={20}
                                        step={1}
                                        value={safeMessageCount}
                                        onChange={(val) => {
                                            setSafeMessageCount(val ?? 5);
                                            saveMemoryConfig();
                                        }}
                                        style={{width: '100px'}}
                                        size="small"
                                        addonAfter="条"
                                    />
                                    <Text type="secondary" style={{fontSize: '12px'}}>
                                        不纳入压缩的安全区域
                                    </Text>
                                </div>

                                {/* 自定义压缩指令 */}
                                <div style={{marginTop: '12px'}}>
                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'baseline',
                                        gap: '12px',
                                        marginBottom: '8px'
                                    }}>
                                        <Text style={{fontSize: '13px', color: '#666', whiteSpace: 'nowrap'}}>
                                            自定义压缩指令
                                        </Text>
                                        <Text type="secondary" style={{fontSize: '12px'}}>
                                            将添加到系统压缩 Prompt 后面
                                        </Text>
                                    </div>
                                    <Input.TextArea
                                        value={customCompressPrompt}
                                        onChange={(e) => {
                                            setCustomCompressPrompt(e.target.value);
                                            saveMemoryConfig();
                                        }}
                                        placeholder="请输入自定义压缩指令，例如：请保留关键信息，删除冗余内容..."
                                        rows={3}
                                        maxLength={500}
                                        showCount
                                        style={{resize: 'vertical'}}
                                    />
                                </div>
                            </div>
                        )}
                    </div>
                </Card>

                {/* 区块4：工具结果修剪 */}
                <Card
                    size="small"
                    style={{borderRadius: '6px'}}
                    bodyStyle={{padding: '16px'}}
                >
                    <div style={{
                        display: 'flex',
                        alignItems: 'baseline',
                        justifyContent: 'space-between',
                        marginBottom: '12px'
                    }}>
                        <Text style={{
                            fontSize: '14px',
                            display: 'flex',
                            alignItems: 'center',
                            fontWeight: 'bold',
                            gap: '2px'
                        }}>
                            <ScissorOutlined style={{fontSize: '14px', color: '#722ed1'}}/>
                            工具结果修剪
                        </Text>
                        <Switch
                            checked={toolTrimEnabled}
                            onChange={(checked) => {
                                setToolTrimEnabled(checked);
                                saveMemoryConfig({ toolTrimEnabled: checked });
                            }}
                            size="small"
                            loading={saving}
                        />
                    </div>
                    <div style={{
                        marginTop: '12px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        flexWrap: 'wrap'
                    }}>
                        <Text style={{fontSize: '13px', color: '#666'}}>保留最近</Text>
                        <InputNumber<number>
                            min={1}
                            max={10}
                            step={1}
                            value={keepRounds}
                            disabled={!toolTrimEnabled}
                            onChange={(val) => {
                                setKeepRounds(val ?? 2);
                                saveMemoryConfig();
                            }}
                            style={{width: '70px'}}
                            size="small"
                        />
                        <Text style={{fontSize: '13px', color: '#666'}}>轮执行结果</Text>
                    </div>
                    <div style={{marginTop: '8px'}}>
                        <Text type="secondary" style={{fontSize: '12px', lineHeight: 1.5}}>
                            轮次按用户输入计算，仅修剪工具执行结果，不裁剪用户和模型信息
                        </Text>
                    </div>
                </Card>
            </div>
        );
    });

export default MemoryConfig;
