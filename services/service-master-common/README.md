# service-master-common

テナント設定（業務パラメータ・フィーチャーフラグ等）を所有するサービス。**他サービスのひな形**でもある。

## 構成（新サービスはこれをコピーする）

```
openapi.yaml                                  契約（先に書く）
src/main/resources/db/migration/V1__init.sql  Flyway（RLS 必須）
src/main/resources/classification.yaml        列ごとの機密区分
jp.proco.mastercommon
  api/            Controller, ProblemDetailsHandler（RFC 9457）
  application/    Service（@Transactional、TenantContext からテナント取得）
  domain/         Record / ProcoException
  infrastructure/ Repository（jOOQ DSLContext, codegen なし）
    tenant/       TenantContext, TenantContextFilter, TenantTransactionAspect
    security/     SecurityConfig（jwt / dev）
```

## テナント分離の仕組み

1. `TenantContextFilter` が JWT の `cgid`（dev モードでは `X-Proco-Tenant` ヘッダ）を `TenantContext` に設定
2. `TenantTransactionAspect` が `@Transactional` メソッド先頭で `set_config('app.tenant', <uuid>, true)`（= `SET LOCAL`）を実行
3. 全テーブルの RLS ポリシー `tenant_isolation` が `company_group_id = app.tenant` を強制

### DB ロール（重要）

PostgreSQL の**スーパーユーザーとテーブル所有者は RLS を迂回**する（`FORCE ROW LEVEL SECURITY` でも所有者には適用されるが、スーパーユーザーには適用されない）。
そのため本番・ステージングでは:

- Flyway（マイグレーション）: 所有者ロール（例 `proco_migrate`）で実行
- アプリ: **非所有者のアプリ用ロール** `proco_app`（LOGIN、`USAGE ON SCHEMA`、`SELECT/INSERT/UPDATE/DELETE ON TABLES`）で接続

マイグレーション（V1）は GRANT を含まない。ロール作成と GRANT は環境構築（Terraform）の責務。
テスト（`PostgresTestSupport`）では Flyway をスーパーユーザーで流したあと `proco_app` を作成し、アプリの DataSource をそのロールで組み立てている。

## ローカル起動

```bash
docker compose up -d postgres          # リポジトリルート
cd services && mvn -q -pl service-master-common spring-boot:run -Dspring-boot.run.profiles=local
curl -H 'X-Proco-Tenant: 11111111-1111-1111-1111-111111111111' localhost:8082/api/v1/config/params
```

ローカル（`local` プロファイル）は `proco.security.mode=dev` + `dev-header-tenant=true`。本番既定は `jwt`。

## テスト

- `RlsIsolationTest`: 他テナントの行が見えない／更新できないことを `proco_app` ロールで検証
- `BusinessParamControllerTest`: dev ヘッダで PUT → GET
- Docker があれば Testcontainers（postgres:17）。なければ `PROCO_TEST_DB_URL`（既定 `localhost:5432/proco`, proco/proco）の既存 PostgreSQL に `proco_test` DB を作って使う。どちらも無い場合はスキップとして**明示的に**報告される（CI では Docker を必須にすること）
