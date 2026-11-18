import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

export default function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  color = 'primary.main',
}) {
  return (
    <Paper
      sx={{
        p: 2.5,
        backgroundColor: '#0F172A',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '10px',
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
      }}
    >
      <Box>
        <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
          {title}
        </Typography>
        <Typography
          variant="h4"
          sx={{
            fontWeight: 700,
            color: 'text.primary',
            mt: 0.5,
            mb: 0.25,
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {value}
        </Typography>
        {subtitle && (
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
            {subtitle}
          </Typography>
        )}
      </Box>
      {Icon && (
        <Box
          sx={{
            width: 42,
            height: 42,
            borderRadius: '8px',
            backgroundColor: 'rgba(255, 255, 255, 0.03)',
            border: '1px solid rgba(255, 255, 255, 0.06)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: color,
          }}
        >
          <Icon sx={{ fontSize: 22 }} />
        </Box>
      )}
    </Paper>
  );
}
