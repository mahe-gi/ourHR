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
      if (!token) {
        setLoading(false);
        return;
      }
      try {
        const userData = await authApi.getMe();
        setUser(userData);
        if (userData.companyId) {
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
      localStorage.setItem('vetan_token', data.token);
      setUser(data.user);
      if (data.user?.companyId) {
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
      const data = await authApi.registerCompany(payload);
      localStorage.setItem('vetan_token', data.token);
      setUser(data.user);
      setCompany(data.company);
      const branchList = await companyApi.getBranches().catch(() => []);
      setBranches(branchList);
      if (branchList.length > 0) {
        setCurrentBranch(branchList[0]);
      }
      return data;
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
