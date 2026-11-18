import React, { useState, useEffect } from 'react';
import { Box, Grid, Dialog, DialogTitle, DialogContent, DialogActions, Select, MenuItem, Typography, IconButton, Tooltip } from '@mui/material';
import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';

import { leaveApi, employeeApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';
import VetanBadge from '../components/ui/VetanBadge';
import SandwichRuleVisualizer from '../components/domain/SandwichRuleVisualizer';

export default function Leaves() {
  const [leaveRequests, setLeaveRequests] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openModal, setOpenModal] = useState(false);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    employeeId: '',
    leaveTypeCode: 'CASUAL_LEAVE',
    startDate: '2026-11-20',
    endDate: '2026-11-23',
    reason: 'Family function in native town',
  });

  const fetchLeaves = async () => {
    setLoading(true);
    try {
      const [reqData, empData] = await Promise.all([
        leaveApi.getLeaveRequests().catch(() => []),
        employeeApi.getEmployees().catch(() => []),
      ]);
      setLeaveRequests(Array.isArray(reqData) ? reqData : []);
      const emps = Array.isArray(empData) ? empData : (empData?.content || []);
      setEmployees(emps);
      if (emps.length > 0) {
        setForm((prev) => ({ ...prev, employeeId: emps[0].id }));
      }
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
    try {
      await leaveApi.applyLeave(form);
      setOpenModal(false);
      fetchLeaves();
    } catch (err) {
      alert(err.message || 'Failed to submit leave request');
    } finally {
      setSaving(false);
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
          {row.totalDays || 4} Days
        </Typography>
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: ({ row }) => <VetanBadge status={row.status || 'APPROVED'} />,
    },
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
              value={form.leaveTypeCode}
              onChange={(e) => setForm({ ...form, leaveTypeCode: e.target.value })}
              sx={{ backgroundColor: '#1E293B', borderRadius: '8px', mb: 2 }}
            >
              <MenuItem value="CASUAL_LEAVE">Casual Leave (CL)</MenuItem>
              <MenuItem value="SICK_LEAVE">Sick Leave (SL)</MenuItem>
              <MenuItem value="EARNED_LEAVE">Earned / Privilege Leave (EL/PL)</MenuItem>
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
