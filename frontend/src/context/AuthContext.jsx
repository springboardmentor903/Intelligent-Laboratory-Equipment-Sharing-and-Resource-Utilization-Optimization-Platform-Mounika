import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Restore session from localStorage on initial app load
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');

    if (savedToken && savedUser) {
      try {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  const handleAuthSuccess = (authData) => {
    const { token: jwtToken, ...userInfo } = authData;
    setToken(jwtToken);
    setUser(userInfo);
    localStorage.setItem('token', jwtToken);
    localStorage.setItem('user', JSON.stringify(userInfo));
  };

  const login = async (email, password) => {
    const res = await api.post('/auth/login', { email, password });
    if (res.success && res.data) {
      handleAuthSuccess(res.data);
      return res.data;
    }
    throw new Error(res.message || 'Login failed');
  };

  const loginWithGoogle = async (googlePayload) => {
    const res = await api.post('/auth/google', googlePayload);
    if (res.success && res.data) {
      handleAuthSuccess(res.data);
      return res.data;
    }
    throw new Error(res.message || 'Google authentication failed');
  };

  const register = async (userData) => {
    const res = await api.post('/auth/register', userData);
    if (res.success && res.data) {
      handleAuthSuccess(res.data);
      return res.data;
    }
    throw new Error(res.message || 'Registration failed');
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  const refreshProfile = async () => {
    try {
      const res = await api.get('/auth/me');
      if (res.success && res.data) {
        setUser((prev) => ({ ...prev, ...res.data }));
        localStorage.setItem('user', JSON.stringify({ ...user, ...res.data }));
      }
    } catch (e) {
      console.error('Failed to refresh profile:', e);
    }
  };

  const value = {
    user,
    token,
    isAuthenticated: !!token,
    loading,
    login,
    loginWithGoogle,
    register,
    logout,
    refreshProfile,
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
