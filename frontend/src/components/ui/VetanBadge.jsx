import React from 'react';
import { Chip } from '@mui/material';

const STATUS_CONFIG = {
  ACTIVE: { bg: 'rgba(16, 185, 129, 0.15)', text: '#10B981', border: 'rgba(16, 185, 129, 0.3)' },
  APPROVED: { bg: 'rgba(16, 185, 129, 0.15)', text: '#10B981', border: 'rgba(16, 185, 129, 0.3)' },
  DISBURSED: { bg: 'rgba(16, 185, 129, 0.15)', text: '#10B981', border: 'rgba(16, 185, 129, 0.3)' },
  COMPLETED: { bg: 'rgba(16, 185, 129, 0.15)', text: '#10B981', border: 'rgba(16, 185, 129, 0.3)' },
  PENDING: { bg: 'rgba(245, 158, 11, 0.15)', text: '#F59E0B', border: 'rgba(245, 158, 11, 0.3)' },
  DRAFT: { bg: 'rgba(148, 163, 184, 0.15)', text: '#94A3B8', border: 'rgba(148, 163, 184, 0.3)' },
  CALCULATED: { bg: 'rgba(99, 102, 241, 0.15)', text: '#818CF8', border: 'rgba(99, 102, 241, 0.3)' },
  REJECTED: { bg: 'rgba(239, 68, 68, 0.15)', text: '#EF4444', border: 'rgba(239, 68, 68, 0.3)' },
  INACTIVE: { bg: 'rgba(239, 68, 68, 0.15)', text: '#EF4444', border: 'rgba(239, 68, 68, 0.3)' },
  LOP: { bg: 'rgba(239, 68, 68, 0.15)', text: '#EF4444', border: 'rgba(239, 68, 68, 0.3)' },
};

export default function VetanBadge({ status, label, size = 'small' }) {
  const normStatus = (status || '').toUpperCase();
  const config = STATUS_CONFIG[normStatus] || {
    bg: 'rgba(148, 163, 184, 0.15)',
    text: '#94A3B8',
    border: 'rgba(148, 163, 184, 0.3)',
  };

  return (
    <Chip
      size={size}
      label={label || normStatus}
      sx={{
        backgroundColor: config.bg,
        color: config.text,
        border: `1px solid ${config.border}`,
        fontWeight: 600,
        fontSize: '0.72rem',
        height: size === 'small' ? '22px' : '26px',
        borderRadius: '4px',
        letterSpacing: '0.03em',
      }}
    />
  );
}
