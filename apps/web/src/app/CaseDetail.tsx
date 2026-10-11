// 案件詳細（プレースホルダ）
import { Paper, Typography } from '@mui/material';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export function CaseDetail() {
  const { id } = useParams<{ id: string }>();
  const { t } = useTranslation();
  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" component="h2" gutterBottom>
        {t('case.title')}
      </Typography>
      <Typography>{t('case.placeholder', { id })}</Typography>
    </Paper>
  );
}
