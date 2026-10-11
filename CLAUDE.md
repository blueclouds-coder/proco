# Proco — 開発ルール（全エージェント共通）

Proco は購買（調達）SaaS。Jienie 2.0 の標準マニュアルを出発点に、UI・業務機能を刷新し、
AI 機能（マニュアルQA、BI向けデータ出力、案件要約）を組み込む。**標準機能は1本のコードで全顧客に提供し、顧客差分はテナント設定と拡張ポイントで吸収する。**

## 1. 最優先の原則

1. **テナント分離はDBで強制する。** すべての業務テーブルに `company_group_id` を持ち、PostgreSQL の Row Level Security を有効にする。アプリ側の WHERE 句だけに頼らない。
2. **顧客名でコードを分岐しない。** `if (tenant == "LION")` は禁止。差分は `tenant_settings`（docs/architecture/tenant-config.md）か `extensions/<tenant>/` の拡張ポイント実装で表現する。
3. **AI は提案のみ。** AI（service-ai）は書き込み API を持たない。承認・発注・マスタ更新などの確定操作は人がボタンを押す。AI への入力は常にログインユーザーの権限で取得し、マスタの個人情報・口座・連絡先列は AI 用ビューに含めない。
4. **契約先行。** 画面も実装も OpenAPI（`services/<svc>/openapi.yaml`）と DB マイグレーション（Flyway）を先に書き、レビューを通してから実装する。
5. **人が判断するものを勝手に決めない。** 業務ルールの解釈が分かれる点は `docs/decisions/` に ADR として起票し、未決のまま実装を進めない。

## 2. リポジトリ構成

```
apps/web                 React 19 + TypeScript + Vite（購買・依頼・サプライヤ共通SPA、ロール別ホーム）
packages/ui              デザインシステム（トークン、共通コンポーネント、Storybook）
packages/api-client      OpenAPI から自動生成した TypeScript クライアント（手書き禁止）
services/service-*       Spring Boot 3.x / Java 21 マイクロサービス（1サービス = 1 DBスキーマ）
extensions/<tenant>      顧客固有の拡張ポイント実装（標準コードを改変しない）
infra/terraform          AWS（VPC, EKS, Aurora, ElastiCache, MQ, S3, CloudFront, WAF, SES, Transfer Family）
infra/helm               サービス共通 Helm チャート
docs/                    要件・ドメイン・アーキテクチャ・ADR
```

## 3. 技術スタック（変更する場合は ADR 必須）

- Backend: Java 21, Spring Boot 3.x, Spring Security (Resource Server), Flyway, jOOQ, springdoc-openapi, Micrometer
- DB: PostgreSQL 17（本番 Aurora）。キャッシュ Valkey。メッセージ ActiveMQ。検索 OpenSearch。分析 Trino/Athena。
- Frontend: React 19, TypeScript, Vite, MUI, TanStack Query/Table, react-hook-form + zod, i18next (ja/en), Vitest, Playwright
- AI: Amazon Bedrock（Claude, 東京リージョン, VPC Endpoint）、RAG は OpenSearch k-NN
- ローカル: `docker compose up` で PostgreSQL / Valkey / ActiveMQ / OpenSearch / LocalStack が起動

## 4. コーディング規約

### 共通
- 言語: コード・識別子は英語、コメント・ドキュメント・UI文言は日本語（UI文言は i18n リソースに置く）
- 命名: サービス `service-<domain>`、テーブル `snake_case`、API パス `/api/v1/<resource>`、DTO は `XxxRequest` / `XxxResponse`
- 日付は ISO 8601、タイムゾーンは UTC で保存し表示時に JST。終了日の「無期限」は `9999-12-31`
- 金額は `numeric(18,2)`、通貨コードを必ず併記（多通貨対応のため）
- 監査列を全テーブルに: `created_at, created_by, updated_at, updated_by, version(楽観ロック)`
- 削除は論理削除＋履歴。案件・明細・承認は訂正削除履歴方式（更新は履歴 INSERT）

### Backend
- パッケージ: `jp.proco.<domain>.{api,application,domain,infrastructure}`（ヘキサゴナル）
- 例外は `ProcoException(code, messageKey)` に統一し、API は RFC 9457 Problem Details で返す
- テナントは JWT の `cgid` クレーム → `SET LOCAL app.tenant` を各トランザクション先頭で実行（`TenantContextFilter`）
- 外部連携は `integration` パッケージのアダプタ経由のみ。レイアウト固有ロジックは `extensions/` へ
- テスト: ユニット JUnit5 + Testcontainers（PostgreSQL 実物）。RLS のテストを必ず書く（他テナントの行が見えないこと）

### Frontend
- 画面は `apps/web/src/features/<feature>/` に集約。共通部品は `packages/ui`
- API 呼び出しは `packages/api-client` の生成コードのみ。`fetch` 直書き禁止
- 表示/必須/表示名は `useFieldConfig(screenId, fieldId)` でテナント設定から解決する（ハードコード禁止）
- アクセシビリティ: 実 `<button>`/`<a>`、`label` 付き入力、44px タッチ領域、コントラスト 4.5:1

## 5. セキュリティ要件（必須）

- 機密区分タグ: 列メタデータに `public / internal / confidential / pii` を定義（`docs/architecture/data-classification.md`）
- `pii` と `confidential` 列は、AI 用ビュー・BI 出力・ログに含めない。BI 出力で `confidential` が必要な場合は管理者承認フロー
- 生成 SQL は実行前に静的検査（SELECT のみ・許可ビューのみ・`SELECT *` 禁止・テナント条件固定）
- 秘密情報は AWS Secrets Manager。リポジトリに `.env` 実体を置かない（`.env.example` のみ）
- 全 AI 質問・生成 SQL・拒否イベントを監査ログに記録

## 6. 作業の進め方

1. タスクは `docs/requirements/` の要件 ID（例 `REQ-QUOTE-012`）に紐づける
2. 仕様アナリストが受入条件（Gherkin）を書く → アーキテクトが OpenAPI/マイグレーション → 実装 → QA が E2E
3. PR は別インスタンスのレビュアーが確認。セルフマージ禁止
4. コミットメッセージは `feat(quote): 明細単位の依頼先選択 (REQ-QUOTE-012)` の形式
5. 不明点はコードに `TODO(question)` を残さず、ADR か Issue に起票する

## 7. 参照すべきドキュメント

- `docs/requirements/standard-feature-candidates.md` — 標準機能候補10本（複数顧客で重複する要件）
- `docs/requirements/fit-gap-summary.md` — 顧客別の標準化/設定化/個別拡張の仕分け
- `docs/domain/glossary.md` — 用語集（エンティティ・ステータス・ロール）
- `docs/architecture/tenant-config.md` — テナント設定モデル
- `docs/architecture/overview.md` — システム構成
