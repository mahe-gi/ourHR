import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import theme from './theme/theme';
import { AuthProvider, useAuth } from './context/AuthContext';

import AppLayout from './components/layout/AppLayout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Branches from './pages/Branches';
import Employees from './pages/Employees';
import Onboarding from './pages/Onboarding';
import Attendance from './pages/Attendance';
import Leaves from './pages/Leaves';
import AttendanceFreeze from './pages/AttendanceFreeze';
import Payroll from './pages/Payroll';
import PayrollStepperPage from './pages/PayrollStepperPage';
import Consultants from './pages/Consultants';
import CaWorkspace from './pages/CaWorkspace';
import EssHome from './pages/EssHome';
import EssPayslips from './pages/EssPayslips';
import EssTax from './pages/EssTax';

function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  if (loading) {
    return null;
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="branches" element={<Branches />} />
              <Route path="employees" element={<Employees />} />
              <Route path="onboarding" element={<Onboarding />} />
              <Route path="attendance" element={<Attendance />} />
              <Route path="attendance/freeze" element={<AttendanceFreeze />} />
              <Route path="leaves" element={<Leaves />} />
              <Route path="payroll" element={<Payroll />} />
              <Route path="payroll/:id" element={<PayrollStepperPage />} />
              <Route path="payroll/consultants" element={<Consultants />} />
              <Route path="ca-workspace" element={<CaWorkspace />} />
              <Route path="ess" element={<EssHome />} />
              <Route path="ess/payslips" element={<EssPayslips />} />
              <Route path="ess/tax" element={<EssTax />} />
            </Route>

            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}
