import React from 'react';
import { Box, Typography, LinearProgress, Paper } from '@mui/material';
import VerifiedUserOutlinedIcon from '@mui/icons-material/VerifiedUserOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';

export default function WageCodeValidator({
  basic = 0,
  grossSalary = 0,
  onAutoFix,
}) {
  const numericBasic = Number(basic) || 0;
  const numericGross = Number(grossSalary) || 0;
  
  const ratio = numericGross > 0 ? (numericBasic / numericGross) * 100 : 0;
  const isCompliant = ratio >= 50;
  const isClose = ratio >= 45 && ratio < 50;

  const color = isCompliant ? '#10B981' : isClose ? '#F59E0B' : '#EF4444';
  const statusLabel = isCompliant
    ? 'Compliant with Code on Wages'
    : 'Non-Compliant: Basic Wage below 50%';

  return (
    <Paper
      sx={{
        p: 2,
        backgroundColor: '#0F172A',
        border: `1px solid ${isCompliant ? 'rgba(16, 185, 129, 0.25)' : 'rgba(239, 68, 68, 0.25)'}`,
        borderRadius: '8px',
      }}
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isCompliant ? (
            <VerifiedUserOutlinedIcon sx={{ color: '#10B981', fontSize: 20 }} />
          ) : (
            <WarningAmberOutlinedIcon sx={{ color: '#EF4444', fontSize: 20 }} />
          )}
          <Typography variant="subtitle2" sx={{ fontWeight: 600, color: 'text.primary' }}>
            50% Wage Code Ratio
          </Typography>
        </Box>
        <Typography
          variant="h6"
          sx={{
            fontWeight: 700,
            color: color,
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {ratio.toFixed(1)}%
        </Typography>
      </Box>

      <LinearProgress
        variant="determinate"
        value={Math.min(ratio, 100)}
        sx={{
          height: 6,
          borderRadius: 3,
          backgroundColor: 'rgba(255, 255, 255, 0.08)',
          '& .MuiLinearProgress-bar': {
            backgroundColor: color,
            borderRadius: 3,
          },
          mb: 1,
        }}
      />

      <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
        {statusLabel}. Under the Indian Code on Wages, Basic pay + DA must constitute at least 50% of total gross wages to prevent statutory liabilities.
      </Typography>
    </Paper>
  );
}
