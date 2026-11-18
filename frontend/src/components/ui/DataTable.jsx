import React, { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Box,
  Typography,
  TextField,
  InputAdornment,
  Skeleton,
} from '@mui/material';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import FolderOpenOutlinedIcon from '@mui/icons-material/FolderOpenOutlined';
import VetanButton from './VetanButton';

export default function DataTable({
  columns = [],
  rows = [],
  loading = false,
  searchPlaceholder = 'Search records...',
  emptyTitle = 'No records found',
  emptySubtitle = 'Get started by creating a new entry.',
  emptyActionLabel,
  onEmptyAction,
  searchField = 'name',
}) {
  const [searchTerm, setSearchTerm] = useState('');

  const filteredRows = rows.filter((row) => {
    if (!searchTerm) return true;
    const val = row[searchField] || '';
    return String(val).toLowerCase().includes(searchTerm.toLowerCase());
  });

  return (
    <Paper
      sx={{
        backgroundColor: '#0F172A',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '10px',
        overflow: 'hidden',
      }}
    >
      <Box sx={{ p: 2, borderBottom: '1px solid rgba(255, 255, 255, 0.06)' }}>
        <TextField
          size="small"
          placeholder={searchPlaceholder}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchOutlinedIcon sx={{ color: 'text.secondary', fontSize: 18 }} />
              </InputAdornment>
            ),
          }}
          sx={{
            maxWidth: 320,
            '& .MuiOutlinedInput-root': {
              backgroundColor: '#1E293B',
              borderRadius: '6px',
              fontSize: '0.84rem',
              '& fieldset': { borderColor: 'rgba(255, 255, 255, 0.08)' },
            },
          }}
        />
      </Box>

      <TableContainer>
        <Table size="medium">
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell
                  key={col.field}
                  align={col.align || 'left'}
                  sx={{ width: col.width, minWidth: col.minWidth }}
                >
                  {col.headerName}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  {columns.map((col) => (
                    <TableCell key={col.field} align={col.align || 'left'}>
                      <Skeleton variant="text" sx={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }} />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : filteredRows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} sx={{ py: 6, textAlign: 'center' }}>
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <FolderOpenOutlinedIcon sx={{ fontSize: 44, color: 'text.secondary', mb: 1.5, opacity: 0.6 }} />
                    <Typography variant="subtitle1" sx={{ fontWeight: 600, color: 'text.primary', mb: 0.5 }}>
                      {emptyTitle}
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
                      {emptySubtitle}
                    </Typography>
                    {emptyActionLabel && onEmptyAction && (
                      <VetanButton variant="outlined" size="small" onClick={onEmptyAction}>
                        {emptyActionLabel}
                      </VetanButton>
                    )}
                  </Box>
                </TableCell>
              </TableRow>
            ) : (
              filteredRows.map((row, index) => (
                <TableRow key={row.id || index}>
                  {columns.map((col) => {
                    const value = row[col.field];
                    return (
                      <TableCell
                        key={col.field}
                        align={col.align || 'left'}
                        sx={col.isNumeric ? { fontVariantNumeric: 'tabular-nums', fontFamily: 'inherit' } : {}}
                      >
                        {col.renderCell ? col.renderCell({ row, value }) : value}
                      </TableCell>
                    );
                  })}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
}
