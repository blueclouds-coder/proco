# テナント設定モデル（設定駆動の核）

目的: 顧客差分をコード分岐ではなく **データ** で表現する。すべての設定は `service-master-common` が所有し、各サービスはキャッシュ（Valkey）経由で参照する。

## 1. 設定の階層と優先順位
```
サービス既定値（コード内 default）
  < テナント（company_group）設定
    < 会社（company）設定
      < 組織（org_unit）設定
        < ユーザー設定（表示上の好みのみ。業務ルールは不可）
```
解決は「より狭いスコープが優先」。業務ルール（承認・採番・締め）はテナントまたは会社までで固定し、組織・ユーザーには開放しない。

## 2. 設定の種類

| 種別 | テーブル | 内容 | 変更者 | 反映 |
|---|---|---|---|---|
| フィーチャーフラグ | `feature_flags` | 機能の ON/OFF（アーカイブ照会、AIチャット、セルフ見積、プロジェクト購買…） | サービス提供会社 | 即時 |
| 画面項目設定 | `field_configs` | 画面×項目の 表示/非表示、必須/任意、初期値、表示順、表示名、入力制約（桁・正規表現・選択肢） | マスタ管理者 | 即時 |
| 表示名辞書 | `label_overrides` | 標準用語 → 顧客用語（内部指図→製番）。画面・帳票・DL・通知すべてに適用 | マスタ管理者 | 即時 |
| カスタム項目 | `custom_field_defs` + 各エンティティの `custom_values jsonb` | 案件/明細/サプライヤ/ユーザーに追加する項目の定義（型・必須・選択肢・表示条件・検索可否） | マスタ管理者 | 即時 |
| 承認ルール | `approval_rules` | 申請種別×条件（金額・品目カテゴリ・組織・カスタム項目値）→ 承認ステップ列（役職・グループ・引上げ可否・代理可否） | マスタ管理者 | 即時 |
| 採番ルール | `numbering_rules` | 案件・発注・帳票の番号書式（接頭辞・年度・連番・桁・リセット単位）、案件まとめキー | サービス提供会社 | 即時 |
| 帳票テンプレート | `document_templates` | 注文書・現品票・納品書・支払通知書のテンプレート（スロット H1/S1〜S8、タイトル切替、出力単位＝案件/明細、条件文言） | マスタ管理者 | 即時 |
| 通知テンプレート | `notification_templates` | メール件名・本文・宛先（CC/BCC）・送信条件（X日後・カレンダー基準） | マスタ管理者 | 即時 |
| 業務パラメータ | `business_params` | 見積依頼先上限、セルフ見積上限金額、検収日制約、締日・支払サイト、取適法閾値、DL上限 等。キー＋型付き値 | マスタ管理者／提供会社 | 即時 |
| カレンダー | `calendars` | 営業日・銀行休業日。拠点ごとに割当 | マスタ管理者 | 即時 |
| 連携定義 | `integration_profiles` | 接続先（SFTP/API）、スケジュール、ファイル規約（文字コード・改行・BOM・区切り）、マッピング定義ID、エラー通知先 | 提供会社 | 即時 |
| 機密区分 | `data_classification`（コード内定義＋テナント上書き） | 列ごとの public/internal/confidential/pii。AI・BI出力の制御に使う | 提供会社 | デプロイ |
| テーマ | `themes` | ロゴ・色・フォント | マスタ管理者 | 即時 |

## 3. field_configs の解決（フロント・バックエンド共通）
```
GET /api/v1/config/fields?screen=ORDER_REQUEST
→ [{ fieldId:"account_code", visible:true, required:true, label:"勘定科目",
     options:{source:"ACCOUNT_MASTER", filter:{entryType:"$form.entry_type"}},
     defaultValue:{source:"USER_PROFILE", key:"default_account"} }, ...]
```
- フロント: `useFieldConfig(screenId)` がこの配列からフォームを組み立てる。ハードコード禁止
- バックエンド: 同じ設定で必須チェック・制約チェックを行う（フロントの検証を信用しない）
- 条件付き表示（LION の入力種別→勘定科目階層）は `visibleWhen` / `optionsFilter` で表現する

## 4. カスタム項目（スロット方式を廃し、定義ベースに）
```sql
custom_field_defs(id, company_group_id, entity, field_key, label, type, required, options, visible_when, searchable, sort_order)
-- entity: CASE | LINE | SUPPLIER | USER | ORG
-- type: TEXT | NUMBER | DATE | SELECT | MULTISELECT | BOOLEAN | REFERENCE(master)
cases.custom_values jsonb   -- {"budget_no":"B-2026-014","meeting_body":"BOARD"}
```
- 検索可能項目は GIN インデックス。帳票・DL・AI出力にも定義ベースで載る
- 旧 J2 の「管理項目20枠」は移行時にこの定義へマップする

## 5. 承認ルール（ルールエンジン）
```yaml
rule:
  id: APR-ORDER-01
  applies_to: ORDER_REQUEST
  when: { amount_gte: 1000000, item_category_in: [...], custom:{budget_type: "CAPEX"} }
  steps:
    - { approver: { role: MANAGER, scope: REQUESTER_ORG }, skippable_by_higher: true }
    - { approver: { group: "PURCHASING_MANAGERS" }, mode: ANY_ONE }
  options: { allow_delegate: true, allow_pull_up: true, mobile: true }
```
最初に一致したルールを採用（優先度順）。評価ログを承認履歴に残す。

## 6. 拡張ポイント（設定で表現できないもの）
```java
public interface LineDerivationHook   { void derive(LineContext ctx); }      // コード導出（会社CD→工場 等）
public interface ValidationHook       { List<Problem> validate(FormContext ctx); }
public interface ImportMappingHook    { Record map(RawRecord in, IntegrationProfile p); }
public interface ExportFormatterHook  { byte[] format(ExportBatch b, IntegrationProfile p); }
public interface NumberingHook        { String next(NumberingContext ctx); }   // 製番など特殊採番
public interface PostEventHook        { void on(DomainEvent e); }              // 承認後・発注後の連携起動
```
- 実装は `extensions/<tenant>/` に置き、`tenant_hooks` テーブルで有効化する
- 標準コードは Hook インターフェースのみ知る。拡張が無効なら no-op
- 標準のバージョンアップで Hook インターフェースを変える場合は互換レイヤを用意する

## 7. 変更管理
- すべての設定変更は `config_audit` に who/when/before/after を記録
- 設定のエクスポート／インポート（JSON）を提供し、stg→prod の昇格と新規顧客の立ち上げに使う
- AI（マニュアルQA）は設定を根拠として参照できる（「当社では締めは20日」の出典が設定になる）
