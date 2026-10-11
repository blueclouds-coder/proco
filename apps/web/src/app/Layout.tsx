// 共通レイアウト（ヘッダ＋ナビ）。ナビは実 <a>（Link）で描画する。
import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material';
import { Link, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export function Layout() {
  const { t } = useTranslation();
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="static" color="primary" elevation={0}>
        <Toolbar>
          <Typography variant="h6" component="h1" sx={{ flexGrow: 1 }}>
            {t('app.title')}
          </Typography>
          <Button component={Link} to="/" color="inherit">
            {t('app.nav.home')}
          </Button>
          <Button component={Link} to="/config" color="inherit">
            {t('app.nav.config')}
          </Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" component="main" sx={{ py: 3 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
