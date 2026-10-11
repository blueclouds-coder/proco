// アプリ全体で共有する API クライアント。fetch の直書きは禁止（CLAUDE.md §4）。
import { createClient } from '@proco/api-client';
import { tenantHeaders } from './tenant';

export const api = createClient({
  baseUrl: '', // 同一オリジン。開発時は Vite proxy が /api を 8082 へ転送
  headers: () => tenantHeaders(),
});
