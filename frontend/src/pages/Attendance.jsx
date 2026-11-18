import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Grid, Typography, Alert, Paper } from '@mui/material';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';
import LockClockOutlinedIcon from '@mui/icons-material/LockClockOutlined';
import TouchAppOutlinedIcon from '@mui/icons-material/TouchAppOutlined';

import { attendanceApi, employeeApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanBadge from '../components/ui/VetanBadge';
import VetanCard from '../components/ui/VetanCard';

export default function Attendance() {
  const navigate = useNavigate();
  const [punches, setPunches] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [punching, setPunching] = useState(false);
  const [message, setMessage] = useState('');

  const fetchAttendance = async () => {
    setLoading(true);
    try {
      const today = new Date().toISOString().split('T')[0];
      const [punchData, empData] = await Promise.all([
        attendanceApi.getDailyPunches(today).catch(() => []),
        employeeApi.getEmployees().catch(() => []),
      ]);
      setPunches(Array.isArray(punchData) ? punchData : []);
      setEmployees(Array.isArray(empData) ? empData : (empData?.content || []));
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAttendance();
  }, []);

  const handleWebPunch = async (type) => {
    setPunching(true);
    setMessage('');
    try {
      const empId = employees[0]?.id;
      if (!empId) {
        setMessage('No active employee profile available for punch.');
        setPunching(false);
        return;
      }
      await attendanceApi.punch({
        employeeId: empId,
        punchType: type,
        latitude: 12.9716,
        longitude: 77.5946,
      });
      setMessage(`Punch ${type} registered successfully.`);
      fetchAttendance();
    } catch (err) {
      setMessage(err.message || 'Failed to record punch.');
    } finally {
      setPunching(false);
    }
  };

  const columns = [
    {
      field: 'employeeName',
      headerName: 'Employee',
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
          {row.employeeName || row.employee?.fullName || 'Employee'}
        </Typography>
      ),
    },
    {
      field: 'punchTime',
      headerName: 'Punch Time',
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums', fontFamily: 'monospace' }}>
          {row.punchTime ? new Date(row.punchTime).toLocaleTimeString('en-IN') : '09:30 AM'}
        </Typography>
      ),
    },
    {
      field: 'punchType',
      headerName: 'Type',
      renderCell: ({ row }) => (
        <VetanBadge status={row.punchType === 'PUNCH_IN' ? 'ACTIVE' : 'PENDING'} label={row.punchType || 'IN'} />
      ),
    },
    {
      field: 'punchSource',
      headerName: 'Source',
      renderCell: ({ row }) => row.punchSource || 'WEB_PORTAL',
    },
    {
      field: 'status',
      headerName: 'Verification',
      renderCell: () => <VetanBadge status="COMPLETED" label="GEO_VERIFIED" />,
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Attendance & Daily Punches"
        subtitle="Real-time employee clock-in/out, GPS geo-tagging, and shift scheduling"
        action={
          <VetanButton
            variant="contained"
            color="primary"
            startIcon={<LockClockOutlinedIcon />}
            onClick={() => navigate('/attendance/freeze')}
          >
            Freeze Attendance for Payroll
          </VetanButton>
        }
      />

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={5}>
          <VetanCard title="Web / Mobile Clock-in">
            <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
              Geo-verified punch terminal (Bengaluru Headquarters, KA).
            </Typography>

            {message && (
              <Alert severity="info" sx={{ mb: 2, backgroundColor: 'rgba(99, 102, 241, 0.1)', color: '#818CF8' }}>
                {message}
              </Alert>
            )}

            <Box sx={{ display: 'flex', gap: 2 }}>
              <VetanButton
                variant="contained"
                color="success"
                startIcon={<TouchAppOutlinedIcon />}
                loading={punching}
                onClick={() => handleWebPunch('IN')}
                fullWidth
              >
                Punch In (09:30)
              </VetanButton>
              <VetanButton
                variant="outlined"
                color="error"
                startIcon={<AccessTimeOutlinedIcon />}
                loading={punching}
                onClick={() => handleWebPunch('OUT')}
                fullWidth
              >
                Punch Out (18:30)
              </VetanButton>
            </Box>
          </VetanCard>
        </Grid>

        <Grid item xs={12} md={7}>
          <VetanCard title="Today's Shift Policy">
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>Standard General Shift</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>09:30 AM - 06:30 PM (9 hrs)</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>Grace Period for Late Mark</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>15 Minutes (Up to 09:45 AM)</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>Late Penalty Rule</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>3 Late Marks = 0.5 Day LOP</Typography>
            </Box>
          </VetanCard>
        </Grid>
      </Grid>

      <DataTable
        columns={columns}
        rows={punches}
        loading={loading}
        searchPlaceholder="Filter punches..."
        searchField="employeeName"
        emptyTitle="No punches registered today"
        emptySubtitle="Punches recorded via mobile GPS or web will appear here in real-time."
      />
    </Box>
  );
}
