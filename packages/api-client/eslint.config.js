// @proco/api-client の ESLint 設定（flat config）
import js from '@eslint/js';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['node_modules', 'dist', 'generated'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  { files: ['**/*.ts'] },
);
