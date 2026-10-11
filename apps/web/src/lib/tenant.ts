// ローカル開発用のテナント解決。
// 本番では JWT の cgid クレームがテナントを運ぶため、このヘッダは DEV ビルドでのみ付与する。

/** ローカル既定テナント（company_group）ID */
export const DEFAULT_DEV_TENANT_ID = '00000000-0000-0000-0000-000000000001';

export const TENANT_HEADER = 'X-Proco-Tenant';

/** 開発時に使うテナント ID（VITE_DEV_TENANT_ID > 既定値） */
export function getDevTenantId(): string {
  const fromEnv = import.meta.env.VITE_DEV_TENANT_ID?.trim();
  return fromEnv && fromEnv.length > 0 ? fromEnv : DEFAULT_DEV_TENANT_ID;
}

/** 全リクエストに付与するヘッダ。本番ビルドでは空 */
export function tenantHeaders(): Record<string, string> {
  if (!import.meta.env.DEV) return {};
  return { [TENANT_HEADER]: getDevTenantId() };
}
