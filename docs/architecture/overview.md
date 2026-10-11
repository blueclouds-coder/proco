# システム構成

## 論理構成
```
[Web SPA (React)]  ──HTTPS──▶ [CloudFront + WAF] ──▶ [ALB → nginx-ingress]
                                                            │
        ┌───────────────────────────────────────────────────┴──────────────┐
        │ EKS (namespace: proco)                                             │
        │  service-oauth           認証・トークン（Spring Authorization Server）│
        │  service-master-common   税区分・権限・共通コード・テナント設定        │
        │  service-buyer-master    会社グループ・会社・組織・ユーザー・会計マスタ  │
        │  service-supplier-master サプライヤ情報・取引条件・取適法判定          │
        │  service-catalog         内部/外部カタログ・表示制御                  │
        │  service-search          商品横串検索（OpenSearch）                   │
        │  service-quote           見積（依頼・回答・選定・明細単位コントロール） │
        │  service-order           発注・受注・納品・検収・分納                   │
        │  service-closing         締め処理・支払通知                            │
        │  service-approval        承認ルールエンジン（全申請種別共通）           │
        │  service-notification    通知・督促・メールテンプレート               │
        │  service-file            ファイル・アップロード基盤・帳票              │
        │  service-schedule        ジョブ・連携スケジューラ                      │
        │  service-integration     SFTP/EDI/API アダプタ・マッピング・文字コード  │
        │  service-ai              RAG・NL→SQL・要約（Bedrock）                  │
        └───────────────────────────────────────────────────────────────────┘
                │                 │                │              │
        [Aurora PG17]       [Valkey]        [ActiveMQ]     [OpenSearch]
        RLS by tenant       cache/session   async events   search + k-NN
                │
        [S3: files, archive(Parquet, Object Lock)] ─▶ [Athena/Trino] ─▶ BI (Parquet mart)
        [Transfer Family SFTP] ─▶ service-integration
        [SES] ◀─ service-notification
        [Bedrock (Tokyo, VPC Endpoint)] ◀─ service-ai
```

既存 J2 との差分: `service-approval`（承認を各サービスから切り出し）、`service-integration`（連携基盤を共通化）、`service-ai` の3つが新設。`service-quote` と `service-order` は旧 buyer-order / supplier を業務単位で再編。

## テナント分離
- JWT クレーム `cgid`（company_group_id）→ `TenantContextFilter` が `SET LOCAL app.tenant`
- 全業務テーブルに `company_group_id NOT NULL` + RLS ポリシー `USING (company_group_id = current_setting('app.tenant')::uuid)`
- サービス間呼び出しはユーザーの JWT をそのまま伝搬（権限継承）。サービスアカウントは管理ジョブのみ

## 環境
| 環境 | 用途 | デプロイ |
|---|---|---|
| local | docker compose | 手動 |
| dev | 結合・E2E | main マージで自動（Argo CD） |
| stg | 受入・性能・顧客確認 | 手動承認 |
| prod | 本番 | 手動承認＋カナリア（Linkerd） |

## 可観測性
Micrometer → Prometheus/Grafana、構造化ログ → CloudWatch、Linkerd（mTLS・トレース）、CloudWatch Alarms → SNS → Slack。

## セキュリティ
docs/architecture/data-classification.md と CLAUDE.md §5 を参照。
