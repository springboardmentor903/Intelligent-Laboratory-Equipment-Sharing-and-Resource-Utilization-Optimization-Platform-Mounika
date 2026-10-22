import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: automatically attaches JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handles error status codes
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response) {
      // If 401 Unauthorized, token might be invalid/expired
      if (error.response.status === 401) {
        const currentPath = window.location.pathname;
        if (currentPath !== '/login' && currentPath !== '/register') {
          // localStorage.removeItem('token');
          // localStorage.removeItem('user');
        }
      }
      const message = error.response.data?.message || 'An unexpected error occurred.';
      return Promise.reject(new Error(message));
    }
    return Promise.reject(new Error('Network error. Backend server may be offline.'));
  }
);

export default api;
