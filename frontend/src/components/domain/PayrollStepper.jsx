import React from 'react';
import { Stepper, Step, StepLabel, Box } from '@mui/material';

const STEPS = [
  'Attendance Sync',
  'Salary Review',
  'Statutory Deductions',
  'TDS Calculation',
  'CA Audit Review',
  'Disbursal',
];

export default function PayrollStepper({ activeStep = 0, onStepClick }) {
  return (
    <Box sx={{ width: '100%', py: 2 }}>
      <Stepper
        activeStep={activeStep}
        alternativeLabel
        sx={{
          '& .MuiStepLabel-label': {
            color: 'text.secondary',
            fontSize: '0.8rem',
            fontWeight: 500,
            mt: 1,
            '&.Mui-active': {
              color: 'primary.light',
              fontWeight: 700,
            },
            '&.Mui-completed': {
              color: '#10B981',
              fontWeight: 600,
            },
          },
          '& .MuiStepIcon-root': {
            color: '#1E293B',
            border: '1px solid rgba(255, 255, 255, 0.15)',
            borderRadius: '50%',
            '&.Mui-active': {
              color: 'primary.main',
              borderColor: 'primary.light',
            },
            '&.Mui-completed': {
              color: '#10B981',
              borderColor: '#10B981',
            },
          },
        }}
      >
        {STEPS.map((label, index) => (
          <Step
            key={label}
            onClick={() => onStepClick && onStepClick(index)}
            sx={{ cursor: onStepClick ? 'pointer' : 'default' }}
          >
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>
    </Box>
  );
}
