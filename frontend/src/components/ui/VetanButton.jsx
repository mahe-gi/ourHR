import React from 'react';
import { Button, CircularProgress } from '@mui/material';

export default function VetanButton({
  children,
  loading = false,
  disabled = false,
  startIcon,
  endIcon,
  variant = 'contained',
  color = 'primary',
  size = 'medium',
  sx = {},
  ...props
}) {
  return (
    <Button
      variant={variant}
      color={color}
      size={size}
      disabled={disabled || loading}
      startIcon={!loading ? startIcon : undefined}
      endIcon={!loading ? endIcon : undefined}
      sx={{
        position: 'relative',
        fontWeight: 600,
        textTransform: 'none',
        borderRadius: '8px',
        ...sx,
      }}
      {...props}
    >
      {loading && (
        <CircularProgress
          size={18}
          color="inherit"
          sx={{
            position: 'absolute',
            left: '50%',
            marginLeft: '-9px',
          }}
        />
      )}
      <span style={{ opacity: loading ? 0 : 1 }}>{children}</span>
    </Button>
  );
}
