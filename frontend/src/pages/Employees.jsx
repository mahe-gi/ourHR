import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Box, Typography, Chip, Tooltip, IconButton, Alert } from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';

import { employeeApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanBadge from '../components/ui/VetanBadge';
import KycMaskedViewer from '../components/domain/KycMaskedViewer';

export default function Employees() {
  const navigate = useNavigate();
  const location = useLocation();
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchEmployees = async () => {
    setLoading(true);
    try {
      const data = await employeeApi.getEmployees();
      const list = Array.isArray(data) ? data : (data?.content || []);
      setEmployees(list);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  const columns = [
    {
      field: 'employeeCode',
      headerName: 'Emp Code',
      width: 120,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontFamily: 'monospace', fontWeight: 600, color: '#818CF8' }}>
          {row.employeeCode || `EMP-${row.id?.slice(0, 5)}`}
        </Typography>
      ),
    },
    {
      field: 'fullName',
      headerName: 'Employee Name',
      minWidth: 200,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
          <PersonOutlineIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
          <Box>
            <Typography variant="body2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
              {row.firstName ? `${row.firstName} ${row.lastName || ''}` : (row.fullName || 'Employee')}
            </Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
              {row.officialEmail || row.email}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      field: 'department',
      headerName: 'Department',
      renderCell: ({ row }) => row.department || 'Engineering',
    },
    {
      field: 'designation',
      headerName: 'Designation',
      renderCell: ({ row }) => row.designation || 'Software Engineer',
    },
    {
      field: 'kyc',
      headerName: 'KYC Aadhaar Masked',
      minWidth: 220,
      renderCell: ({ row }) => (
        <KycMaskedViewer
          label="Aadhaar"
          value={row.maskedAadhaar || row.aadhaarNumber || '[Aadhaar Redacted]'}
          type="aadhaar"
        />
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 110,
      renderCell: ({ row }) => <VetanBadge status={row.status || 'ACTIVE'} />,
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Employee Master Directory"
        subtitle="Manage personnel profiles, statutory salary structures, and KYC compliance records"
        action={
          <VetanButton
            variant="contained"
            color="primary"
            startIcon={<AddOutlinedIcon />}
            onClick={() => navigate('/onboarding')}
          >
            Add Employee
          </VetanButton>
        }
      />

      {location.state?.createdEmail && (
        <Alert severity="success" sx={{ mb: 3, backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10B981' }}>
          Employee profile created for <strong>{location.state.createdEmail}</strong>! Initial self-service password is <strong>Welcome@1234</strong>.
        </Alert>
      )}

      <DataTable
        columns={columns}
        rows={employees}
        loading={loading}
        searchPlaceholder="Search employees by name..."
        searchField="firstName"
        emptyTitle="No employees in directory"
        emptySubtitle="Start building your team by adding your first employee or contractor."
        emptyActionLabel="Add Employee"
        onEmptyAction={() => navigate('/onboarding')}
      />
    </Box>
  );
}
