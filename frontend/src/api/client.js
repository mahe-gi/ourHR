import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('vetan_token');
    if (token && token !== 'undefined' && token !== 'null') {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => {
    if (response.data && response.data.success !== undefined) {
      if (response.data.success) {
        return response.data.data;
      }
      let msg = response.data.error?.message || 'Request failed';
      if (response.data.error?.details && Array.isArray(response.data.error.details)) {
        const issues = response.data.error.details
          .map((d) => (d.issue || d.message ? `${d.field}: ${d.issue || d.message}` : (typeof d === 'string' ? d : JSON.stringify(d))))
          .join('; ');
        if (issues) msg = `${msg} (${issues})`;
      }
      const err = new Error(msg);
      err.code = response.data.error?.code;
      err.details = response.data.error?.details;
      return Promise.reject(err);
    }
    return response.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('vetan_token');
      localStorage.removeItem('vetan_user');
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }
    let message = error.response?.data?.error?.message || error.message || 'Network error';
    if (error.response?.data?.error?.details && Array.isArray(error.response.data.error.details)) {
      const issues = error.response.data.error.details
        .map((d) => (d.issue || d.message ? `${d.field}: ${d.issue || d.message}` : (typeof d === 'string' ? d : JSON.stringify(d))))
        .join('; ');
      if (issues) message = `${message} (${issues})`;
    }
    const enhancedError = new Error(message);
    enhancedError.status = error.response?.status;
    enhancedError.code = error.response?.data?.error?.code;
    enhancedError.details = error.response?.data?.error?.details;
    return Promise.reject(enhancedError);
  }
);

export default apiClient;
