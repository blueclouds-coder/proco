// 業務パラメータ管理画面: 一覧（GET）と更新フォーム（PUT）
import { useState } from 'react';
import { Alert, CircularProgress, Paper, Snackbar, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { api } from '../../lib/api';
import { useFieldConfig } from '../../lib/fieldConfig';
import { CONFIG_PARAMS_SCREEN_ID, ParamForm } from './ParamForm';
import { ParamsTable } from './ParamsTable';
import { ProblemAlert } from './ProblemAlert';
import type { ParamFormValues } from './schema';

const PARAMS_QUERY_KEY = ['config', 'params'] as const;

export function ConfigParams() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [saved, setSaved] = useState(false);
  const { isFallback } = useFieldConfig(CONFIG_PARAMS_SCREEN_ID);

  const list = useQuery({
    queryKey: PARAMS_QUERY_KEY,
    queryFn: () => api.listParams(),
  });

  const put = useMutation({
    mutationFn: (values: ParamFormValues) => api.putParam(values.key, values),
    onSuccess: async () => {
      setSaved(true);
      await queryClient.invalidateQueries({ queryKey: PARAMS_QUERY_KEY });
    },
  });

  return (
    <>
      <Typography variant="h5" component="h2" gutterBottom>
        {t('config.title')}
      </Typography>

      {isFallback && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {t('config.fallbackNotice')}
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="subtitle1" component="h3" gutterBottom>
          {t('config.form.title')}
        </Typography>
        {put.isError && <ProblemAlert error={put.error} />}
        <ParamForm
          submitting={put.isPending}
          onSubmit={async (values) => {
            try {
              await put.mutateAsync(values);
            } catch {
              // エラーは put.error として ProblemAlert に表示する
            }
          }}
        />
      </Paper>

      <Paper sx={{ p: 2 }}>
        {list.isLoading && <CircularProgress aria-label={t('common.loading')} />}
        {list.isError && <ProblemAlert error={list.error} />}
        {list.data && <ParamsTable data={list.data} />}
      </Paper>

      <Snackbar
        open={saved}
        autoHideDuration={3000}
        onClose={() => setSaved(false)}
        message={t('config.form.saved')}
      />
    </>
  );
}
