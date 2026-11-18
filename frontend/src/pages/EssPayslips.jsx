import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography } from '@mui/material';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';

import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import VetanButton from '../components/ui/VetanButton';
import VetanBadge from '../components/ui/VetanBadge';

export default function EssPayslips() {
  const navigate = useNavigate();
  const [payslips] = useState([
    {
      id: 'pay-2026-10',
      monthYear: 'October 2026',
      grossSalary: 60000,
      totalDeductions: 4520,
      netPay: 55480,
      status: 'DISBURSED',
    },
    {
      id: 'pay-2026-09',
      monthYear: 'September 2026',
      grossSalary: 60000,
      totalDeductions: 4520,
      netPay: 55480,
      status: 'DISBURSED',
    },
  ]);

  const handleDownloadPdf = (id) => {
    alert(`Downloading PDF Payslip for ${id}...`);
  };

  const columns = [
    {
      field: 'monthYear',
      headerName: 'Pay Period',
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
          {row.monthYear}
        </Typography>
      ),
    },
    {
      field: 'grossSalary',
      headerName: 'Gross Earnings',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          INR {row.grossSalary.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Typography>
      ),
    },
    {
      field: 'totalDeductions',
      headerName: 'Deductions (EPF/PT/TDS)',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ color: '#EF4444', fontVariantNumeric: 'tabular-nums' }}>
          - INR {row.totalDeductions.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Typography>
      ),
    },
    {
      field: 'netPay',
      headerName: 'Net Take-Home Pay',
      isNumeric: true,
      renderCell: ({ row }) => (
        <Typography variant="body2" sx={{ fontWeight: 700, color: '#10B981', fontVariantNumeric: 'tabular-nums' }}>
          INR {row.netPay.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </Typography>
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      renderCell: ({ row }) => <VetanBadge status={row.status} />,
    },
    {
      field: 'actions',
      headerName: 'Payslip',
      renderCell: ({ row }) => (
        <VetanButton
          variant="outlined"
          size="small"
          startIcon={<DownloadOutlinedIcon />}
          onClick={() => handleDownloadPdf(row.id)}
        >
          Download PDF
        </VetanButton>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        title="My Payslips & Tax Slips"
        subtitle="Download official digitally generated payslips with full statutory earnings and deduction breakdowns"
        action={
          <VetanButton
            variant="outlined"
            startIcon={<ArrowBackOutlinedIcon />}
            onClick={() => navigate('/ess')}
          >
            Back to Portal
          </VetanButton>
        }
      />

      <DataTable
        columns={columns}
        rows={payslips}
        searchPlaceholder="Filter payslips..."
        searchField="monthYear"
        emptyTitle="No payslips generated yet"
        emptySubtitle="Monthly payslips will appear here once payroll is disbursed."
      />
    </Box>
  );
}
