// Proco 共通 MUI テーマ。顧客ごとの色差分は themes 設定（tenant-config.md）で上書きする想定。
import { createTheme } from '@mui/material/styles';
import { tokens } from './tokens';

export const theme = createTheme({
  palette: {
    primary: { main: tokens.color.primary },
    secondary: { main: tokens.color.secondary },
    warning: { main: tokens.color.warning },
    error: { main: tokens.color.error },
    background: { default: tokens.color.background, paper: tokens.color.surface },
    text: { primary: tokens.color.textPrimary, secondary: tokens.color.textSecondary },
  },
  typography: {
    fontFamily: tokens.font.family,
  },
  shape: {
    borderRadius: tokens.radius.md,
  },
  components: {
    // 44px タッチ領域を既定にする
    MuiButton: {
      styleOverrides: {
        root: { minHeight: tokens.touchTarget, textTransform: 'none' },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: { minWidth: tokens.touchTarget, minHeight: tokens.touchTarget },
      },
    },
  },
});

export type ProcoTheme = typeof theme;
