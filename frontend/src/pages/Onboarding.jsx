import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Grid, Typography, Stepper, Step, StepLabel, Alert, Select, MenuItem } from '@mui/material';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import CheckOutlinedIcon from '@mui/icons-material/CheckOutlined';

import { employeeApi, companyApi } from '../api';
import { useAuth } from '../context/AuthContext';
import PageHeader from '../components/ui/PageHeader';
import VetanCard from '../components/ui/VetanCard';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';
import WageCodeValidator from '../components/domain/WageCodeValidator';

const STEPS = ['Personal & Contact', 'Statutory KYC & Bank', 'Salary & Wage Code'];

export default function Onboarding() {
  const navigate = useNavigate();
  const { currentBranch, branches: authBranches } = useAuth();
  const [branches, setBranches] = useState([]);
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [form, setForm] = useState({
    employeeCode: 'EMP-101',
    branchId: '',
    firstName: '',
    lastName: '',
    officialEmail: '',
    phoneNumber: '',
    dateOfBirth: '1995-01-01',
    gender: 'MALE',
    department: 'Engineering',
    designation: 'Software Engineer',
    dateOfJoining: new Date().toISOString().split('T')[0],
    pan: '',
    aadhaarNumber: '',
    bankAccountNumber: '',
    bankIfscCode: '',
    uanNumber: '',
    basicSalary: 30000,
    hra: 15000,
    specialAllowance: 15000,
  });

  useEffect(() => {
    const loadBranches = async () => {
      try {
        const list = await companyApi.getBranches();
        if (list && list.length > 0) {
          setBranches(list);
          const defaultBranch = currentBranch?.id || list[0].id;
          setForm((prev) => ({ ...prev, branchId: defaultBranch }));
        } else if (authBranches && authBranches.length > 0) {
          setBranches(authBranches);
          setForm((prev) => ({ ...prev, branchId: authBranches[0].id }));
        }
      } catch (e) {
        if (authBranches && authBranches.length > 0) {
          setBranches(authBranches);
          setForm((prev) => ({ ...prev, branchId: authBranches[0].id }));
        }
      }
    };
    loadBranches();
  }, [currentBranch, authBranches]);

  const grossSalary =
    Number(form.basicSalary || 0) + Number(form.hra || 0) + Number(form.specialAllowance || 0);

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
  };

  const handleNext = () => {
    if (activeStep === 0) {
      if (!form.firstName || !form.lastName || !form.officialEmail) {
        setError('First name, last name, and official email are required.');
        return;
      }
      if (!form.branchId && branches.length > 0) {
        setForm((prev) => ({ ...prev, branchId: branches[0].id }));
      }
    }
    if (activeStep === 1) {
      if (!form.pan || !form.bankAccountNumber) {
        setError('PAN and Bank Account number are required for statutory onboarding.');
        return;
      }
    }
    setError('');
    setActiveStep((prev) => prev + 1);
  };

  const handleBack = () => {
    setError('');
    setActiveStep((prev) => prev - 1);
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError('');
    try {
      const selectedBranchId = form.branchId || branches[0]?.id;
      if (!selectedBranchId) {
        throw new Error('No active branch found. Please create a branch first.');
      }

      // Step 1: Create Employee Record
      const empPayload = {
        employeeCode: form.employeeCode || `EMP-${Math.floor(100 + Math.random() * 900)}`,
        branchId: selectedBranchId,
        firstName: form.firstName,
        lastName: form.lastName,
        officialEmail: form.officialEmail,
        dateOfBirth: form.dateOfBirth || '1995-01-01',
        gender: form.gender || 'MALE',
        dateOfJoining: form.dateOfJoining || new Date().toISOString().split('T')[0],
        workerType: 'REGULAR',
        department: form.department || 'Engineering',
        designation: form.designation || 'Software Engineer',
      };

      const createdEmp = await employeeApi.createEmployee(empPayload);
      const empId = createdEmp?.id;

      if (empId) {
        // Step 2: Create Salary Structure
        try {
          await employeeApi.createSalaryStructure(empId, {
            annualCtc: grossSalary * 12,
            monthlyGross: grossSalary,
            basicPay: Number(form.basicSalary),
            daPay: 0,
            hraPay: Number(form.hra),
            specialAllowance: Number(form.specialAllowance),
            effectiveFrom: form.dateOfJoining || new Date().toISOString().split('T')[0],
            pfOptOut: false,
          });
        } catch (salErr) {
          console.warn('Salary structure notice:', salErr);
        }

        // Step 3: Save KYC Vault Record if provided
        if (form.pan && form.bankAccountNumber) {
          try {
            await employeeApi.saveKycVault(empId, {
              panNumber: form.pan.trim().toUpperCase(),
              aadhaarNumber: form.aadhaarNumber ? form.aadhaarNumber.replace(/\D/g, '') : undefined,
              bankAccountNumber: form.bankAccountNumber.trim(),
              bankIfsc: form.bankIfscCode ? form.bankIfscCode.trim().toUpperCase() : 'HDFC0001234',
              bankName: 'HDFC Bank',
              uan: form.uanNumber ? form.uanNumber.replace(/\D/g, '') : undefined,
            });
          } catch (kycErr) {
            console.warn('KYC vault notice:', kycErr);
          }
        }
      }

      navigate('/employees');
    } catch (err) {
      setError(err.message || 'Failed to onboard employee.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Employee Onboarding"
        subtitle="3-step statutory onboarding with automated 50% Wage Code compliance validation"
        action={
          <VetanButton
            variant="outlined"
            startIcon={<ArrowBackOutlinedIcon />}
            onClick={() => navigate('/employees')}
          >
            Cancel
          </VetanButton>
        }
      />

      <VetanCard sx={{ mb: 3 }}>
        <Stepper activeStep={activeStep} alternativeLabel>
          {STEPS.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>
      </VetanCard>

      {error && (
        <Alert severity="error" sx={{ mb: 3, backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#EF4444' }}>
          {error}
        </Alert>
      )}

      {activeStep === 0 && (
        <VetanCard title="Personal & Official Information">
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Employee Code"
                value={form.employeeCode}
                onChange={handleChange('employeeCode')}
                placeholder="EMP-101"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" sx={{ display: 'block', mb: 0.75, color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', fontSize: '0.72rem' }}>
                Assigned Branch
              </Typography>
              <Select
                fullWidth
                size="small"
                value={form.branchId || (branches[0] ? branches[0].id : '')}
                onChange={handleChange('branchId')}
                sx={{ backgroundColor: '#0F172A', borderRadius: '8px', mb: 2, fontSize: '0.875rem' }}
              >
                {branches.map((b) => (
                  <MenuItem key={b.id} value={b.id}>
                    {b.branchName || b.name} ({b.stateCode || 'HQ'})
                  </MenuItem>
                ))}
              </Select>
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="First Name"
                value={form.firstName}
                onChange={handleChange('firstName')}
                placeholder="Aarav"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Last Name"
                value={form.lastName}
                onChange={handleChange('lastName')}
                placeholder="Patel"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Official Email"
                type="email"
                value={form.officialEmail}
                onChange={handleChange('officialEmail')}
                placeholder="aarav.patel@company.com"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Mobile Number"
                value={form.phoneNumber}
                onChange={handleChange('phoneNumber')}
                placeholder="9876543210"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Date of Birth"
                type="date"
                value={form.dateOfBirth}
                onChange={handleChange('dateOfBirth')}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Date of Joining"
                type="date"
                value={form.dateOfJoining}
                onChange={handleChange('dateOfJoining')}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Department"
                value={form.department}
                onChange={handleChange('department')}
                placeholder="Engineering"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Designation"
                value={form.designation}
                onChange={handleChange('designation')}
                placeholder="Software Engineer"
              />
            </Grid>
          </Grid>
        </VetanCard>
      )}

      {activeStep === 1 && (
        <VetanCard title="Statutory KYC & Banking Records">
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
            Government identifiers are encrypted at rest and masked per the Sensitive ID Redaction standard.
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Permanent Account Number (PAN)"
                value={form.pan}
                onChange={handleChange('pan')}
                placeholder="ABCDE1234F"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Aadhaar Number (12 Digits)"
                value={form.aadhaarNumber}
                onChange={handleChange('aadhaarNumber')}
                placeholder="[Aadhaar Redacted]"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Bank Account Number"
                value={form.bankAccountNumber}
                onChange={handleChange('bankAccountNumber')}
                placeholder="50100234567890"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Bank IFSC Code"
                value={form.bankIfscCode}
                onChange={handleChange('bankIfscCode')}
                placeholder="HDFC0001234"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Universal Account Number (UAN for EPF)"
                value={form.uanNumber}
                onChange={handleChange('uanNumber')}
                placeholder="1009-XXXX-XXXX (UAN)"
              />
            </Grid>
          </Grid>
        </VetanCard>
      )}

      {activeStep === 2 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={7}>
            <VetanCard title="Monthly Salary Structure (INR)">
              <VetanInput
                label="Basic Wage (Must be >= 50% of Gross)"
                type="number"
                value={form.basicSalary}
                onChange={handleChange('basicSalary')}
                required
              />
              <VetanInput
                label="House Rent Allowance (HRA)"
                type="number"
                value={form.hra}
                onChange={handleChange('hra')}
                required
              />
              <VetanInput
                label="Special Allowance"
                type="number"
                value={form.specialAllowance}
                onChange={handleChange('specialAllowance')}
                required
              />
            </VetanCard>
          </Grid>
          <Grid item xs={12} md={5}>
            <WageCodeValidator
              basic={form.basicSalary}
              grossSalary={grossSalary}
            />
          </Grid>
        </Grid>
      )}

      <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
        <VetanButton
          variant="outlined"
          disabled={activeStep === 0}
          onClick={handleBack}
        >
          Previous Step
        </VetanButton>

        {activeStep < STEPS.length - 1 ? (
          <VetanButton variant="contained" color="primary" onClick={handleNext}>
            Continue
          </VetanButton>
        ) : (
          <VetanButton
            variant="contained"
            color="success"
            startIcon={<CheckOutlinedIcon />}
            loading={loading}
            onClick={handleSubmit}
          >
            Complete Onboarding
          </VetanButton>
        )}
      </Box>
    </Box>
  );
}
