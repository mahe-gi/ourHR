import React from 'react';
import { Box, Select, MenuItem, Typography, Chip } from '@mui/material';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import { useAuth } from '../../context/AuthContext';

export default function BranchSelector({ size = 'small' }) {
  const { branches, currentBranch, setCurrentBranch } = useAuth();

  if (!branches || branches.length === 0) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <BusinessOutlinedIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          Headquarters
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <BusinessOutlinedIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
      <Select
        size={size}
        value={currentBranch ? currentBranch.id : (branches[0] ? branches[0].id : '')}
        onChange={(e) => {
          const selected = branches.find((b) => b.id === e.target.value);
          if (selected) setCurrentBranch(selected);
        }}
        sx={{
          minWidth: 180,
          backgroundColor: '#1E293B',
          borderRadius: '6px',
          fontSize: '0.84rem',
          '& fieldset': { borderColor: 'rgba(255, 255, 255, 0.08)' },
          '& .MuiSelect-select': {
            py: 0.75,
            px: 1.5,
            display: 'flex',
            alignItems: 'center',
            gap: 1,
          },
        }}
      >
        {branches.map((b) => (
          <MenuItem key={b.id} value={b.id} sx={{ fontSize: '0.84rem', display: 'flex', justifyContent: 'space-between', gap: 2 }}>
            <span>{b.branchName || b.name}</span>
            <Chip
              label={b.stateCode || 'HQ'}
              size="small"
              sx={{
                height: 18,
                fontSize: '0.68rem',
                backgroundColor: b.isHq ? 'rgba(99, 102, 241, 0.15)' : 'rgba(255, 255, 255, 0.05)',
                color: b.isHq ? '#818CF8' : '#94A3B8',
              }}
            />
          </MenuItem>
        ))}
      </Select>
    </Box>
  );
}
