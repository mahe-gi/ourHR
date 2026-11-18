import React from 'react';
import { Card, CardHeader, CardContent, Divider, Box, Typography } from '@mui/material';

export default function VetanCard({
  title,
  subtitle,
  action,
  children,
  noPadding = false,
  sx = {},
  ...props
}) {
  return (
    <Card
      sx={{
        backgroundColor: '#0F172A',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '10px',
        overflow: 'hidden',
        ...sx,
      }}
      {...props}
    >
      {(title || action) && (
        <>
          <CardHeader
            title={
              typeof title === 'string' ? (
                <Typography variant="h6" sx={{ fontWeight: 600, color: 'text.primary' }}>
                  {title}
                </Typography>
              ) : (
                title
              )
            }
            subheader={
              subtitle && (
                <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.25 }}>
                  {subtitle}
                </Typography>
              )
            }
            action={action}
            sx={{ px: 2.5, py: 1.75 }}
          />
          <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.06)' }} />
        </>
      )}
      <CardContent sx={{ p: noPadding ? 0 : 2.5, '&:last-child': { pb: noPadding ? 0 : 2.5 } }}>
        {children}
      </CardContent>
    </Card>
  );
}
