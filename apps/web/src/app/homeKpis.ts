// ホームの KPI 定義。静的サンプルデータ（TODO: API 接続時に置き換え）
export interface KpiItem {
  id: string;
  labelKey: string;
  count: number;
  /** MUI パレットのキー */
  color: 'primary' | 'secondary' | 'warning' | 'error';
}

export const SAMPLE_KPIS: KpiItem[] = [
  { id: 'pendingApproval', labelKey: 'home.kpi.pendingApproval', count: 5, color: 'primary' },
  { id: 'returned', labelKey: 'home.kpi.returned', count: 2, color: 'error' },
  { id: 'dueWithin3Days', labelKey: 'home.kpi.dueWithin3Days', count: 3, color: 'warning' },
  { id: 'awaitingReply', labelKey: 'home.kpi.awaitingReply', count: 7, color: 'secondary' },
];
