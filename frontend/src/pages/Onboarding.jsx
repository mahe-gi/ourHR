import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Grid, Typography, Stepper, Step, StepLabel, Alert, Select, MenuItem } from '@mui/material';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import CheckOutlinedIcon from '@mui/icons-material/CheckOutlined';

import { employeeApi } from '../api';
import PageHeader from '../components/ui/PageHeader';
import VetanCard from '../components/ui/VetanCard';
import VetanButton from '../components/ui/VetanButton';
import VetanInput from '../components/ui/VetanInput';
import WageCodeValidator from '../components/domain/WageCodeValidator';

const STEPS = ['Personal & Contact', 'Statutory KYC & Bank', 'Salary & Wage Code'];

export default function Onboarding() {
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    officialEmail: '',
    phoneNumber: '',
    dateOfBirth: '1995-01-01',
    gender: 'MALE',
    department: 'Engineering',
    designation: 'Software Engineer',
    joiningDate: new Date().toISOString().split('T')[0],
    pan: '',
    aadhaarNumber: '',
    bankAccountNumber: '',
    bankIfscCode: '',
    uanNumber: '',
    basicSalary: 30000,
    hra: 15000,
    specialAllowance: 15000,
  });

  const grossSalary =
    Number(form.basicSalary || 0) + Number(form.hra || 0) + Number(form.specialAllowance || 0);

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
  };

  const handleNext = () => {
    if (activeStep === 0) {
      if (!form.firstName || !form.officialEmail) {
        setError('First name and email are mandatory.');
        return;
      }
    }
    if (activeStep === 1) {
      if (!form.pan || !form.aadhaarNumber) {
        setError('PAN and Aadhaar are required for statutory KYC compliance.');
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
      await employeeApi.createEmployee({
        ...form,
        ctc: grossSalary * 12,
      });
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
                required
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
