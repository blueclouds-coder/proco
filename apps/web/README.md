# apps/web

Proco の SPA（React 19 + TypeScript + Vite + MUI）。購買・依頼・サプライヤ共通、ロール別ホーム。

## 起動

```bash
pnpm install
pnpm --filter web dev      # http://localhost:5173
pnpm --filter web test     # Vitest
pnpm --filter web lint
pnpm --filter web build
```

`/api/*` への呼び出しは Vite の dev proxy で `http://localhost:8082`（service-master-common）へ転送される。

## テナントの扱い（ローカル開発）

- ローカル開発では **全リクエストに `X-Proco-Tenant` ヘッダ**を付与する（`src/lib/tenant.ts`）。
- 値は環境変数 `VITE_DEV_TENANT_ID`。未設定時の既定値は `00000000-0000-0000-0000-000000000001`。
  `.env.example` を `.env` にコピーして変更できる（`.env` は git 管理外）。
- **本番ではこのヘッダは使わない。** テナントは JWT の `cgid` クレームで運ばれ、バックエンドの
  `TenantContextFilter` が `SET LOCAL app.tenant` を実行し RLS で分離する（CLAUDE.md §4）。
  `import.meta.env.DEV` が false のビルドではヘッダを付与しない。

## 構成

```
src/app/              App（ThemeProvider / QueryClient / Router）とルート画面
src/features/<f>/     機能ごとの画面（config: 業務パラメータ管理）
src/lib/api.ts        @proco/api-client のインスタンス（fetch 直書き禁止）
src/lib/tenant.ts     ローカル用テナントヘッダ
src/lib/fieldConfig/  useFieldConfig(screenId) と <ConfiguredField>（表示/必須/表示名の設定駆動）
src/i18n/             ja（既定）/ en リソース
```

## 画面項目設定（useFieldConfig）

`GET /api/v1/config/fields?screen=<SCREEN_ID>` の結果から `fieldId -> {visible, required, label, ...}`
を解決する。エンドポイント未実装（404）やネットワーク失敗時は **全項目 visible=true / required=false**
のローカルフォールバックで描画する。画面は `<ConfiguredField screenId fieldId>` で項目を包み、
表示/非表示・必須をハードコードしない。
