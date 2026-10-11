// 業務パラメータの登録・更新フォーム（react-hook-form + zod）
import { Box, Button, MenuItem, Stack, TextField } from '@mui/material';
import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { ConfiguredField } from '../../lib/fieldConfig';
import { PARAM_VALUE_TYPES, paramFormSchema, type ParamFormValues } from './schema';

export const CONFIG_PARAMS_SCREEN_ID = 'CONFIG_PARAMS';

export interface ParamFormProps {
  onSubmit: (values: ParamFormValues) => Promise<void> | void;
  submitting?: boolean;
}

export function ParamForm({ onSubmit, submitting }: ParamFormProps) {
  const { t } = useTranslation();
  const { control, handleSubmit, reset } = useForm<ParamFormValues>({
    resolver: zodResolver(paramFormSchema),
    defaultValues: { key: '', valueType: 'STRING', value: '' },
  });

  return (
    <Box
      component="form"
      noValidate
      onSubmit={handleSubmit(async (values) => {
        await onSubmit(values);
        reset();
      })}
    >
      <Stack spacing={2} direction={{ xs: 'column', md: 'row' }} alignItems="flex-start">
        <ConfiguredField screenId={CONFIG_PARAMS_SCREEN_ID} fieldId="key" defaultLabel={t('config.form.key')}>
          {({ label, required }) => (
            <Controller
              name="key"
              control={control}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  id="param-key"
                  label={label}
                  required={required}
                  error={!!fieldState.error}
                  helperText={fieldState.error ? t(fieldState.error.message ?? '') : ' '}
                  size="small"
                />
              )}
            />
          )}
        </ConfiguredField>
        <ConfiguredField
          screenId={CONFIG_PARAMS_SCREEN_ID}
          fieldId="valueType"
          defaultLabel={t('config.form.valueType')}
        >
          {({ label, required }) => (
            <Controller
              name="valueType"
              control={control}
              render={({ field }) => (
                <TextField {...field} id="param-value-type" select label={label} required={required} size="small" sx={{ minWidth: 140 }}>
                  {PARAM_VALUE_TYPES.map((vt) => (
                    <MenuItem key={vt} value={vt}>
                      {vt}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
          )}
        </ConfiguredField>
        <ConfiguredField screenId={CONFIG_PARAMS_SCREEN_ID} fieldId="value" defaultLabel={t('config.form.value')}>
          {({ label, required }) => (
            <Controller
              name="value"
              control={control}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  id="param-value"
                  label={label}
                  required={required}
                  error={!!fieldState.error}
                  helperText={fieldState.error ? t(fieldState.error.message ?? '') : ' '}
                  size="small"
                  sx={{ minWidth: 260 }}
                />
              )}
            />
          )}
        </ConfiguredField>
        <Button type="submit" variant="contained" disabled={submitting}>
          {t('config.form.submit')}
        </Button>
      </Stack>
    </Box>
  );
}
