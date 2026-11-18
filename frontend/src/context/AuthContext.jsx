import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi, companyApi } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [company, setCompany] = useState(null);
  const [branches, setBranches] = useState([]);
  const [currentBranch, setCurrentBranch] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('vetan_token');
      if (!token || token === 'undefined' || token === 'null') {
        localStorage.removeItem('vetan_token');
        setLoading(false);
        return;
      }
      try {
        const userData = await authApi.getMe();
        const normUser = {
          ...userData,
          id: userData.userId || userData.id,
          roles: userData.roles || (userData.role ? [userData.role] : ['ROLE_FOUNDER']),
          role: (userData.roles && userData.roles[0]) ? userData.roles[0] : (userData.role || 'ROLE_FOUNDER'),
        };
        setUser(normUser);
        if (normUser.companyId) {
          try {
            const [companyData, branchList] = await Promise.all([
              companyApi.getCurrentCompany().catch(() => null),
              companyApi.getBranches().catch(() => []),
            ]);
            setCompany(companyData);
            setBranches(branchList || []);
            if (branchList && branchList.length > 0) {
              const hq = branchList.find((b) => b.isHq) || branchList[0];
              setCurrentBranch(hq);
            }
          } catch (e) {
            // Non-blocking metadata load
          }
        }
      } catch (err) {
        localStorage.removeItem('vetan_token');
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (email, password) => {
    setError(null);
    try {
      const data = await authApi.login({ email, password });
      const token = data.accessToken || data.token;
      if (token) {
        localStorage.setItem('vetan_token', token);
      }
      const normUser = {
        ...data,
        id: data.userId || data.id,
        roles: data.roles || (data.role ? [data.role] : ['ROLE_FOUNDER']),
        role: (data.roles && data.roles[0]) ? data.roles[0] : (data.role || 'ROLE_FOUNDER'),
      };
      setUser(normUser);
      if (normUser.companyId) {
        try {
          const [companyData, branchList] = await Promise.all([
            companyApi.getCurrentCompany().catch(() => null),
            companyApi.getBranches().catch(() => []),
          ]);
          setCompany(companyData);
          setBranches(branchList || []);
          if (branchList && branchList.length > 0) {
            const hq = branchList.find((b) => b.isHq) || branchList[0];
            setCurrentBranch(hq);
          }
        } catch (e) {
          // Non-blocking metadata load
        }
      }
      return data;
    } catch (err) {
      setError(err.message || 'Login failed');
      throw err;
    }
  };

  const registerCompany = async (payload) => {
    setError(null);
    try {
      const formattedPayload = {
        legalName: payload.legalName || payload.companyName,
        tradeName: payload.tradeName || payload.companyName,
        panNumber: payload.panNumber || payload.pan,
        tanNumber: payload.tanNumber || 'BLRV12345D',
        gstin: payload.gstin || undefined,
        signatoryName: payload.signatoryName || payload.adminFullName,
        signatoryDesignation: payload.signatoryDesignation || 'Founder',
        stateCode: payload.stateCode || 'KA',
        city: payload.city || 'Bengaluru',
        addressLine: payload.addressLine || 'Main Office Road',
        pincode: payload.pincode || '560001',
        adminEmail: payload.adminEmail,
        adminPassword: payload.adminPassword,
        adminFullName: payload.adminFullName,
        adminPhoneNumber: payload.adminPhoneNumber || payload.phoneNumber || '9876543210',
      };
      await authApi.registerCompany(formattedPayload);
      const loginData = await login(payload.adminEmail, payload.adminPassword);
      return loginData;
    } catch (err) {
      setError(err.message || 'Company registration failed');
      throw err;
    }
  };

  const logout = () => {
    localStorage.removeItem('vetan_token');
    setUser(null);
    setCompany(null);
    setBranches([]);
    setCurrentBranch(null);
  };

  const value = {
    user,
    company,
    branches,
    currentBranch,
    setCurrentBranch,
    loading,
    error,
    login,
    registerCompany,
    logout,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
