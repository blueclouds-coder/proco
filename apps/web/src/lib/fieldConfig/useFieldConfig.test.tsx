import type { ReactNode } from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProblemDetailsError } from '@proco/api-client';
import { api } from '../api';
import { useFieldConfig } from './useFieldConfig';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useFieldConfig', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('エンドポイントが 404 のとき全項目 visible=true のフォールバックを返す', async () => {
    vi.spyOn(api, 'getFieldConfig').mockRejectedValue(
      new ProblemDetailsError({ title: 'Not Found', status: 404 }),
    );
    const { result } = renderHook(() => useFieldConfig('ORDER_REQUEST'), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.isFallback).toBe(true);
    expect(result.current.get('account_code').visible).toBe(true);
    expect(result.current.get('account_code').required).toBe(false);
  });

  it('ネットワーク失敗時もフォールバックを返す', async () => {
    vi.spyOn(api, 'getFieldConfig').mockRejectedValue(new TypeError('Failed to fetch'));
    const { result } = renderHook(() => useFieldConfig('ORDER_REQUEST'), { wrapper });
    await waitFor(() => expect(result.current.isFallback).toBe(true));
    expect(result.current.get('anything').visible).toBe(true);
  });

  it('取得できた設定を fieldId でマップする', async () => {
    vi.spyOn(api, 'getFieldConfig').mockResolvedValue([
      { fieldId: 'account_code', visible: false, required: true, label: '勘定科目' },
    ]);
    const { result } = renderHook(() => useFieldConfig('ORDER_REQUEST'), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.isFallback).toBe(false);
    expect(result.current.get('account_code')).toMatchObject({ visible: false, required: true, label: '勘定科目' });
    expect(result.current.get('other').visible).toBe(true);
  });
});
