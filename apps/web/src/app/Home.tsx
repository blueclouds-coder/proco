// ホーム（マイタスク）。KPI カードは当面静的サンプル。将来は service から集計を取得する。
import { Card, CardContent, Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { SAMPLE_KPIS } from './homeKpis';

export function Home() {
  const { t } = useTranslation();
  return (
    <>
      <Typography variant="h5" component="h2" gutterBottom>
        {t('home.title')}
      </Typography>
      <Grid container spacing={2}>
        {SAMPLE_KPIS.map((kpi) => (
          <Grid key={kpi.id} size={{ xs: 12, sm: 6, md: 3 }}>
            <Card data-testid={`kpi-${kpi.id}`} sx={{ borderTop: 4, borderColor: `${kpi.color}.main` }}>
              <CardContent>
                <Typography variant="body2" color="text.secondary">
                  {t(kpi.labelKey)}
                </Typography>
                <Typography variant="h4" component="p" sx={{ fontWeight: 700 }}>
                  {kpi.count}
                  <Typography component="span" variant="body2" sx={{ ml: 0.5 }}>
                    {t('home.unit')}
                  </Typography>
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
