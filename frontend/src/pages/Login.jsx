import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Box, Typography, Paper, Alert, Chip } from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import VetanInput from '../components/ui/VetanInput';
import VetanButton from '../components/ui/VetanButton';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleQuickLogin = (demoEmail, demoPassword) => {
    setEmail(demoEmail);
    setPassword(demoPassword);
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please provide email and password.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const data = await login(email, password);
      const roles = data?.roles || (data?.role ? [data.role] : []);
      const isPureEmployee =
        roles.includes('ROLE_EMPLOYEE') &&
        !roles.includes('ROLE_FOUNDER') &&
        !roles.includes('ROLE_HR') &&
        !roles.includes('ROLE_FINANCE') &&
        !roles.includes('ROLE_CA');

      if (isPureEmployee) {
        navigate('/ess');
      } else if (
        roles.includes('ROLE_CA') &&
        !roles.includes('ROLE_FOUNDER') &&
        !roles.includes('ROLE_HR') &&
        !roles.includes('ROLE_FINANCE')
      ) {
        navigate('/ca-workspace');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.message || 'Invalid email or password.');
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
        p: 2,
      }}
    >
      <Paper
        sx={{
          p: 4,
          maxWidth: 400,
          width: '100%',
          backgroundColor: '#0F172A',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: '12px',
        }}
      >
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <img src="/brand/vetan-icon.svg" alt="Vetan Logo" style={{ width: 36, height: 36, marginBottom: 8 }} />
          <Typography variant="h5" sx={{ fontWeight: 700, color: '#F8FAFC' }}>
            Sign In to Vetan
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
            Indian MSME HRMS & Statutory Payroll
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2.5, backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#EF4444' }}>
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <VetanInput
            id="login-email"
            name="email"
            autoComplete="username"
            label="Email Address"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="admin@company.com"
            required
          />
          <VetanInput
            id="login-password"
            name="password"
            autoComplete="current-password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
          <VetanButton
            type="submit"
            fullWidth
            size="large"
            loading={loading}
            startIcon={<LockOutlinedIcon />}
            sx={{ mt: 1 }}
          >
            Sign In
          </VetanButton>
        </form>

        <Box sx={{ mt: 3, pt: 2, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 1, fontWeight: 600, textTransform: 'uppercase', fontSize: '0.68rem', letterSpacing: '0.04em' }}>
            Quick Demo Personas:
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
            <Chip
              label="Founder"
              size="small"
              onClick={() => handleQuickLogin('demo@vetan.in', 'Demo@1234')}
              sx={{ backgroundColor: '#1E293B', color: '#F8FAFC', cursor: 'pointer', '&:hover': { backgroundColor: '#334155' } }}
            />
            <Chip
              label="HR Manager"
              size="small"
              onClick={() => handleQuickLogin('hr@vetan.in', 'Welcome@1234')}
              sx={{ backgroundColor: '#1E293B', color: '#F8FAFC', cursor: 'pointer', '&:hover': { backgroundColor: '#334155' } }}
            />
            <Chip
              label="Finance"
              size="small"
              onClick={() => handleQuickLogin('finance@vetan.in', 'Welcome@1234')}
              sx={{ backgroundColor: '#1E293B', color: '#F8FAFC', cursor: 'pointer', '&:hover': { backgroundColor: '#334155' } }}
            />
            <Chip
              label="Chartered Accountant"
              size="small"
              onClick={() => handleQuickLogin('ca@vetan.in', 'Welcome@1234')}
              sx={{ backgroundColor: '#1E293B', color: '#F8FAFC', cursor: 'pointer', '&:hover': { backgroundColor: '#334155' } }}
            />
            <Chip
              label="Employee"
              size="small"
              onClick={() => handleQuickLogin('arun.kumar@vetan.in', 'Welcome@1234')}
              sx={{ backgroundColor: '#1E293B', color: '#F8FAFC', cursor: 'pointer', '&:hover': { backgroundColor: '#334155' } }}
            />
          </Box>
        </Box>

        <Box sx={{ textAlign: 'center', mt: 3 }}>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            Need to setup a new company?{' '}
            <Link to="/register" style={{ color: '#818CF8', textDecoration: 'none', fontWeight: 600 }}>
              Register Company
            </Link>
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
}
