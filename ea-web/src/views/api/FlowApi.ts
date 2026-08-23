const API_BASE_URL = 'http://localhost:8080';

export type PortKind = 'CONTROL' | 'DATA' | 'WORKER';
export type ValueType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON' | 'IMAGE' | 'MESSAGE' | 'MESSAGE_LIST' | 'ANY';

export interface BaseResult<T> { success: boolean; data: T; message?: string; }
export interface PortDefinition { key: string; name: string; kind: PortKind; valueType: ValueType; required: boolean; multiple: boolean; defaultValue?: unknown; }
export interface ConfigPropertySchema {
  type?: string; title?: string; description?: string; enum?: unknown[]; default?: unknown;
  items?: {type?: string; properties?: Record<string, ConfigPropertySchema>; required?: string[]};
}
export interface NodeMetadata {
  type: string; version: string; category: string; displayName: string; description: string; icon?: string;
  inputPorts: PortDefinition[]; outputPorts: PortDefinition[];
  configSchema: {properties?: Record<string, ConfigPropertySchema>; required?: string[]};
}
export interface FlowNodeDefinition {
  id: string; type: string; version: string; name: string; position: {x: number; y: number};
  config: Record<string, unknown>; inputPorts: PortDefinition[]; outputPorts: PortDefinition[];
}
export interface FlowEdgeDefinition {
  id: string; kind: PortKind; source: {nodeId: string; portKey: string}; target: {nodeId: string; portKey: string}; config: Record<string, unknown>;
}
export interface FlowDefinition { schemaVersion: string; flowId?: number; revisionId?: number; name?: string; nodes: FlowNodeDefinition[]; edges: FlowEdgeDefinition[]; }
export interface FlowSummary {
  id: number; flowName: string; flowKey: string; flowDesc?: string; avatar?: string; welcomeMessage?: string;
  currentDraftId?: number; publishedRevisionId?: number; publishedRevisionNo?: number; draft?: FlowDefinition;
}
export interface ValidationIssue { severity: 'ERROR' | 'WARNING'; targetType: 'FLOW' | 'NODE' | 'EDGE'; targetId?: string; code: string; message: string; }
export interface ValidationResult { valid: boolean; issues: ValidationIssue[]; }

const json = async <T>(url: string, init?: RequestInit): Promise<T> => {
  const response = await fetch(`${API_BASE_URL}${url}`, init);
  const result = await response.json() as BaseResult<T>;
  if (!result.success) throw new Error(result.message || '请求失败');
  return result.data;
};

export const flowApi = {
  list: () => json<FlowSummary[]>('/eaAgent/flow/list'),
  detail: (id: number) => json<FlowSummary>(`/eaAgent/flow/${id}`),
  create: (body: Pick<FlowSummary, 'flowName' | 'flowKey' | 'flowDesc' | 'avatar' | 'welcomeMessage'>) =>
    json<number>('/eaAgent/flow', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)}),
  remove: (id: number) => json<boolean>(`/eaAgent/flow/${id}`, {method: 'DELETE'}),
  nodeTypes: () => json<NodeMetadata[]>('/eaAgent/flow/node-types'),
  draft: (id: number) => json<FlowDefinition>(`/eaAgent/flow/${id}/draft`),
  saveDraft: (id: number, definition: FlowDefinition) => json<boolean>(`/eaAgent/flow/${id}/draft`, {method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(definition)}),
  validate: (id: number, definition: FlowDefinition) => json<ValidationResult>(`/eaAgent/flow/${id}/validate`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(definition)}),
  publish: (id: number) => json<number>(`/eaAgent/flow/${id}/publish`, {method: 'POST'}),
};
