import { render, screen } from '@testing-library/react';
import { ThemeProvider } from '@mui/material';
import { theme } from '@proco/ui';
import { Home } from './Home';

describe('Home', () => {
  it('4 つの KPI カードを描画する', () => {
    render(
      <ThemeProvider theme={theme}>
        <Home />
      </ThemeProvider>,
    );
    expect(screen.getByText('承認待ち')).toBeInTheDocument();
    expect(screen.getByText('差戻し')).toBeInTheDocument();
    expect(screen.getByText('納期3日以内')).toBeInTheDocument();
    expect(screen.getByText('回答待ち')).toBeInTheDocument();
    expect(screen.getAllByTestId(/^kpi-/)).toHaveLength(4);
  });
});
