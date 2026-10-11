// RFC 9457 Problem Details を Alert で表示する
import { Alert, AlertTitle } from '@mui/material';
import { toProblemDetails } from '@proco/api-client';

export function ProblemAlert({ error }: { error: unknown }) {
  const problem = toProblemDetails(error);
  return (
    <Alert severity="error" sx={{ mb: 2 }} role="alert">
      <AlertTitle>
        {problem.title}
        {problem.status ? ` (${problem.status})` : ''}
      </AlertTitle>
      {problem.detail}
    </Alert>
  );
}
