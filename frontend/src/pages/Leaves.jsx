import React, { useState, useEffect } from 'react';
import { Box, Grid, Dialog, DialogTitle, DialogContent, DialogActions, Select, MenuItem, Typography, IconButton, Tooltip, Alert } from '@mui/material';
import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import CheckOutlinedIcon from '@mui/icons-material/CheckOutlined';

import { leaveApi, employeeApi } from '../api';
import { useAuth } from '../context/AuthContext';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';
import VetanBadge from '../components/ui/VetanBadge';
import SandwichRuleVisualizer from '../components/domain/SandwichRuleVisualizer';

export default function Leaves() {
  const { user } = useAuth();
  const [leaveRequests, setLeaveRequests] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openModal, setOpenModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const isHrOrFounder =
    user?.roles?.includes('ROLE_FOUNDER') ||
    user?.roles?.includes('ROLE_HR') ||
    user?.role === 'ROLE_FOUNDER' ||
    user?.role === 'ROLE_HR';

  const [form, setForm] = useState({
    employeeId: '',
    leaveTypeId: '',
    leaveTypeCode: 'CASUAL_LEAVE',
    startDate: '2026-11-20',
    endDate: '2026-11-23',
    reason: 'Family function in native town',
  });

  const fetchLeaves = async () => {
    setLoading(true);
    try {
      const [reqData, empData, typeData] = await Promise.all([
        leaveApi.getLeaveRequests().catch(() => []),
        employeeApi.getEmployees().catch(() => []),
        leaveApi.getLeaveTypes().catch(() => []),
      ]);
      setLeaveRequests(Array.isArray(reqData) ? reqData : []);
      const emps = Array.isArray(empData) ? empData : (empData?.content || []);
      setEmployees(emps);
      const types = Array.isArray(typeData) ? typeData : [];
      setLeaveTypes(types);

      setForm((prev) => ({
        ...prev,
        employeeId: prev.employeeId || emps[0]?.id || '',
        leaveTypeId: prev.leaveTypeId || types[0]?.id || '',
      }));
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeaves();
  }, []);

  const handleApply = async (e) => {
    e.preventDefault();
    setSaving(true);
    setSubmitError('');
    try {
      const targetType = leaveTypes.find(
        (t) => t.id === form.leaveTypeId || t.leaveCode === form.leaveTypeCode
      );

      const payload = {
        employeeId: form.employeeId || employees[0]?.id,
        leaveTypeId: targetType?.id || form.leaveTypeId || undefined,
        leaveTypeCode: form.leaveTypeCode,
        startDate: form.startDate,
        endDate: form.endDate,
        reason: form.reason,
      };

      await leaveApi.applyLeave(payload);
      setOpenModal(false);
      fetchLeaves();
    } catch (err) {
      setSubmitError(err.message || 'Failed to submit leave request');
    } finally {
      setSaving(false);
    }
  };

  const handleApprove = async (id) => {
    try {
      await leaveApi.approveLeave(id);
      fetchLeaves();
    } catch (e) {
      alert(e.message || 'Failed to approve leave');
    }
  };

  const handleReject = async (id) => {
    try {
      await leaveApi.rejectLeave(id, 'Rejected by HR Manager');
      fetchLeaves();
    } catch (e) {
      alert(e.message || 'Failed to reject leave');
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
      field: 'leaveTypeCode',
      headerName: 'Leave Type',
      renderCell: ({ row }) => row.leaveTypeCode || 'CASUAL_LEAVE',
    },
    {
      field: 'startDate',
      headerName: 'Duration',
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          {row.startDate} to {row.endDate}
        </Typography>
      ),
    },
    {
      field: 'totalDays',
      headerName: 'Days Debited',
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
          {row.totalDays || 1} Days
        </Typography>
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: ({ row }) => <VetanBadge status={row.status || 'APPROVED'} />,
    },
    ...(isHrOrFounder
      ? [
          {
            field: 'actions',
            headerName: 'Review',
            width: 120,
            renderCell: ({ row }) => {
              if (row.status === 'PENDING') {
                return (
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Tooltip title="Approve Leave">
                      <IconButton aria-label="Approve leave"
                        size="small"
                        onClick={() => handleApprove(row.id)}
                        sx={{ color: '#10B981', '&:hover': { backgroundColor: 'rgba(16, 185, 129, 0.1)' } }}
                      >
                        <CheckOutlinedIcon sx={{ fontSize: 18 }} />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Reject Leave">
                      <IconButton aria-label="Reject leave"
                        size="small"
                        onClick={() => handleReject(row.id)}
                        sx={{ color: '#EF4444', '&:hover': { backgroundColor: 'rgba(239, 68, 68, 0.1)' } }}
                      >
                        <CloseOutlinedIcon sx={{ fontSize: 18 }} />
                      </IconButton>
                    </Tooltip>
                  </Box>
                );
              }
              return (
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  Finalized
                </Typography>
              );
            },
          },
        ]
      : []),
  ];

  return (
    <Box>
      <PageHeader
        title="Leave Ledger & Policies"
        subtitle="Immutable leave balance ledger with automated Indian Sandwich rule detection"
        action={
          <VetanButton
            variant="contained"
            color="primary"
            startIcon={<AddOutlinedIcon />}
            onClick={() => setOpenModal(true)}
          >
            Apply Leave
          </VetanButton>
        }
      />

      <Box sx={{ mb: 3 }}>
        <SandwichRuleVisualizer
          fromDate={form.startDate}
          toDate={form.endDate}
          isSandwichTriggered={true}
        />
      </Box>

      <DataTable
        columns={columns}
        rows={leaveRequests}
        loading={loading}
        searchPlaceholder="Filter leave requests..."
        searchField="employeeName"
        emptyTitle="No leave requests logged"
        emptySubtitle="Submitted leave applications and ledger credit/debit entries appear here."
        emptyActionLabel="Apply Leave"
        onEmptyAction={() => setOpenModal(true)}
      />

      <Dialog
        open={openModal}
        onClose={() => setOpenModal(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: {
            backgroundColor: '#0F172A',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '10px',
          },
        }}
      >
        <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            Apply for Leave
          </Typography>
          <Tooltip title="Close modal">
            <IconButton aria-label="Close apply leave modal" size="small" onClick={() => setOpenModal(false)} sx={{ color: 'text.secondary' }}>
              <CloseOutlinedIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Tooltip>
        </DialogTitle>

        <form onSubmit={handleApply}>
          <DialogContent sx={{ py: 2 }}>
            {submitError && (
              <Alert severity="error" sx={{ mb: 2, backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#EF4444' }}>
                {submitError}
              </Alert>
            )}

            <Typography variant="caption" sx={{ display: 'block', mb: 0.75, color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', fontSize: '0.72rem' }}>
              Employee
            </Typography>
            <Select
              fullWidth
              size="small"
              value={form.employeeId}
              onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
              sx={{ backgroundColor: '#1E293B', borderRadius: '8px', mb: 2 }}
            >
              {employees.map((emp) => (
                <MenuItem key={emp.id} value={emp.id}>
                  {emp.firstName} {emp.lastName} ({emp.employeeCode || 'EMP'})
                </MenuItem>
              ))}
            </Select>

            <Typography variant="caption" sx={{ display: 'block', mb: 0.75, color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', fontSize: '0.72rem' }}>
              Leave Type
            </Typography>
            <Select
              fullWidth
              size="small"
              value={form.leaveTypeId || form.leaveTypeCode}
              onChange={(e) => {
                const val = e.target.value;
                const match = leaveTypes.find((t) => t.id === val || t.leaveCode === val);
                setForm({
                  ...form,
                  leaveTypeId: match ? match.id : val,
                  leaveTypeCode: match ? match.leaveCode : val,
                });
              }}
              sx={{ backgroundColor: '#1E293B', borderRadius: '8px', mb: 2 }}
            >
              {leaveTypes.length > 0 ? (
                leaveTypes.map((lt) => (
                  <MenuItem key={lt.id} value={lt.id}>
                    {lt.leaveName} ({lt.leaveCode})
                  </MenuItem>
                ))
              ) : (
                [
                  <MenuItem key="CL" value="CASUAL_LEAVE">Casual Leave (CL)</MenuItem>,
                  <MenuItem key="SL" value="SICK_LEAVE">Sick Leave (SL)</MenuItem>,
                  <MenuItem key="EL" value="EARNED_LEAVE">Earned / Privilege Leave (EL/PL)</MenuItem>,
                ]
              )}
            </Select>

            <Grid container spacing={2}>
              <Grid item xs={6}>
                <VetanInput
                  label="Start Date"
                  type="date"
                  value={form.startDate}
                  onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                  required
                />
              </Grid>
              <Grid item xs={6}>
                <VetanInput
                  label="End Date"
                  type="date"
                  value={form.endDate}
                  onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                  required
                />
              </Grid>
            </Grid>

            <VetanInput
              label="Reason for Absence"
              value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })}
              placeholder="Provide reason for leave"
              required
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
            <VetanButton variant="outlined" onClick={() => setOpenModal(false)}>
              Cancel
            </VetanButton>
            <VetanButton type="submit" variant="contained" color="primary" loading={saving}>
              Submit Application
            </VetanButton>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
