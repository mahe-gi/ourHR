import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Alert, Paper, Grid } from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';

import { attendanceApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import VetanCard from '../components/ui/VetanCard';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';

export default function AttendanceFreeze() {
  const navigate = useNavigate();
  const [yearMonth, setYearMonth] = useState('2026-11');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleFreeze = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess(false);
    try {
      await attendanceApi.freezeAttendance({ yearMonth });
      setSuccess(true);
    } catch (err) {
      setError(err.message || 'Failed to freeze monthly attendance.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Monthly Attendance Freeze"
        subtitle="Lock punch records, compute Sandwich LOP, and generate locked attendance summary for payroll"
      />

      <Grid container spacing={3}>
        <Grid item xs={12} md={7}>
          <VetanCard title="Execute Monthly Lock">
            <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2.5 }}>
              Under the ourHR domain laws, raw biometric and GPS punches cannot be modified after freezing. The locked <code>monthly_attendance_summary</code> becomes immutable input for the payroll calculation engine.
            </Typography>

            {error && (
              <Alert severity="error" sx={{ mb: 2.5, backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#EF4444' }}>
                {error}
              </Alert>
            )}

            {success && (
              <Alert
                severity="success"
                icon={<CheckCircleOutlinedIcon sx={{ color: '#10B981' }} />}
                sx={{ mb: 2.5, backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10B981' }}
              >
                Attendance for {yearMonth} has been successfully frozen! You can now proceed to run payroll.
              </Alert>
            )}

            <form onSubmit={handleFreeze}>
              <VetanInput
                label="Payroll Cycle (YYYY-MM)"
                value={yearMonth}
                onChange={(e) => setYearMonth(e.target.value)}
                placeholder="2026-11"
                required
              />

              <Box sx={{ display: 'flex', gap: 2, mt: 1 }}>
                <VetanButton
                  type="submit"
                  variant="contained"
                  color="primary"
                  startIcon={<LockOutlinedIcon />}
                  loading={loading}
                >
                  Freeze {yearMonth} Attendance
                </VetanButton>

                {success && (
                  <VetanButton
                    variant="contained"
                    color="success"
                    endIcon={<ArrowForwardOutlinedIcon />}
                    onClick={() => navigate('/payroll')}
                  >
                    Proceed to Payroll
                  </VetanButton>
                )}
              </Box>
            </form>
          </VetanCard>
        </Grid>

        <Grid item xs={12} md={5}>
          <VetanCard title="Freeze Rules & Validation">
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box sx={{ p: 1.5, backgroundColor: '#1E293B', borderRadius: '6px' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#818CF8' }}>
                  1. Missed Punch Regularization
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                  All pending regularization requests must be approved or rejected prior to freeze.
                </Typography>
              </Box>
              <Box sx={{ p: 1.5, backgroundColor: '#1E293B', borderRadius: '6px' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#818CF8' }}>
                  2. Sandwich Rule LOP Audit
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                  Automated scan converts intervening weekends between unpaid leaves into LOP days.
                </Typography>
              </Box>
              <Box sx={{ p: 1.5, backgroundColor: '#1E293B', borderRadius: '6px' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#818CF8' }}>
                  3. Immutability Seal
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                  Creates locked snapshots consumed strictly in read-only mode by the payroll runner.
                </Typography>
              </Box>
            </Box>
          </VetanCard>
        </Grid>
      </Grid>
    </Box>
  );
}
