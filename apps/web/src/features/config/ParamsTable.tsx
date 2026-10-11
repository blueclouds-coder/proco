// 業務パラメータ一覧（TanStack Table）
import { useMemo } from 'react';
import { Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { createColumnHelper, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';
import { useTranslation } from 'react-i18next';
import type { ConfigParamResponse } from '@proco/api-client';

const columnHelper = createColumnHelper<ConfigParamResponse>();

export function ParamsTable({ data }: { data: ConfigParamResponse[] }) {
  const { t } = useTranslation();
  const columns = useMemo(
    () => [
      columnHelper.accessor('key', { header: t('config.table.key') }),
      columnHelper.accessor('valueType', { header: t('config.table.valueType') }),
      columnHelper.accessor('value', { header: t('config.table.value') }),
      columnHelper.accessor('description', {
        header: t('config.table.description'),
        cell: (info) => info.getValue() ?? '',
      }),
      columnHelper.accessor('updatedAt', {
        header: t('config.table.updatedAt'),
        // UTC 保存 → JST 表示
        cell: (info) => {
          const v = info.getValue();
          return v ? new Date(v).toLocaleString('ja-JP', { timeZone: 'Asia/Tokyo' }) : '';
        },
      }),
    ],
    [t],
  );

  const table = useReactTable({ data, columns, getCoreRowModel: getCoreRowModel() });

  if (data.length === 0) {
    return <Typography color="text.secondary">{t('config.table.empty')}</Typography>;
  }

  return (
    <Table size="small" aria-label={t('config.title')}>
      <TableHead>
        {table.getHeaderGroups().map((hg) => (
          <TableRow key={hg.id}>
            {hg.headers.map((h) => (
              <TableCell key={h.id}>{flexRender(h.column.columnDef.header, h.getContext())}</TableCell>
            ))}
          </TableRow>
        ))}
      </TableHead>
      <TableBody>
        {table.getRowModel().rows.map((row) => (
          <TableRow key={row.id}>
            {row.getVisibleCells().map((cell) => (
              <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
