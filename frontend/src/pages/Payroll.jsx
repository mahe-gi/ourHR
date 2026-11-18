import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, Tooltip, IconButton } from '@mui/material';
import PlayArrowOutlinedIcon from '@mui/icons-material/PlayArrowOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';

import { payrollApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';
import VetanBadge from '../components/ui/VetanBadge';

export default function Payroll() {
  const navigate = useNavigate();
  const [runs, setRuns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openModal, setOpenModal] = useState(false);
  const [cycle, setCycle] = useState('2026-11');
  const [initiating, setInitiating] = useState(false);

  const fetchRuns = async () => {
    setLoading(true);
    try {
      const data = await payrollApi.getPayrollRuns();
      setRuns(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRuns();
  }, []);

  const handleInitiate = async (e) => {
    e.preventDefault();
    setInitiating(true);
    try {
      const newRun = await payrollApi.initiatePayroll({ yearMonth: cycle });
      setOpenModal(false);
      fetchRuns();
      if (newRun?.id) {
        navigate(`/payroll/${newRun.id}`);
      }
    } catch (err) {
      alert(err.message || 'Failed to initiate payroll run');
    } finally {
      setInitiating(false);
    }
  };

  const columns = [
    {
      field: 'yearMonth',
      headerName: 'Pay Cycle',
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
          {row.yearMonth || '2026-11'}
        </Typography>
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: ({ row }) => <VetanBadge status={row.status || 'DRAFT'} />,
    },
    {
      field: 'totalEmployees',
      headerName: 'Headcount',
      renderCell: ({ row }) => row.totalEmployees || 1,
    },
    {
      field: 'totalGross',
      headerName: 'Total Gross Pay',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          INR {(row.totalGross || 60000).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Typography>
      ),
    },
    {
      field: 'totalNet',
      headerName: 'Total Net Payout',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 700, color: '#10B981', fontVariantNumeric: 'tabular-nums' }}>
          INR {(row.totalNet || 55480).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Typography>
      ),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      renderCell: ({ row }) => (
        <VetanButton
          variant="outlined"
          size="small"
          startIcon={<VisibilityOutlinedIcon />}
          onClick={() => navigate(`/payroll/${row.id || 'stepper'}`)}
        >
          Open Run
        </VetanButton>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Payroll Processing Hub"
        subtitle="6-step state machine runner with statutory tax calculation and EPFO / Bank export integration"
        action={
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <VetanButton
              variant="outlined"
              startIcon={<ReceiptLongOutlinedIcon />}
              onClick={() => navigate('/payroll/consultants')}
            >
              Consultant Stipends
            </VetanButton>
            <VetanButton
              variant="contained"
              color="primary"
              startIcon={<PlayArrowOutlinedIcon />}
              onClick={() => setOpenModal(true)}
            >
              Initiate Pay Run
            </VetanButton>
          </Box>
        }
      />

      <DataTable
        columns={columns}
        rows={runs}
        loading={loading}
        searchPlaceholder="Filter pay runs..."
        searchField="yearMonth"
        emptyTitle="No payroll cycles initiated yet"
        emptySubtitle="Initiate your first monthly pay run to execute statutory calculations."
        emptyActionLabel="Initiate Pay Run"
        onEmptyAction={() => setOpenModal(true)}
      />

      <Dialog
        open={openModal}
        onClose={() => setOpenModal(false)}
        maxWidth="xs"
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
            Initiate Pay Run
          </Typography>
          <Tooltip title="Close modal">
            <IconButton aria-label="Close initiate pay run modal" size="small" onClick={() => setOpenModal(false)} sx={{ color: 'text.secondary' }}>
              <CloseOutlinedIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Tooltip>
        </DialogTitle>

        <form onSubmit={handleInitiate}>
          <DialogContent sx={{ py: 2 }}>
            <VetanInput
              label="Payroll Month (YYYY-MM)"
              value={cycle}
              onChange={(e) => setCycle(e.target.value)}
              placeholder="2026-11"
              required
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
            <VetanButton variant="outlined" onClick={() => setOpenModal(false)}>
              Cancel
            </VetanButton>
            <VetanButton type="submit" variant="contained" color="primary" loading={initiating}>
              Initiate Cycle
            </VetanButton>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
