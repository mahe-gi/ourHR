import React, { useState } from 'react';
import { Box, Typography, IconButton, Tooltip } from '@mui/material';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined';

export default function KycMaskedViewer({
  label = 'Aadhaar',
  value = '',
  type = 'aadhaar',
}) {
  const [revealed, setRevealed] = useState(false);
  const [copied, setCopied] = useState(false);

  const formatMasked = () => {
    if (!value) return '[Aadhaar Redacted]';
    if (type === 'aadhaar') {
      const clean = String(value).replace(/\s/g, '');
      const lastFour = clean.length >= 4 ? clean.slice(-4) : 'XXXX';
      return revealed ? clean : `XXXX-XXXX-${lastFour}`;
    }
    if (type === 'pan') {
      const clean = String(value).trim().toUpperCase();
      if (clean.length === 10) {
        return revealed ? clean : `${clean.slice(0, 5)}****${clean.slice(9)}`;
      }
      return revealed ? clean : 'XXXXX****X';
    }
    return revealed ? value : '********';
  };

  const handleCopy = () => {
    navigator.clipboard?.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 1,
        px: 1.5,
        py: 0.5,
        backgroundColor: '#1E293B',
        borderRadius: '6px',
        border: '1px solid rgba(255, 255, 255, 0.08)',
      }}
    >
      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
        {label}:
      </Typography>
      <Typography
        variant="body2"
        sx={{
          fontFamily: 'monospace',
          fontWeight: 600,
          color: '#F8FAFC',
          letterSpacing: '0.05em',
        }}
      >
        {formatMasked()}
      </Typography>
      <Tooltip title={revealed ? 'Hide sensitive ID' : 'Reveal sensitive ID'}>
        <IconButton aria-label={revealed ? 'Hide sensitive ID' : 'Reveal sensitive ID'} size="small" onClick={() => setRevealed(!revealed)} sx={{ color: 'text.secondary', p: 0.5 }}>
          {revealed ? (
            <VisibilityOffOutlinedIcon sx={{ fontSize: 16 }} />
          ) : (
            <VisibilityOutlinedIcon sx={{ fontSize: 16 }} />
          )}
        </IconButton>
      </Tooltip>
      <Tooltip title={copied ? 'Copied' : 'Copy ID'}>
        <IconButton aria-label="Copy identification" size="small" onClick={handleCopy} sx={{ color: 'text.secondary', p: 0.5 }}>
          <ContentCopyOutlinedIcon sx={{ fontSize: 16 }} />
        </IconButton>
      </Tooltip>
    </Box>
  );
}
