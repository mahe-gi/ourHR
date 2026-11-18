import React from 'react';
import { TextField, Typography, Box } from '@mui/material';

export default function VetanInput({
  label,
  error,
  helperText,
  fullWidth = true,
  size = 'small',
  sx = {},
  ...props
}) {
  return (
    <Box sx={{ mb: 2, width: fullWidth ? '100%' : 'auto' }}>
      {label && (
        <Typography
          variant="caption"
          sx={{
            display: 'block',
            mb: 0.75,
            color: error ? 'error.main' : 'text.secondary',
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
            fontSize: '0.72rem',
          }}
        >
          {label}
        </Typography>
      )}
      <TextField
        fullWidth={fullWidth}
        size={size}
        error={Boolean(error)}
        helperText={error || helperText}
        sx={{
          '& .MuiOutlinedInput-root': {
            backgroundColor: '#0F172A',
            borderRadius: '8px',
            '& fieldset': {
              borderColor: 'rgba(255, 255, 255, 0.12)',
            },
            '&:hover fieldset': {
              borderColor: 'rgba(255, 255, 255, 0.25)',
            },
            '&.Mui-focused fieldset': {
              borderColor: 'primary.main',
            },
          },
          '& .MuiInputBase-input': {
            color: '#F8FAFC',
            fontSize: '0.875rem',
            padding: '10px 14px',
          },
          ...sx,
        }}
        {...props}
      />
    </Box>
  );
}
