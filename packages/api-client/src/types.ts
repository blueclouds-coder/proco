// API の DTO 型（暫定手書き。OpenAPI 生成に置き換える）

/** 業務パラメータの値の型 */
export type ParamValueType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON';

/** GET /api/v1/config/params の 1 要素 */
export interface ConfigParamResponse {
  key: string;
  valueType: ParamValueType;
  value: string;
  description?: string;
  updatedAt?: string;
  updatedBy?: string;
  version?: number;
}

/** PUT /api/v1/config/params/{key} のリクエスト */
export interface PutConfigParamRequest {
  key: string;
  valueType: ParamValueType;
  value: string;
}

/** 画面項目設定（tenant-config.md §3） */
export interface FieldConfigResponse {
  fieldId: string;
  visible: boolean;
  required: boolean;
  label?: string;
  defaultValue?: unknown;
  options?: unknown;
  visibleWhen?: unknown;
  sortOrder?: number;
}
