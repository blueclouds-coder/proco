/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** ローカル開発用テナント ID（本番では未使用） */
  readonly VITE_DEV_TENANT_ID?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
