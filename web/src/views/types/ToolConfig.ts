export interface EaToolConfig {
  id?: number;
  agentId?: number;
  toolType: string;
  toolInstanceId?: string;
  toolInstanceName?: string;
  inputTemplate?: string;
  outTemplate?: string;
  isRequired?: boolean;
  sortOrder?: number;
  isActive?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  toolValue?: string;
  extraConfig?: string;
}

export interface EaToolConfigReq extends EaToolConfig {}

export interface EaToolConfigResult extends EaToolConfig {}