// 最小限の手書き HTTP クライアント。
// 注意: これは OpenAPI コード生成（openapi/README.md）が配線されるまでのつなぎ実装。
import { ProblemDetailsError, type ProblemDetails } from './problem';
import type { ConfigParamResponse, FieldConfigResponse, PutConfigParamRequest } from './types';

export interface ClientOptions {
  /** 例: '' （同一オリジン＋Vite proxy）または 'https://api.example.com' */
  baseUrl: string;
  /** 全リクエストに付与するヘッダ。関数の場合は毎回評価する（トークン更新対応） */
  headers?: Record<string, string> | (() => Record<string, string>);
  /** テスト差し替え用。省略時は globalThis.fetch */
  fetch?: typeof fetch;
}

export interface ProcoApiClient {
  listParams(): Promise<ConfigParamResponse[]>;
  putParam(key: string, body: PutConfigParamRequest): Promise<ConfigParamResponse>;
  getFieldConfig(screen: string): Promise<FieldConfigResponse[]>;
}

async function parseProblem(res: Response): Promise<ProblemDetails> {
  const contentType = res.headers.get('content-type') ?? '';
  if (contentType.includes('json')) {
    try {
      const body = (await res.json()) as Partial<ProblemDetails>;
      return {
        ...body,
        title: body.title ?? res.statusText ?? 'Error',
        status: body.status ?? res.status,
      };
    } catch {
      // JSON として読めない場合は下のフォールバックへ
    }
  }
  return { title: res.statusText || 'Error', status: res.status };
}

export function createClient(options: ClientOptions): ProcoApiClient {
  const fetchImpl = options.fetch ?? globalThis.fetch.bind(globalThis);
  const base = options.baseUrl.replace(/\/$/, '');

  const resolveHeaders = (): Record<string, string> =>
    typeof options.headers === 'function' ? options.headers() : (options.headers ?? {});

  async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const res = await fetchImpl(`${base}${path}`, {
      method,
      headers: {
        Accept: 'application/json, application/problem+json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...resolveHeaders(),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) {
      throw new ProblemDetailsError(await parseProblem(res));
    }
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  }

  return {
    listParams: () => request<ConfigParamResponse[]>('GET', '/api/v1/config/params'),
    putParam: (key, body) =>
      request<ConfigParamResponse>('PUT', `/api/v1/config/params/${encodeURIComponent(key)}`, body),
    getFieldConfig: (screen) =>
      request<FieldConfigResponse[]>(
        'GET',
        `/api/v1/config/fields?screen=${encodeURIComponent(screen)}`,
      ),
  };
}
