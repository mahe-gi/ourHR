import React from 'react';
import { Box, Typography, Divider, Grid } from '@mui/material';
import VetanCard from '../ui/VetanCard';

export default function StatutoryBreakdownCard({
  breakdown = {},
  title = 'Indian Statutory Deductions',
}) {
  const {
    employeePf = 1800,
    employerEpf = 550,
    employerEps = 1250,
    employeeEsic = 0,
    employerEsic = 0,
    professionalTax = 200,
    labourWelfareFund = 20,
    tds = 2500,
    regime = 'NEW',
  } = breakdown;

  const totalEmployeeDeductions =
    Number(employeePf) + Number(employeeEsic) + Number(professionalTax) + Number(labourWelfareFund) + Number(tds);
  const totalEmployerContributions =
    Number(employerEpf) + Number(employerEps) + Number(employerEsic);

  const formatCurrency = (val) =>
    `INR ${Number(val).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

  return (
    <VetanCard title={title}>
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#818CF8', mb: 1.5, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Employee Deductions (from Gross)
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>EPF Employee (12%)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(employeePf)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>ESIC Employee (0.75%)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(employeeEsic)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>Professional Tax (PT)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(professionalTax)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>Labour Welfare Fund (LWF)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(labourWelfareFund)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>TDS ({regime} Regime)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(tds)}
              </Typography>
            </Box>
            <Divider sx={{ my: 0.5, borderColor: 'rgba(255, 255, 255, 0.08)' }} />
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>Total Employee Deductions</Typography>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#EF4444', fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(totalEmployeeDeductions)}
              </Typography>
            </Box>
          </Box>
        </Grid>

        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#10B981', mb: 1.5, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Employer Contributions (over Gross)
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>EPF Employer (3.67%)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(employerEpf)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>EPS Pension (8.33%)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(employerEps)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>ESIC Employer (3.25%)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(employerEsic)}
              </Typography>
            </Box>
            <Divider sx={{ my: 0.5, borderColor: 'rgba(255, 255, 255, 0.08)' }} />
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>Total Employer Liability</Typography>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#10B981', fontVariantNumeric: 'tabular-nums' }}>
                {formatCurrency(totalEmployerContributions)}
              </Typography>
            </Box>
          </Box>
        </Grid>
      </Grid>
    </VetanCard>
  );
}
