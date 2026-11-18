import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Box, Typography, Paper, Alert, Grid, Select, MenuItem } from '@mui/material';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import VetanInput from '../components/ui/VetanInput';
import VetanButton from '../components/ui/VetanButton';
import { useAuth } from '../context/AuthContext';

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

export default function Register() {
  const navigate = useNavigate();
  const { registerCompany } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [form, setForm] = useState({
    companyName: '',
    cin: '',
    pan: '',
    gstin: '',
    adminFullName: '',
    adminEmail: '',
    adminPassword: '',
    branchName: 'Headquarters',
    stateCode: 'KA',
  });

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await registerCompany(form);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#090D16',
        p: 3,
      }}
    >
      <Paper
        sx={{
          p: 4,
          maxWidth: 640,
          width: '100%',
          backgroundColor: '#0F172A',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: '12px',
        }}
      >
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <img src="/brand/vetan-icon.svg" alt="Vetan Logo" style={{ width: 36, height: 36, marginBottom: 8 }} />
          <Typography variant="h5" sx={{ fontWeight: 700, color: '#F8FAFC' }}>
            Setup Your Company on Vetan
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
            Automated statutory compliance and payroll for Indian MSMEs
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2.5, backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#EF4444' }}>
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <Typography variant="subtitle2" sx={{ color: '#818CF8', mb: 1.5, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Company Legal Details
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Company Legal Name"
                value={form.companyName}
                onChange={handleChange('companyName')}
                placeholder="Acme Technologies Pvt Ltd"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Company PAN"
                value={form.pan}
                onChange={handleChange('pan')}
                placeholder="ABCDE1234F"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="CIN (Optional)"
                value={form.cin}
                onChange={handleChange('cin')}
                placeholder="U72200KA2024PTC123456"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="GSTIN (Optional)"
                value={form.gstin}
                onChange={handleChange('gstin')}
                placeholder="29ABCDE1234F1Z5"
              />
            </Grid>
          </Grid>

          <Typography variant="subtitle2" sx={{ color: '#818CF8', mt: 2, mb: 1.5, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Primary Headquarters Branch
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Branch Name"
                value={form.branchName}
                onChange={handleChange('branchName')}
                placeholder="Headquarters"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" sx={{ display: 'block', mb: 0.75, color: 'text.secondary', fontWeight: 600, textTransform: 'uppercase', fontSize: '0.72rem' }}>
                State (for PT & LWF Rules)
              </Typography>
              <Select
                fullWidth
                size="small"
                value={form.stateCode}
                onChange={handleChange('stateCode')}
                sx={{
                  backgroundColor: '#0F172A',
                  borderRadius: '8px',
                  fontSize: '0.875rem',
                  '& fieldset': { borderColor: 'rgba(255, 255, 255, 0.12)' },
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
          </Grid>

          <Typography variant="subtitle2" sx={{ color: '#818CF8', mt: 1, mb: 1.5, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Administrator Account
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <VetanInput
                label="Admin Full Name"
                value={form.adminFullName}
                onChange={handleChange('adminFullName')}
                placeholder="Rajesh Sharma"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Work Email"
                type="email"
                value={form.adminEmail}
                onChange={handleChange('adminEmail')}
                placeholder="rajesh@acme.com"
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <VetanInput
                label="Password"
                type="password"
                value={form.adminPassword}
                onChange={handleChange('adminPassword')}
                placeholder="••••••••"
                required
              />
            </Grid>
          </Grid>

          <VetanButton
            type="submit"
            fullWidth
            size="large"
            loading={loading}
            startIcon={<BusinessOutlinedIcon />}
            sx={{ mt: 2 }}
          >
            Create Company & Workspace
          </VetanButton>
        </form>

        <Box sx={{ textAlign: 'center', mt: 3 }}>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: '#818CF8', textDecoration: 'none', fontWeight: 600 }}>
              Sign In
            </Link>
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
}
