// 画面項目設定（field_configs）をテナント設定から解決するフック。
// 表示/必須/表示名のハードコードは禁止（CLAUDE.md §4、tenant-config.md §3）。
import { useQuery } from '@tanstack/react-query';
import { ProblemDetailsError, type FieldConfigResponse } from '@proco/api-client';
import { api } from '../api';

export interface ResolvedFieldConfig {
  visible: boolean;
  required: boolean;
  label?: string;
  defaultValue?: unknown;
  options?: unknown;
  visibleWhen?: unknown;
  sortOrder?: number;
}

export type FieldConfigMap = Record<string, ResolvedFieldConfig>;

/** 設定が無い項目に適用する既定値（全項目表示・任意） */
export const FALLBACK_FIELD_CONFIG: ResolvedFieldConfig = Object.freeze({
  visible: true,
  required: false,
});

export interface UseFieldConfigResult {
  /** fieldId -> 設定。未定義の項目は get() でフォールバックを返す */
  fields: FieldConfigMap;
  /** 指定項目の設定（未定義ならフォールバック） */
  get: (fieldId: string) => ResolvedFieldConfig;
  isLoading: boolean;
  /** エンドポイント未実装（404）や通信失敗でフォールバック中か */
  isFallback: boolean;
  error: unknown;
}

export function toFieldConfigMap(list: FieldConfigResponse[]): FieldConfigMap {
  const map: FieldConfigMap = {};
  for (const f of list) {
    map[f.fieldId] = {
      visible: f.visible,
      required: f.required,
      label: f.label,
      defaultValue: f.defaultValue,
      options: f.options,
      visibleWhen: f.visibleWhen,
      sortOrder: f.sortOrder,
    };
  }
  return map;
}

/**
 * GET /api/v1/config/fields?screen=<screenId> を取得し、fieldId -> 設定 のマップを返す。
 * 404（未実装）やネットワークエラー時は全項目 visible=true のローカルフォールバックを返す。
 */
export function useFieldConfig(screenId: string): UseFieldConfigResult {
  const query = useQuery({
    queryKey: ['config', 'fields', screenId],
    queryFn: async (): Promise<{ map: FieldConfigMap; fallback: boolean }> => {
      try {
        const list = await api.getFieldConfig(screenId);
        return { map: toFieldConfigMap(list), fallback: false };
      } catch (err) {
        // 404 = エンドポイント未実装、その他の失敗もフォールバックで描画を止めない
        if (err instanceof ProblemDetailsError && err.status !== 404 && err.status < 500) {
          throw err; // 401/403 等はフォールバックせず呼び出し側へ
        }
        return { map: {}, fallback: true };
      }
    },
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  const fields = query.data?.map ?? {};
  return {
    fields,
    get: (fieldId) => fields[fieldId] ?? FALLBACK_FIELD_CONFIG,
    isLoading: query.isLoading,
    isFallback: query.data?.fallback ?? query.isError,
    error: query.error,
  };
}
