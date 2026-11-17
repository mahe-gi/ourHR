package com.ourhr.exporter.ca;

import com.ourhr.exporter.ca.dto.CaComplianceSummaryResponse;
import com.ourhr.exporter.ca.dto.CaPtReturnSummaryResponse;
import com.ourhr.exporter.ca.dto.CaTaxAuditSummaryResponse;
import com.ourhr.security.JwtAuthenticationFilter;
import com.ourhr.security.JwtService;
import com.ourhr.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaPortalController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CaPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CaPortalService caPortalService;

    private final UUID payrollRunId = UUID.randomUUID();

    @Test
    @DisplayName("ROLE_CA can access compliance summary endpoint")
    @WithMockUser(roles = "CA")
    void testGetComplianceSummaryWithCaRole() throws Exception {
        CaComplianceSummaryResponse response = CaComplianceSummaryResponse.builder()
                .payrollRunId(payrollRunId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalEmployeesCovered(15)
                .totalGrossWages(new BigDecimal("750000.00"))
                .totalNetDisbursed(new BigDecimal("680000.00"))
                .ecrFileStatus("GENERATED")
                .bankPayoutStatus("GENERATED")
                .build();

        when(caPortalService.getComplianceSummary(payrollRunId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ca/compliance-summary/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payrollRunId").value(payrollRunId.toString()))
                .andExpect(jsonPath("$.data.totalEmployeesCovered").value(15))
                .andExpect(jsonPath("$.data.totalGrossWages").value(750000.00))
                .andExpect(jsonPath("$.data.ecrFileStatus").value("GENERATED"));
    }

    @Test
    @DisplayName("ROLE_FOUNDER can access compliance summary endpoint")
    @WithMockUser(roles = "FOUNDER")
    void testGetComplianceSummaryWithFounderRole() throws Exception {
        CaComplianceSummaryResponse response = CaComplianceSummaryResponse.builder()
                .payrollRunId(payrollRunId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalEmployeesCovered(10)
                .build();

        when(caPortalService.getComplianceSummary(payrollRunId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ca/compliance-summary/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEmployeesCovered").value(10));
    }

    @Test
    @DisplayName("ROLE_FINANCE can access compliance summary endpoint")
    @WithMockUser(roles = "FINANCE")
    void testGetComplianceSummaryWithFinanceRole() throws Exception {
        CaComplianceSummaryResponse response = CaComplianceSummaryResponse.builder()
                .payrollRunId(payrollRunId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalEmployeesCovered(12)
                .build();

        when(caPortalService.getComplianceSummary(payrollRunId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ca/compliance-summary/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEmployeesCovered").value(12));
    }

    @Test
    @DisplayName("ROLE_HR is denied access (403 Forbidden) to CA compliance summary endpoint")
    @WithMockUser(roles = "HR")
    void testGetComplianceSummaryWithHrRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/compliance-summary/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE is denied access (403 Forbidden) to CA compliance summary endpoint")
    @WithMockUser(roles = "EMPLOYEE")
    void testGetComplianceSummaryWithEmployeeRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/compliance-summary/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_CA can access tax audit breakdown")
    @WithMockUser(roles = "CA")
    void testGetTaxAuditSummaryWithCaRole() throws Exception {
        CaTaxAuditSummaryResponse response = CaTaxAuditSummaryResponse.builder()
                .payrollRunId(payrollRunId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalTdsSection192(new BigDecimal("25000.00"))
                .totalTdsSection194J(new BigDecimal("15000.00"))
                .totalTdsSection194C(BigDecimal.ZERO)
                .totalTaxDeducted(new BigDecimal("40000.00"))
                .auditLineItems(List.of())
                .build();

        when(caPortalService.getTaxAuditSummary(payrollRunId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ca/tax-audit/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTdsSection192").value(25000.00))
                .andExpect(jsonPath("$.data.totalTdsSection194J").value(15000.00))
                .andExpect(jsonPath("$.data.totalTaxDeducted").value(40000.00));
    }

    @Test
    @DisplayName("ROLE_FINANCE and ROLE_FOUNDER can access tax audit breakdown")
    @WithMockUser(roles = "FINANCE")
    void testGetTaxAuditSummaryWithFinanceRole() throws Exception {
        CaTaxAuditSummaryResponse response = CaTaxAuditSummaryResponse.builder()
                .payrollRunId(payrollRunId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalTaxDeducted(new BigDecimal("20000.00"))
                .build();

        when(caPortalService.getTaxAuditSummary(payrollRunId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ca/tax-audit/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTaxDeducted").value(20000.00));
    }

    @Test
    @DisplayName("ROLE_HR and ROLE_EMPLOYEE are denied access (403 Forbidden) to tax audit breakdown")
    @WithMockUser(roles = "HR")
    void testGetTaxAuditSummaryWithHrRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/tax-audit/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE is denied access (403 Forbidden) to tax audit breakdown")
    @WithMockUser(roles = "EMPLOYEE")
    void testGetTaxAuditSummaryWithEmployeeRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/tax-audit/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_CA can access state-wise PT returns summary")
    @WithMockUser(roles = "CA")
    void testGetPtReturnSummaryWithCaRole() throws Exception {
        CaPtReturnSummaryResponse response = CaPtReturnSummaryResponse.builder()
                .payrollRunId(payrollRunId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalPtPayable(new BigDecimal("4000.00"))
                .stateBreakdowns(List.of(
                        CaPtReturnSummaryResponse.StatePtBreakdownDto.builder()
                                .stateCode("KA")
                                .employeeCount(20)
                                .totalSalary(new BigDecimal("1000000.00"))
                                .taxAmount(new BigDecimal("4000.00"))
                                .build()
                ))
                .build();

        when(caPortalService.getPtReturnSummary(payrollRunId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ca/pt-returns/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPtPayable").value(4000.00))
                .andExpect(jsonPath("$.data.stateBreakdowns[0].stateCode").value("KA"));
    }

    @Test
    @DisplayName("ROLE_FINANCE can access state-wise PT returns summary")
    @WithMockUser(roles = "FINANCE")
    void testGetPtReturnSummaryWithFinanceRole() throws Exception {
        CaPtReturnSummaryResponse response = CaPtReturnSummaryResponse.builder()
                .payrollRunId(payrollRunId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalPtPayable(new BigDecimal("1000.00"))
                .build();

        when(caPortalService.getPtReturnSummary(payrollRunId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ca/pt-returns/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("ROLE_HR and ROLE_EMPLOYEE are denied access (403 Forbidden) to PT returns summary")
    @WithMockUser(roles = "HR")
    void testGetPtReturnSummaryWithHrRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/pt-returns/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE is denied access (403 Forbidden) to PT returns summary")
    @WithMockUser(roles = "EMPLOYEE")
    void testGetPtReturnSummaryWithEmployeeRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/pt-returns/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Download ECR file returns text/plain byte stream with attachment header for ROLE_CA, ROLE_FOUNDER, ROLE_FINANCE")
    @WithMockUser(roles = "CA")
    void testDownloadEcrFile() throws Exception {
        byte[] mockBytes = "100987654321#~#Mahesh Kumar#~#15000.00#~#...".getBytes();
        when(caPortalService.downloadEcrFile(payrollRunId)).thenReturn(mockBytes);

        mockMvc.perform(get("/api/v1/ca/ecr/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ECR_" + payrollRunId + ".txt\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("Download ECR file allowed for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void testDownloadEcrFileWithFinanceRole() throws Exception {
        byte[] mockBytes = "100987654321#~#Mahesh Kumar#~#15000.00#~#...".getBytes();
        when(caPortalService.downloadEcrFile(payrollRunId)).thenReturn(mockBytes);

        mockMvc.perform(get("/api/v1/ca/ecr/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("Download ECR file forbidden for ROLE_HR and ROLE_EMPLOYEE")
    @WithMockUser(roles = "HR")
    void testDownloadEcrFileWithHrRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/ecr/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Download ECR file forbidden for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void testDownloadEcrFileWithEmployeeRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/ecr/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Download Bank payout file returns text/csv byte stream with attachment header")
    @WithMockUser(roles = "CA")
    void testDownloadBankFile() throws Exception {
        byte[] mockBytes = "Beneficiary Account No,Beneficiary Name,Payment Amount\n...".getBytes();
        when(caPortalService.downloadBankPayoutFile(eq(payrollRunId), eq("HDFC"))).thenReturn(mockBytes);

        mockMvc.perform(get("/api/v1/ca/bank-file/" + payrollRunId + "?bankFormat=HDFC"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"bank_payout_hdfc_" + payrollRunId + ".csv\""))
                .andExpect(content().contentTypeCompatibleWith(new MediaType("text", "csv")))
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("Download Bank payout file allowed for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void testDownloadBankFileWithFounderRole() throws Exception {
        byte[] mockBytes = "Beneficiary Account No,Beneficiary Name,Payment Amount\n...".getBytes();
        when(caPortalService.downloadBankPayoutFile(eq(payrollRunId), eq("HDFC"))).thenReturn(mockBytes);

        mockMvc.perform(get("/api/v1/ca/bank-file/" + payrollRunId + "?bankFormat=HDFC"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("Download Bank payout file forbidden for ROLE_HR and ROLE_EMPLOYEE")
    @WithMockUser(roles = "HR")
    void testDownloadBankFileWithHrRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/bank-file/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Download Bank payout file forbidden for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void testDownloadBankFileWithEmployeeRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ca/bank-file/" + payrollRunId))
                .andExpect(status().isForbidden());
    }
}
