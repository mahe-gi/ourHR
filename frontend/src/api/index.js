import apiClient from './client';

export const authApi = {
  registerCompany: (payload) => apiClient.post('/auth/register-company', payload),
  login: (credentials) => apiClient.post('/auth/login', credentials),
  getMe: () => apiClient.get('/auth/me'),
};

export const companyApi = {
  getCurrentCompany: () => apiClient.get('/companies/current'),
  getBranches: () => apiClient.get('/branches'),
  createBranch: (payload) => apiClient.post('/branches', payload),
};

export const employeeApi = {
  getEmployees: (params = {}) => apiClient.get('/employees', { params }),
  getEmployee: (id) => apiClient.get(`/employees/${id}`),
  createEmployee: (payload) => apiClient.post('/employees', payload),
  updateSalaryStructure: (employeeId, payload) =>
    apiClient.post(`/employees/${employeeId}/salary-structure`, payload),
};

export const attendanceApi = {
  punch: (payload) => apiClient.post('/attendance/punch', payload),
  getDailyPunches: (date) => apiClient.get('/attendance/daily', { params: { date } }),
  freezeAttendance: (payload) => apiClient.post('/attendance/freeze', payload),
};

export const leaveApi = {
  getLeaveTypes: () => apiClient.get('/leaves/types'),
  getLeaveBalances: (employeeId) => apiClient.get(`/leaves/balances/${employeeId}`),
  applyLeave: (payload) => apiClient.post('/leaves/apply', payload),
  getLeaveRequests: () => apiClient.get('/leaves/requests'),
};

export const payrollApi = {
  getPayrollRuns: () => apiClient.get('/payroll'),
  getPayrollRun: (id) => apiClient.get(`/payroll/${id}`),
  initiatePayroll: (payload) => apiClient.post('/payroll/initiate', payload),
  calculatePayroll: (id) => apiClient.post(`/payroll/${id}/calculate`),
  disbursePayroll: (id) => apiClient.post(`/payroll/${id}/disburse`),
  getConsultants: () => apiClient.get('/payroll/consultants'),
  calculatePreview: (payload) => apiClient.post('/calculate', payload),
};

export const caApi = {
  getComplianceSummary: (yearMonth) =>
    apiClient.get('/ca/compliance-summary', { params: { yearMonth } }),
  getTaxAudit: (yearMonth) =>
    apiClient.get('/ca/tax-audit', { params: { yearMonth } }),
};

export const exporterApi = {
  getEcrExport: (runId) => apiClient.get(`/exporters/ecr/${runId}`),
  getBankExport: (runId, bankFormat = 'HDFC') =>
    apiClient.get(`/exporters/bank/${runId}`, { params: { bankFormat } }),
};
