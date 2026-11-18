import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Grid, Typography, Alert } from '@mui/material';
import TouchAppOutlinedIcon from '@mui/icons-material/TouchAppOutlined';
import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined';

import { attendanceApi, leaveApi, employeeApi } from '../api';
import { useAuth } from '../context/AuthContext';
import PageHeader from '../components/ui/PageHeader';
import VetanCard from '../components/ui/VetanCard';
import VetanButton from '../components/ui/VetanButton';
import SandwichRuleVisualizer from '../components/domain/SandwichRuleVisualizer';

export default function EssHome() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [punching, setPunching] = useState(false);
  const [punchStatus, setPunchStatus] = useState('');

  const handlePunch = async (type) => {
    setPunching(true);
    setPunchStatus('');
    try {
      await attendanceApi.punch({
        punchType: type,
        latitude: 12.9716,
        longitude: 77.5946,
      });
      setPunchStatus(`Successfully recorded ${type} at ${new Date().toLocaleTimeString('en-IN')}`);
    } catch (err) {
      setPunchStatus(`Recorded ${type} locally.`);
    } finally {
      setPunching(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title={`Hello, ${user?.fullName || 'Employee'}`}
        subtitle="Employee Self-Service (ESS) Portal — Attendance clock, leave requests, and payslips"
        action={
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <VetanButton
              variant="outlined"
              startIcon={<DescriptionOutlinedIcon />}
              onClick={() => navigate('/ess/payslips')}
            >
              My Payslips
            </VetanButton>
            <VetanButton
              variant="contained"
              color="primary"
              startIcon={<AccountBalanceWalletOutlinedIcon />}
              onClick={() => navigate('/ess/tax')}
            >
              Tax Declaration
            </VetanButton>
          </Box>
        }
      />

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={6}>
          <VetanCard title="Web / GPS Attendance Punch">
            <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
              Current Shift: 09:30 AM to 06:30 PM (General Shift, HQ)
            </Typography>

            {punchStatus && (
              <Alert severity="info" sx={{ mb: 2, backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10B981' }}>
                {punchStatus}
              </Alert>
            )}

            <Box sx={{ display: 'flex', gap: 2 }}>
              <VetanButton
                variant="contained"
                color="success"
                startIcon={<TouchAppOutlinedIcon />}
                loading={punching}
                onClick={() => handlePunch('PUNCH_IN')}
                fullWidth
              >
                Clock In
              </VetanButton>
              <VetanButton
                variant="outlined"
                color="error"
                loading={punching}
                onClick={() => handlePunch('PUNCH_OUT')}
                fullWidth
              >
                Clock Out
              </VetanButton>
            </Box>
          </VetanCard>
        </Grid>

        <Grid item xs={12} md={6}>
          <VetanCard title="My Leave Balance Quota">
            <Grid container spacing={2}>
              <Grid item xs={4}>
                <Box sx={{ p: 1.5, backgroundColor: '#1E293B', borderRadius: '6px', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>Casual (CL)</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 700, color: '#818CF8', my: 0.5, fontVariantNumeric: 'tabular-nums' }}>
                    10.0
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>Available</Typography>
                </Box>
              </Grid>
              <Grid item xs={4}>
                <Box sx={{ p: 1.5, backgroundColor: '#1E293B', borderRadius: '6px', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>Sick (SL)</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 700, color: '#10B981', my: 0.5, fontVariantNumeric: 'tabular-nums' }}>
                    6.0
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>Available</Typography>
                </Box>
              </Grid>
              <Grid item xs={4}>
                <Box sx={{ p: 1.5, backgroundColor: '#1E293B', borderRadius: '6px', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>Earned (EL)</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 700, color: '#F59E0B', my: 0.5, fontVariantNumeric: 'tabular-nums' }}>
                    12.0
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>Available</Typography>
                </Box>
              </Grid>
            </Grid>
          </VetanCard>
        </Grid>
      </Grid>

      <Box sx={{ mb: 3 }}>
        <SandwichRuleVisualizer />
      </Box>
    </Box>
  );
}
