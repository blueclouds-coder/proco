# @proco/api-client の生成について

CLAUDE.md の規約どおり、このパッケージは本来 **OpenAPI から自動生成**される（手書き禁止）。

- 入力: `services/<svc>/openapi.yaml`（契約先行。レビュー済みのものだけを使う）
- 生成: `openapi-typescript`（型）＋ `orval`（TanStack Query 用フック／fetch クライアント）
- 出力先: `src/generated/<svc>/`（git 管理対象。生成結果はレビューで差分を確認する）
- 実行: `pnpm --filter @proco/api-client generate`

## 現状（暫定）

`services/*/openapi.yaml` がまだ存在しないため、`src/index.ts` に**最小限の手書きクライアント**を置いている。
これは OpenAPI コード生成が配線されるまでの**つなぎ**であり、生成が有効になった時点で手書き部分は削除する。

手書きクライアントが扱うエンドポイント:

| メソッド | パス | 用途 |
|---|---|---|
| GET | `/api/v1/config/params` | 業務パラメータ一覧（`business_params`） |
| PUT | `/api/v1/config/params/{key}` | 業務パラメータ更新 |
| GET | `/api/v1/config/fields?screen=<id>` | 画面項目設定（`field_configs`） |

エラーは RFC 9457 Problem Details として `ProblemDetailsError` に変換して throw する。

## generate スクリプトの配線（TODO）

```jsonc
// package.json（配線後の想定）
"generate": "openapi-typescript ../../services/service-master-common/openapi.yaml -o src/generated/master-common/schema.d.ts && orval --config orval.config.ts"
```
