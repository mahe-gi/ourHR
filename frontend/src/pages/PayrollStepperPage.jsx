import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Grid, Typography, Alert, Divider } from '@mui/material';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';

import { payrollApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import VetanCard from '../components/ui/VetanCard';
import VetanButton from '../components/ui/VetanButton';
import VetanBadge from '../components/ui/VetanBadge';
import PayrollStepper from '../components/domain/PayrollStepper';
import StatutoryBreakdownCard from '../components/domain/StatutoryBreakdownCard';
import WageCodeValidator from '../components/domain/WageCodeValidator';

export default function PayrollStepperPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState(0);
  const [run, setRun] = useState(null);
  const [calculating, setCalculating] = useState(false);
  const [disbursing, setDisbursing] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');

  useEffect(() => {
    const fetchRun = async () => {
      if (!id || id === 'stepper') return;
      try {
        const data = await payrollApi.getPayrollRun(id);
        setRun(data);
      } catch (e) {
        console.error(e);
      }
    };
    fetchRun();
  }, [id]);

  const handleCalculate = async () => {
    setCalculating(true);
    setStatusMessage('');
    try {
      if (id && id !== 'stepper') {
        const updated = await payrollApi.calculatePayroll(id);
        setRun(updated);
      }
      setStatusMessage('Statutory payroll calculation completed successfully.');
      setActiveStep(2);
    } catch (err) {
      setStatusMessage(err.message || 'Calculation error');
    } finally {
      setCalculating(false);
    }
  };

  const handleDisburse = async () => {
    setDisbursing(true);
    try {
      if (id && id !== 'stepper') {
        const updated = await payrollApi.disbursePayroll(id);
        setRun(updated);
      }
      setStatusMessage('Payroll disbursed and payslips finalized as immutable records.');
      setActiveStep(5);
    } catch (err) {
      setStatusMessage(err.message || 'Disbursal failed');
    } finally {
      setDisbursing(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title={`Payroll Cycle: ${run?.yearMonth || '2026-11'}`}
        subtitle="6-step Indian statutory wage computation, tax deductions, and disbursal lock"
        action={
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <VetanButton
              variant="outlined"
              startIcon={<ArrowBackOutlinedIcon />}
              onClick={() => navigate('/payroll')}
            >
              All Runs
            </VetanButton>
            <VetanBadge status={run?.status || 'CALCULATED'} />
          </Box>
        }
      />

      <VetanCard sx={{ mb: 3 }}>
        <PayrollStepper activeStep={activeStep} onStepClick={(s) => setActiveStep(s)} />
      </VetanCard>

      {statusMessage && (
        <Alert
          severity="info"
          icon={<CheckCircleOutlinedIcon sx={{ color: '#10B981' }} />}
          sx={{ mb: 3, backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10B981' }}
        >
          {statusMessage}
        </Alert>
      )}

      {/* STEP 0: Attendance Sync */}
      {activeStep === 0 && (
        <VetanCard title="Step 1: Attendance Verification & LOP Sync">
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
            Attendance records have been frozen and validated against late marks and the Indian Sandwich leave rule.
          </Typography>
          <Box sx={{ p: 2, backgroundColor: '#1E293B', borderRadius: '8px', mb: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#F8FAFC' }}>
              Locked Attendance Inputs:
            </Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
              Cycle Days: 30 | Working Days: 22 | Paid Leaves: 2 | Loss of Pay (LOP) Days: 0.0
            </Typography>
          </Box>
          <VetanButton variant="contained" color="primary" onClick={() => setActiveStep(1)}>
            Confirm Attendance & Proceed
          </VetanButton>
        </VetanCard>
      )}

      {/* STEP 1: Salary Review & 50% Wage Code */}
      {activeStep === 1 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={7}>
            <VetanCard title="Step 2: Base Wages & Allowances">
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
                Review base wage components and verify compliance with the 50% Wage Code rule.
              </Typography>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Basic Pay (50%)</Typography>
                <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                  INR 30,000.00
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">House Rent Allowance (HRA)</Typography>
                <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                  INR 15,000.00
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="body2">Special Allowance</Typography>
                <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                  INR 15,000.00
                </Typography>
              </Box>
              <VetanButton
                variant="contained"
                color="primary"
                startIcon={<CalculateOutlinedIcon />}
                loading={calculating}
                onClick={handleCalculate}
              >
                Execute Statutory Calculation
              </VetanButton>
            </VetanCard>
          </Grid>
          <Grid item xs={12} md={5}>
            <WageCodeValidator basic={30000} grossSalary={60000} />
          </Grid>
        </Grid>
      )}

      {/* STEP 2: Statutory Deductions */}
      {activeStep === 2 && (
        <Box>
          <StatutoryBreakdownCard
            title="Step 3: EPF, ESIC, PT & LWF Breakdown"
            breakdown={{
              employeePf: 1800,
              employerEpf: 550,
              employerEps: 1250,
              employeeEsic: 0,
              employerEsic: 0,
              professionalTax: 200,
              labourWelfareFund: 20,
              tds: 2500,
              regime: 'NEW',
            }}
          />
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
            <VetanButton variant="outlined" onClick={() => setActiveStep(1)}>
              Back
            </VetanButton>
            <VetanButton variant="contained" color="primary" onClick={() => setActiveStep(3)}>
              Proceed to Tax Projection
            </VetanButton>
          </Box>
        </Box>
      )}

      {/* STEP 3: TDS Calculation */}
      {activeStep === 3 && (
        <VetanCard title="Step 4: Income Tax / TDS Projection (Old vs New Regime)">
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
            Dynamic tax projection computes annual taxable income, factoring in Form 12B previous employment income and chosen regime.
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6}>
              <Box sx={{ p: 2, backgroundColor: '#1E293B', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#10B981' }}>
                  Selected: New Tax Regime (Section 115BAC)
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 700, my: 1, fontVariantNumeric: 'tabular-nums' }}>
                  INR 2,500.00 / mo
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  Standard Deduction of INR 75,000 applied. Lower slab rates with zero exemptions needed.
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Box sx={{ p: 2, backgroundColor: '#1E293B', borderRadius: '8px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#94A3B8' }}>
                  Comparison: Old Tax Regime
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 700, my: 1, fontVariantNumeric: 'tabular-nums', color: '#94A3B8' }}>
                  INR 3,250.00 / mo
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  Higher monthly deduction due to insufficient Chapter VI-A investment proofs declared.
                </Typography>
              </Box>
            </Grid>
          </Grid>
          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
            <VetanButton variant="outlined" onClick={() => setActiveStep(2)}>
              Back
            </VetanButton>
            <VetanButton variant="contained" color="primary" onClick={() => setActiveStep(4)}>
              Proceed to CA Review
            </VetanButton>
          </Box>
        </VetanCard>
      )}

      {/* STEP 4: CA Review */}
      {activeStep === 4 && (
        <VetanCard title="Step 5: Chartered Accountant Audit & Review">
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
            Statutory figures verified for EPFO ECR compliance and bank payout batches.
          </Typography>
          <Box sx={{ p: 2, backgroundColor: '#1E293B', borderRadius: '8px', mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="body2">Total Gross Wages</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                INR 60,000.00
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="body2">Total Employee Deductions (EPF + PT + TDS)</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#EF4444', fontVariantNumeric: 'tabular-nums' }}>
                - INR 4,520.00
              </Typography>
            </Box>
            <Divider sx={{ my: 1, borderColor: 'rgba(255, 255, 255, 0.08)' }} />
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>Total Net Salary Payable</Typography>
              <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#10B981', fontVariantNumeric: 'tabular-nums' }}>
                INR 55,480.00
              </Typography>
            </Box>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
            <VetanButton variant="outlined" onClick={() => setActiveStep(3)}>
              Back
            </VetanButton>
            <VetanButton
              variant="contained"
              color="success"
              startIcon={<SendOutlinedIcon />}
              loading={disbursing}
              onClick={handleDisburse}
            >
              Approve & Disburse Payroll
            </VetanButton>
          </Box>
        </VetanCard>
      )}

      {/* STEP 5: Disbursed */}
      {activeStep === 5 && (
        <VetanCard title="Step 6: Payout Finalized & Immutable">
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <CheckCircleOutlinedIcon sx={{ fontSize: 64, color: '#10B981', mb: 2 }} />
            <Typography variant="h5" sx={{ fontWeight: 700, color: '#F8FAFC', mb: 1 }}>
              Payroll Disbursed Successfully!
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary', maxWidth: 480, mx: 'auto', mb: 3 }}>
              Per the Immutable Payroll Law, payslip line items for this run have been permanently sealed. You can now download the EPFO ECR .txt challan and Bank NEFT/RTGS CSV.
            </Typography>
            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2 }}>
              <VetanButton
                variant="contained"
                color="primary"
                onClick={() => navigate('/ca-workspace')}
              >
                Go to Compliance & Exporters Workspace
              </VetanButton>
            </Box>
          </Box>
        </VetanCard>
      )}
    </Box>
  );
}
