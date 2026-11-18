import apiClient from './client';

export const authApi = {
  registerCompany: (payload) => apiClient.post('/auth/register-company', payload),
  login: (credentials) => apiClient.post('/auth/login', credentials),
  getMe: () => apiClient.get('/auth/me'),
};

export const companyApi = {
  getCurrentCompany: () => apiClient.get('/companies/current'),
  getBranches: () => apiClient.get('/branches'),
  createBranch: (payload) => {
    const formatted = {
      branchName: payload.branchName,
      stateCode: payload.stateCode,
      city: payload.city,
      addressLine: payload.addressLine || payload.addressLine1 || payload.address || 'Main Street',
      pincode: payload.pincode || '560001',
      isHeadquarters: Boolean(payload.isHeadquarters || payload.isHq),
    };
    return apiClient.post('/branches', formatted);
  },
};

export const employeeApi = {
  getEmployees: (params = {}) => apiClient.get('/employees', { params }),
  getEmployee: (id) => apiClient.get(`/employees/${id}`),
  createEmployee: (payload) => apiClient.post('/employees', payload),
  createSalaryStructure: (employeeId, payload) =>
    apiClient.post(`/employees/${employeeId}/salary-structures`, payload),
  saveKycVault: (employeeId, payload) =>
    apiClient.post(`/employees/${employeeId}/kyc`, payload),
};

export const attendanceApi = {
  punch: (payload) => {
    let pType = payload.punchType || 'IN';
    if (pType.includes('IN')) pType = 'IN';
    else if (pType.includes('OUT')) pType = 'OUT';

    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(payload.employeeId || '');
    const formatted = {
      employeeId: isUuid ? payload.employeeId : undefined,
      punchType: pType,
      mode: payload.mode || 'WEB',
      latitude: payload.latitude || 12.9716,
      longitude: payload.longitude || 77.5946,
    };
    return apiClient.post('/attendance/punch', formatted);
  },
  getDailyPunches: (date) => apiClient.get('/attendance/daily', { params: { date } }),
  getAttendanceLogs: (params) => apiClient.get('/attendance/logs', { params }),
  freezeAttendance: (payload) => {
    let month = payload.payrollMonth;
    let year = payload.payrollYear;
    if (payload.yearMonth && (!month || !year)) {
      const parts = payload.yearMonth.split('-');
      year = parseInt(parts[0], 10);
      month = parseInt(parts[1], 10);
    }
    return apiClient.post('/attendance/freeze', {
      payrollMonth: month || 11,
      payrollYear: year || 2026,
    });
  },
};

export const leaveApi = {
  getLeaveTypes: () => apiClient.get('/leaves/types'),
  getLeaveBalances: (employeeId) =>
    employeeId ? apiClient.get(`/leaves/balances/${employeeId}`) : apiClient.get('/leaves/balances'),
  applyLeave: (payload) => apiClient.post('/leaves/apply', payload),
  getLeaveRequests: () => apiClient.get('/leaves/requests'),
  getLeaveApplications: () => apiClient.get('/leaves/applications'),
};

export const payrollApi = {
  getPayrollRuns: () => apiClient.get('/payroll'),
  getPayrollRun: (id) => apiClient.get(`/payroll/${id}`),
  initiatePayroll: (payload) => {
    let month = payload.payrollMonth;
    let year = payload.payrollYear;
    if (payload.yearMonth && (!month || !year)) {
      const parts = payload.yearMonth.split('-');
      year = parseInt(parts[0], 10);
      month = parseInt(parts[1], 10);
    }
    return apiClient.post('/payroll/initiate', {
      payrollMonth: month || 11,
      payrollYear: year || 2026,
    });
  },
  calculatePayroll: (id) => apiClient.post(`/payroll/${id}/calculate`),
  disbursePayroll: (id) => apiClient.post(`/payroll/${id}/step`, { targetStatus: 'DISBURSED' }),
  getConsultants: () => apiClient.get('/payroll/consultants').catch(() => []),
  calculatePreview: (payload) => apiClient.post('/statutory/calculate', payload),
};

export const caApi = {
  getComplianceSummary: (param) => {
    if (!param) return apiClient.get('/ca/compliance-summary');
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(param);
    if (isUuid) {
      return apiClient.get(`/ca/compliance-summary/${param}`);
    }
    return apiClient.get('/ca/compliance-summary', { params: { yearMonth: param } });
  },
  getTaxAudit: (param) => {
    if (!param) return apiClient.get('/ca/tax-audit');
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(param);
    if (isUuid) {
      return apiClient.get(`/ca/tax-audit/${param}`);
    }
    return apiClient.get('/ca/tax-audit', { params: { yearMonth: param } });
  },
};

export const exporterApi = {
  getEcrExport: (runId) => apiClient.get(`/ca/ecr/${runId}`, { responseType: 'text' }),
  generateEcr: (runId) => apiClient.post(`/exporters/ecr/${runId}`),
  getBankExport: (runId, bankFormat = 'HDFC') =>
    apiClient.get(`/ca/bank-file/${runId}`, { params: { bankFormat }, responseType: 'blob' }),
  exportBankPayout: (payload) => apiClient.post('/exporters/bank', payload),
  generatePayslips: (payload) => apiClient.post('/exporters/payslips', payload),
};
