import React, { useState, useEffect } from 'react';
import { Box, Grid, Dialog, DialogTitle, DialogContent, DialogActions, Select, MenuItem, Typography, Chip, Tooltip, IconButton, Alert } from '@mui/material';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';

import { companyApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';
import VetanBadge from '../components/ui/VetanBadge';

const INDIAN_STATES = [
  { code: 'KA', name: 'Karnataka' },
  { code: 'MH', name: 'Maharashtra' },
  { code: 'DL', name: 'Delhi NCR' },
  { code: 'TN', name: 'Tamil Nadu' },
  { code: 'TS', name: 'Telangana' },
  { code: 'HR', name: 'Haryana' },
  { code: 'GJ', name: 'Gujarat' },
  { code: 'WB', name: 'West Bengal' },
];

export default function Branches() {
  const [branches, setBranches] = useState([]);
  const [company, setCompany] = useState(null);
  const [loading, setLoading] = useState(true);
  const [openModal, setOpenModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const [form, setForm] = useState({
    branchName: '',
    stateCode: 'KA',
    city: 'Bengaluru',
    addressLine1: '',
    pincode: '560001',
    isHq: false,
  });

  const fetchBranches = async () => {
    setLoading(true);
    try {
      const [compData, branchData] = await Promise.all([
        companyApi.getCurrentCompany().catch(() => null),
        companyApi.getBranches().catch(() => []),
      ]);
      setCompany(compData);
      setBranches(branchData || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBranches();
  }, []);

  const handleCreateBranch = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrorMsg('');
    try {
      await companyApi.createBranch(form);
      setOpenModal(false);
      setForm({
        branchName: '',
        stateCode: 'KA',
        city: '',
        addressLine1: '',
        pincode: '',
        isHq: false,
      });
      fetchBranches();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to create branch');
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      field: 'branchName',
      headerName: 'Branch Name',
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <BusinessOutlinedIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
          <Typography variant="body2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
            {row.branchName || row.name}
          </Typography>
          {row.isHq && (
            <Chip label="HQ" size="small" sx={{ height: 18, fontSize: '0.65rem', bgcolor: 'rgba(99, 102, 241, 0.15)', color: '#818CF8' }} />
          )}
        </Box>
      ),
    },
    {
      field: 'stateCode',
      headerName: 'State Code',
      renderCell: ({ row }) => (
        <Chip
          label={row.stateCode}
          size="small"
          sx={{ height: 20, fontSize: '0.72rem', fontWeight: 600, bgcolor: 'rgba(255, 255, 255, 0.05)', color: '#94A3B8' }}
        />
      ),
    },
    {
      field: 'city',
      headerName: 'City',
      renderCell: ({ row }) => row.city || '—',
    },
    {
      field: 'addressLine1',
      headerName: 'Address',
      renderCell: ({ row }) => row.addressLine1 || row.address || '—',
    },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: () => <VetanBadge status="ACTIVE" />,
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Multi-State Branches"
        subtitle={`Manage regional offices and statutory tax locations for ${company?.companyName || 'Company'}`}
        action={
          <VetanButton
            variant="contained"
            color="primary"
            startIcon={<AddOutlinedIcon />}
            onClick={() => setOpenModal(true)}
          >
            Add Branch
          </VetanButton>
        }
      />

      <DataTable
        columns={columns}
        rows={branches}
        loading={loading}
        searchPlaceholder="Search branch by name..."
        searchField="branchName"
        emptyTitle="No branches found"
        emptySubtitle="Add your first branch or regional office to configure state-specific PT and LWF."
        emptyActionLabel="Add Branch"
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
            Add Regional Branch
          </Typography>
          <Tooltip title="Close modal">
            <IconButton aria-label="Close add branch modal" size="small" onClick={() => setOpenModal(false)} sx={{ color: 'text.secondary' }}>
              <CloseOutlinedIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Tooltip>
        </DialogTitle>

        <form onSubmit={handleCreateBranch}>
          <DialogContent sx={{ py: 2 }}>
            {errorMsg && (
              <Alert severity="error" sx={{ mb: 2, backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#EF4444' }}>
                {errorMsg}
              </Alert>
            )}
            <VetanInput
              label="Branch Name"
              value={form.branchName}
              onChange={(e) => setForm({ ...form, branchName: e.target.value })}
              placeholder="Mumbai Tech Hub"
              required
            />
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="caption" sx={{ display: 'block', mb: 0.75, color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', fontSize: '0.72rem' }}>
                  State Code (PT Slab Rule)
                </Typography>
                <Select
                  fullWidth
                  size="small"
                  value={form.stateCode}
                  onChange={(e) => setForm({ ...form, stateCode: e.target.value })}
                  sx={{
                    backgroundColor: '#1E293B',
                    borderRadius: '8px',
                    fontSize: '0.875rem',
                    mb: 2,
                  }}
                >
                  {INDIAN_STATES.map((s) => (
                    <MenuItem key={s.code} value={s.code}>
                      {s.name} ({s.code})
                    </MenuItem>
                  ))}
                </Select>
              </Grid>
              <Grid item xs={12} sm={6}>
                <VetanInput
                  label="City"
                  value={form.city}
                  onChange={(e) => setForm({ ...form, city: e.target.value })}
                  placeholder="Mumbai"
                  required
                />
              </Grid>
            </Grid>
            <VetanInput
              label="Office Address"
              value={form.addressLine1}
              onChange={(e) => setForm({ ...form, addressLine1: e.target.value })}
              placeholder="Level 4, Bandra Kurla Complex"
              required
            />
            <VetanInput
              label="PIN Code"
              value={form.pincode}
              onChange={(e) => setForm({ ...form, pincode: e.target.value })}
              placeholder="400051"
              required
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
            <VetanButton variant="outlined" onClick={() => setOpenModal(false)}>
              Cancel
            </VetanButton>
            <VetanButton type="submit" variant="contained" color="primary" loading={saving}>
              Save Branch
            </VetanButton>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
