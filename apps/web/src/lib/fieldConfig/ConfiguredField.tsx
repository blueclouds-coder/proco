// useFieldConfig の結果に従って、項目の表示/非表示・必須マークを制御するラッパ。
import type { ReactNode } from 'react';
import { useFieldConfig, type ResolvedFieldConfig } from './useFieldConfig';

export interface ConfiguredFieldRenderProps {
  /** 設定から解決した必須フラグ */
  required: boolean;
  /** 設定の表示名（label_overrides 適用後）。無ければ呼び出し側の既定ラベル */
  label: string;
  config: ResolvedFieldConfig;
}

export interface ConfiguredFieldProps {
  screenId: string;
  fieldId: string;
  /** 設定に表示名が無い場合に使う既定ラベル（i18n 済み文字列） */
  defaultLabel: string;
  /** 子は render prop。required / label を受け取って入力部品を描画する */
  children: (props: ConfiguredFieldRenderProps) => ReactNode;
}

export function ConfiguredField({ screenId, fieldId, defaultLabel, children }: ConfiguredFieldProps) {
  const { get } = useFieldConfig(screenId);
  const config = get(fieldId);
  if (!config.visible) return null;
  return (
    <div data-field-id={fieldId} data-required={config.required || undefined}>
      {children({
        required: config.required,
        label: config.label ?? defaultLabel,
        config,
      })}
    </div>
  );
}
