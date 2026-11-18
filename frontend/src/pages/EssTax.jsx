import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Grid, Typography, RadioGroup, FormControlLabel, Radio, Alert, Divider } from '@mui/material';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';

import PageHeader from '../components/ui/PageHeader';
import VetanCard from '../components/ui/VetanCard';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';

export default function EssTax() {
  const navigate = useNavigate();
  const [regime, setRegime] = useState('NEW');
  const [saved, setSaved] = useState(false);

  const [declarations, setDeclarations] = useState({
    sec80c: '150000',
    sec80d: '25000',
    hraAnnualRent: '240000',
    nps80ccd: '50000',
    homeLoanInterest: '200000',
  });

  const handleChange = (field) => (e) => {
    setDeclarations({ ...declarations, [field]: e.target.value });
  };

  const handleSave = (e) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <Box>
      <PageHeader
        title="Income Tax Regime & Declarations (Form 12BB)"
        subtitle="Select statutory tax regime and submit annual investment declarations for TDS withholding optimization"
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

      {saved && (
        <Alert severity="success" sx={{ mb: 3, backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10B981' }}>
          Tax regime election and Form 12BB declarations submitted for CA tax audit.
        </Alert>
      )}

      <form onSubmit={handleSave}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <VetanCard title="Select Income Tax Regime (FY 2026-27)">
              <RadioGroup
                value={regime}
                onChange={(e) => setRegime(e.target.value)}
                sx={{ gap: 2, mt: 1 }}
              >
                <Box
                  sx={{
                    p: 2,
                    borderRadius: '8px',
                    border: `1px solid ${regime === 'NEW' ? '#6366F1' : 'rgba(255, 255, 255, 0.08)'}`,
                    backgroundColor: regime === 'NEW' ? 'rgba(99, 102, 241, 0.08)' : '#1E293B',
                  }}
                >
                  <FormControlLabel
                    value="NEW"
                    control={<Radio sx={{ color: '#818CF8' }} />}
                    label={
                      <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                          New Tax Regime (Section 115BAC) — Default
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                          Standard deduction of INR 75,000 with lower slab rates. No investment proofs required.
                        </Typography>
                      </Box>
                    }
                  />
                </Box>

                <Box
                  sx={{
                    p: 2,
                    borderRadius: '8px',
                    border: `1px solid ${regime === 'OLD' ? '#6366F1' : 'rgba(255, 255, 255, 0.08)'}`,
                    backgroundColor: regime === 'OLD' ? 'rgba(99, 102, 241, 0.08)' : '#1E293B',
                  }}
                >
                  <FormControlLabel
                    value="OLD"
                    control={<Radio sx={{ color: '#818CF8' }} />}
                    label={
                      <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
                          Old Tax Regime (With Deductions)
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                          Eligible for Chapter VI-A deductions (80C up to INR 1.5L, 80D health insurance, HRA, Home Loan).
                        </Typography>
                      </Box>
                    }
                  />
                </Box>
              </RadioGroup>
            </VetanCard>
          </Grid>

          <Grid item xs={12} md={6}>
            <VetanCard title="Form 12BB Investment Declarations (INR)">
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
                Applicable if Old Tax Regime is chosen or for CA compliance reporting.
              </Typography>

              <VetanInput
                label="Section 80C (PPF, ELSS, Life Insurance, EPF)"
                type="number"
                value={declarations.sec80c}
                onChange={handleChange('sec80c')}
                disabled={regime === 'NEW'}
              />
              <VetanInput
                label="Section 80D (Health Insurance Premium)"
                type="number"
                value={declarations.sec80d}
                onChange={handleChange('sec80d')}
                disabled={regime === 'NEW'}
              />
              <VetanInput
                label="Annual House Rent Paid (for HRA Exemption)"
                type="number"
                value={declarations.hraAnnualRent}
                onChange={handleChange('hraAnnualRent')}
                disabled={regime === 'NEW'}
              />
              <VetanInput
                label="Section 80CCD(1B) (National Pension Scheme - NPS)"
                type="number"
                value={declarations.nps80ccd}
                onChange={handleChange('nps80ccd')}
                disabled={regime === 'NEW'}
              />

              <VetanButton
                type="submit"
                variant="contained"
                color="primary"
                startIcon={<SaveOutlinedIcon />}
                sx={{ mt: 1 }}
              >
                Save Tax Declaration
              </VetanButton>
            </VetanCard>
          </Grid>
        </Grid>
      </form>
    </Box>
  );
}
