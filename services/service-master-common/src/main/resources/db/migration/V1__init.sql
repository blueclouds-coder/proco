-- service-master-common 初期スキーマ
-- 方針:
--   * ポリシーは NULLIF(current_setting('app.tenant', true), '') を使う。SET LOCAL 後にプール接続へ戻ると
--     値が '' にリセットされ、uuid キャストでエラーになるため。未設定時は NULL → どの行にも一致しない（fail-closed）
--   * 全テーブルに company_group_id を持ち、RLS（app.tenant）で強制分離する
--   * FORCE ROW LEVEL SECURITY によりテーブル所有者にもポリシーを適用する（ただしスーパーユーザーは常に迂回する）
--   * このマイグレーションは GRANT を行わない。アプリ用ロール（非所有者）の作成と GRANT は環境構築側の責務
--     （本番: Terraform / ローカル・テスト: README と RlsIsolationTest を参照）

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 業務パラメータ（キー＋型付き値）
CREATE TABLE business_params (
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    company_group_id uuid        NOT NULL,
    param_key        text        NOT NULL,
    value_type       text        NOT NULL CHECK (value_type IN ('STRING', 'INT', 'DECIMAL', 'BOOL', 'JSON')),
    value            jsonb       NOT NULL,
    description      text,
    created_at       timestamptz NOT NULL DEFAULT now(),
    created_by       text,
    updated_at       timestamptz NOT NULL DEFAULT now(),
    updated_by       text,
    version          int         NOT NULL DEFAULT 0,
    UNIQUE (company_group_id, param_key)
);

ALTER TABLE business_params ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_params FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON business_params
    USING (company_group_id = NULLIF(current_setting('app.tenant', true), '')::uuid)
    WITH CHECK (company_group_id = NULLIF(current_setting('app.tenant', true), '')::uuid);

-- フィーチャーフラグ（機能の ON/OFF）
CREATE TABLE feature_flags (
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    company_group_id uuid        NOT NULL,
    flag_key         text        NOT NULL,
    enabled          boolean     NOT NULL DEFAULT false,
    created_at       timestamptz NOT NULL DEFAULT now(),
    created_by       text,
    updated_at       timestamptz NOT NULL DEFAULT now(),
    updated_by       text,
    version          int         NOT NULL DEFAULT 0,
    UNIQUE (company_group_id, flag_key)
);

ALTER TABLE feature_flags ENABLE ROW LEVEL SECURITY;
ALTER TABLE feature_flags FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON feature_flags
    USING (company_group_id = NULLIF(current_setting('app.tenant', true), '')::uuid)
    WITH CHECK (company_group_id = NULLIF(current_setting('app.tenant', true), '')::uuid);
