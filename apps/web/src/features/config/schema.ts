// 業務パラメータ更新フォームの zod スキーマ。
// エラーメッセージは i18n キーを入れ、表示側で t() する。
import { z } from 'zod';

export const PARAM_VALUE_TYPES = ['STRING', 'NUMBER', 'BOOLEAN', 'JSON'] as const;

export const paramFormSchema = z
  .object({
    key: z
      .string()
      .min(1, 'config.validation.keyRequired')
      .regex(/^[A-Z0-9_.]+$/, 'config.validation.keyFormat'),
    valueType: z.enum(PARAM_VALUE_TYPES),
    value: z.string().min(1, 'config.validation.valueRequired'),
  })
  .superRefine((data, ctx) => {
    // 型ごとの値チェック（バックエンドでも同じ検証を行う前提）
    switch (data.valueType) {
      case 'NUMBER':
        if (Number.isNaN(Number(data.value))) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['value'], message: 'config.validation.numberFormat' });
        }
        break;
      case 'BOOLEAN':
        if (!/^(true|false)$/i.test(data.value)) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['value'], message: 'config.validation.booleanFormat' });
        }
        break;
      case 'JSON':
        try {
          JSON.parse(data.value);
        } catch {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['value'], message: 'config.validation.jsonFormat' });
        }
        break;
      default:
        break;
    }
  });

export type ParamFormValues = z.infer<typeof paramFormSchema>;
