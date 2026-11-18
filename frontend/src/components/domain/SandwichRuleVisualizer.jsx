import React from 'react';
import { Box, Typography, Paper } from '@mui/material';
import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

export default function SandwichRuleVisualizer({
  fromDate = '2026-11-20',
  toDate = '2026-11-23',
  isSandwichTriggered = true,
}) {
  const days = [
    { day: 'Thu', date: 'Nov 19', type: 'WORK', label: 'Present', color: '#10B981' },
    { day: 'Fri', date: 'Nov 20', type: 'LEAVE', label: 'Leave', color: '#6366F1' },
    { day: 'Sat', date: 'Nov 21', type: 'SANDWICH_LOP', label: 'Sandwich LOP', color: '#EF4444' },
    { day: 'Sun', date: 'Nov 22', type: 'SANDWICH_LOP', label: 'Sandwich LOP', color: '#EF4444' },
    { day: 'Mon', date: 'Nov 23', type: 'LEAVE', label: 'Leave', color: '#6366F1' },
    { day: 'Tue', date: 'Nov 24', type: 'WORK', label: 'Present', color: '#10B981' },
  ];

  return (
    <Paper
      sx={{
        p: 2.5,
        backgroundColor: '#0F172A',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '8px',
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <EventNoteOutlinedIcon sx={{ color: 'primary.main', fontSize: 20 }} />
        <Typography variant="subtitle2" sx={{ fontWeight: 600, color: 'text.primary' }}>
          Sandwich Rule Simulation
        </Typography>
      </Box>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: 'repeat(3, 1fr)', sm: 'repeat(6, 1fr)' },
          gap: 1.5,
          mb: 2,
        }}
      >
        {days.map((d, idx) => (
          <Box
            key={idx}
            sx={{
              p: 1.5,
              borderRadius: '6px',
              backgroundColor: '#1E293B',
              border: `1px solid ${d.type === 'SANDWICH_LOP' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(255, 255, 255, 0.08)'}`,
              textAlign: 'center',
            }}
          >
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', fontWeight: 600 }}>
              {d.day}
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 700, color: '#F8FAFC', my: 0.5 }}>
              {d.date}
            </Typography>
            <Box
              sx={{
                fontSize: '0.7rem',
                fontWeight: 600,
                color: d.color,
                px: 0.75,
                py: 0.25,
                borderRadius: '4px',
                backgroundColor: 'rgba(255, 255, 255, 0.04)',
              }}
            >
              {d.label}
            </Box>
          </Box>
        ))}
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, p: 1.5, backgroundColor: 'rgba(239, 68, 68, 0.08)', borderRadius: '6px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
        <InfoOutlinedIcon sx={{ color: '#EF4444', fontSize: 18, mt: 0.25 }} />
        <Typography variant="caption" sx={{ color: '#F8FAFC', lineHeight: 1.4 }}>
          <strong>Sandwich Rule Applied:</strong> Leaves booked consecutively across Friday and Monday cause intervening weekly offs (Saturday and Sunday) to be debited as leave days or Loss of Pay (LOP) per company attendance policy.
        </Typography>
      </Box>
    </Paper>
  );
}
