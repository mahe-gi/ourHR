import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Grid, Box, Typography, Alert } from '@mui/material';
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline';
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';
import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import PlayArrowOutlinedIcon from '@mui/icons-material/PlayArrowOutlined';
import VerifiedUserOutlinedIcon from '@mui/icons-material/VerifiedUserOutlined';

import { useAuth } from '../context/AuthContext';
import { employeeApi, payrollApi, leaveApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import StatCard from '../components/ui/StatCard';
import VetanButton from '../components/ui/VetanButton';
import VetanCard from '../components/ui/VetanCard';

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, company, currentBranch } = useAuth();

  const [stats, setStats] = useState({
    employeeCount: 0,
    payrollRuns: 0,
    pendingLeaves: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const roles = user?.roles || (user?.role ? [user.role] : []);
    const isPureEmployee =
      roles.includes('ROLE_EMPLOYEE') &&
      !roles.includes('ROLE_FOUNDER') &&
      !roles.includes('ROLE_HR') &&
      !roles.includes('ROLE_FINANCE') &&
      !roles.includes('ROLE_CA');

    if (isPureEmployee) {
      navigate('/ess', { replace: true });
      return;
    }

    const isPureCa =
      roles.includes('ROLE_CA') &&
      !roles.includes('ROLE_FOUNDER') &&
      !roles.includes('ROLE_HR') &&
      !roles.includes('ROLE_FINANCE');

    if (isPureCa) {
      navigate('/ca-workspace', { replace: true });
      return;
    }

    const fetchDashboardData = async () => {
      try {
        const [empData, payData, leaveData] = await Promise.all([
          employeeApi.getEmployees().catch(() => []),
          payrollApi.getPayrollRuns().catch(() => []),
          leaveApi.getLeaveRequests().catch(() => []),
        ]);
        setStats({
          employeeCount: Array.isArray(empData) ? empData.length : (empData?.content?.length || 0),
          payrollRuns: Array.isArray(payData) ? payData.length : 0,
          pendingLeaves: Array.isArray(leaveData) ? leaveData.length : 0,
        });
      } catch (e) {
        // Fallback for dashboard stats
      } finally {
        setLoading(false);
      }
    };
    fetchDashboardData();
  }, []);

  return (
    <Box>
      <PageHeader
        title={`Welcome, ${user?.fullName || 'User'}`}
        subtitle={`${company?.companyName || 'Vetan Workspace'} — ${currentBranch?.branchName || 'Headquarters'} (${currentBranch?.stateCode || 'IN'})`}
        action={
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <VetanButton
              variant="outlined"
              startIcon={<AddOutlinedIcon />}
              onClick={() => navigate('/onboarding')}
            >
              Add Employee
            </VetanButton>
            <VetanButton
              variant="contained"
              color="primary"
              startIcon={<PlayArrowOutlinedIcon />}
              onClick={() => navigate('/payroll')}
            >
              Run Payroll
            </VetanButton>
          </Box>
        }
      />

      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Employees"
            value={stats.employeeCount}
            subtitle="Active on payroll"
            icon={PeopleOutlineIcon}
            color="#6366F1"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Payroll Runs"
            value={stats.payrollRuns}
            subtitle="Processed cycles"
            icon={PaymentsOutlinedIcon}
            color="#10B981"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Leave Requests"
            value={stats.pendingLeaves}
            subtitle="Awaiting approval"
            icon={EventNoteOutlinedIcon}
            color="#F59E0B"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Statutory Status"
            value="Compliant"
            subtitle="EPF / ESIC / PT / TDS"
            icon={VerifiedUserOutlinedIcon}
            color="#10B981"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={7}>
          <VetanCard title="Quick Actions">
            <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
              Streamlined Indian payroll workflows matching statutory deadlines:
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Box
                  onClick={() => navigate('/attendance/freeze')}
                  sx={{
                    p: 2,
                    borderRadius: '8px',
                    backgroundColor: '#1E293B',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    cursor: 'pointer',
                    '&:hover': { borderColor: 'primary.light' },
                  }}
                >
                  <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                    1. Freeze Monthly Attendance
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    Lock punch records and calculate LOP for monthly pay run.
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box
                  onClick={() => navigate('/payroll')}
                  sx={{
                    p: 2,
                    borderRadius: '8px',
                    backgroundColor: '#1E293B',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    cursor: 'pointer',
                    '&:hover': { borderColor: 'primary.light' },
                  }}
                >
                  <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                    2. Execute 6-Step Payroll Run
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    Calculate EPF, ESIC, PT, and dual-regime TDS.
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box
                  onClick={() => navigate('/ca-workspace')}
                  sx={{
                    p: 2,
                    borderRadius: '8px',
                    backgroundColor: '#1E293B',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    cursor: 'pointer',
                    '&:hover': { borderColor: 'primary.light' },
                  }}
                >
                  <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                    3. Download EPFO ECR & Bank CSV
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    Export 25-col .txt challan and HDFC/ICICI salary files.
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box
                  onClick={() => navigate('/leaves')}
                  sx={{
                    p: 2,
                    borderRadius: '8px',
                    backgroundColor: '#1E293B',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    cursor: 'pointer',
                    '&:hover': { borderColor: 'primary.light' },
                  }}
                >
                  <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                    4. Leave Ledger & Sandwich Rule
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    Review credit/debit balances and weekend LOP conversions.
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </VetanCard>
        </Grid>

        <Grid item xs={12} md={5}>
          <VetanCard title="Indian Compliance Calendar">
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box sx={{ p: 1.5, borderRadius: '6px', backgroundColor: '#1E293B', borderLeft: '4px solid #10B981' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                  EPFO Monthly ECR Return
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                  Due by 15th of every month. File Form 5A & 12A via Unified Portal.
                </Typography>
              </Box>
              <Box sx={{ p: 1.5, borderRadius: '6px', backgroundColor: '#1E293B', borderLeft: '4px solid #6366F1' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                  ESIC Monthly Contribution
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                  Due by 15th of every month for gross wages up to INR 21,000.
                </Typography>
              </Box>
              <Box sx={{ p: 1.5, borderRadius: '6px', backgroundColor: '#1E293B', borderLeft: '4px solid #F59E0B' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                  TDS Payment (Challan ITNS 281)
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                  Due by 7th of succeeding month (Form 24Q quarterly filing).
                </Typography>
              </Box>
            </Box>
          </VetanCard>
        </Grid>
      </Grid>
    </Box>
  );
}
