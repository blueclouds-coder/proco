// i18n 初期化。既定は ja、フォールバックも ja。UI 文言はすべてリソースに置く（CLAUDE.md §4）。
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import ja from './locales/ja.json';
import en from './locales/en.json';

export const resources = {
  ja: { translation: ja },
  en: { translation: en },
} as const;

if (!i18n.isInitialized) {
  void i18n.use(initReactI18next).init({
    resources,
    lng: 'ja',
    fallbackLng: 'ja',
    interpolation: { escapeValue: false },
  });
}

export default i18n;
