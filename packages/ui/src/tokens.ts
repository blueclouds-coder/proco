// デザイントークン。色・フォント・角丸などの「生の値」はここだけに置き、
// MUI テーマ（theme.ts）はこのトークンから組み立てる。
export const tokens = {
  color: {
    primary: '#1F3A5F',
    secondary: '#0E7C7B',
    warning: '#B45309',
    error: '#B42318',
    background: '#F3F5F8',
    surface: '#FFFFFF',
    textPrimary: '#1A1F2B',
    textSecondary: '#5B6472',
    border: '#D9DEE6',
  },
  font: {
    family: "'Noto Sans JP', 'Hiragino Sans', 'Yu Gothic', sans-serif",
  },
  radius: {
    md: 10,
  },
  /** タッチ領域の最小サイズ（アクセシビリティ要件: 44px） */
  touchTarget: 44,
} as const;

export type Tokens = typeof tokens;
