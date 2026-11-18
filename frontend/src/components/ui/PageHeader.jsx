import React from 'react';
import { Box, Typography } from '@mui/material';

export default function PageHeader({
  title,
  subtitle,
  action,
  breadcrumbs,
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: { xs: 'column', sm: 'row' },
        justifyContent: 'space-between',
        alignItems: { xs: 'flex-start', sm: 'center' },
        gap: 2,
        mb: 3,
      }}
    >
      <Box>
        {breadcrumbs && (
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 0.5 }}>
            {breadcrumbs}
          </Box>
        )}
        <Typography variant="h4" sx={{ fontWeight: 700, color: 'text.primary', letterSpacing: '-0.02em' }}>
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.25 }}>
            {subtitle}
          </Typography>
        )}
      </Box>
      {action && <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>{action}</Box>}
    </Box>
  );
}
