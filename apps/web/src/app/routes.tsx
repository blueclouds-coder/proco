// ルート定義
import { Route, Routes } from 'react-router-dom';
import { Layout } from './Layout';
import { Home } from './Home';
import { CaseDetail } from './CaseDetail';
import { ConfigParams } from '../features/config/ConfigParams';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/cases/:id" element={<CaseDetail />} />
        <Route path="/config" element={<ConfigParams />} />
      </Route>
    </Routes>
  );
}
