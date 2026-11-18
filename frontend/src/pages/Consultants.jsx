import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography } from '@mui/material';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';

import { payrollApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanBadge from '../components/ui/VetanBadge';
import KycMaskedViewer from '../components/domain/KycMaskedViewer';

export default function Consultants() {
  const navigate = useNavigate();
  const [consultants, setConsultants] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchConsultants = async () => {
      setLoading(true);
      try {
        const data = await payrollApi.getConsultants();
        setConsultants(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchConsultants();
  }, []);

  const columns = [
    {
      field: 'name',
      headerName: 'Consultant / Vendor',
      renderCell: ({ row }) => (
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
            {row.name || 'Advisory Partner'}
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            {row.email || 'consultant@partner.com'}
          </Typography>
        </Box>
      ),
    },
    {
      field: 'tdsSection',
      headerName: 'TDS Section',
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 600, color: '#818CF8' }}>
          {row.tdsSection || 'Sec 194J (10%)'}
        </Typography>
      ),
    },
    {
      field: 'pan',
      headerName: 'PAN',
      renderCell: ({ row }) => (
        <KycMaskedViewer label="PAN" value={row.pan || 'ABCDE1234F'} type="pan" />
      ),
    },
    {
      field: 'retainerAmount',
      headerName: 'Gross Retainer',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          INR {(row.retainerAmount || 50000).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Typography>
      ),
    },
    {
      field: 'tdsDeduction',
      headerName: 'TDS Withheld',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ color: '#EF4444', fontVariantNumeric: 'tabular-nums' }}>
          - INR {(row.tdsDeduction || 5000).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Typography>
      ),
    },
    {
      field: 'netPayable',
      headerName: 'Net Payable',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 700, color: '#10B981', fontVariantNumeric: 'tabular-nums' }}>
          INR {(row.netPayable || 45000).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
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
        title="Consultant Stipends & Retainers"
        subtitle="Manage professional retainers with statutory Section 194J (10% professional / 2% technical) and 194C TDS withholding"
        action={
          <VetanButton
            variant="outlined"
            startIcon={<ArrowBackOutlinedIcon />}
            onClick={() => navigate('/payroll')}
          >
            Back to Payroll
          </VetanButton>
        }
      />

      <DataTable
        columns={columns}
        rows={consultants}
        loading={loading}
        searchPlaceholder="Filter consultants..."
        searchField="name"
        emptyTitle="No consultant retainers logged"
        emptySubtitle="Contractors and external advisors paid under Section 194J/194C appear here."
      />
    </Box>
  );
}
