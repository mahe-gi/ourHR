package com.ourhr.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.exporter.bank.BankExporterService;
import com.ourhr.exporter.dto.BankExportRequest;
import com.ourhr.exporter.dto.BankExportResponse;
import com.ourhr.exporter.dto.EcrExportResponse;
import com.ourhr.exporter.dto.PayslipGenerationRequest;
import com.ourhr.exporter.dto.PayslipGenerationResponse;
import com.ourhr.exporter.ecr.EcrExporterService;
import com.ourhr.exporter.pdf.PdfPayslipService;
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
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExporterController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ExporterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private EcrExporterService ecrExporterService;

    @MockBean
    private BankExporterService bankExporterService;

    @MockBean
    private PdfPayslipService pdfPayslipService;

    private final UUID payrollRunId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @Test
    @DisplayName("ROLE_HR can trigger EPFO ECR generation")
    @WithMockUser(roles = "HR")
    void testGenerateEcrEndpoint() throws Exception {
        EcrExportResponse response = EcrExportResponse.builder()
                .payrollRunId(payrollRunId)
                .establishmentCode("BGM00123456")
                .payrollMonth(11)
                .payrollYear(2026)
                .fileName("ECR_BGM00123456_11_2026.txt")
                .fileDownloadUrl("https://storage/ecr.txt")
                .memberCount(5)
                .totalGrossWages(new BigDecimal("150000.00"))
                .totalEpfWages(new BigDecimal("75000.00"))
                .totalEeShare(new BigDecimal("9000.00"))
                .totalEpsShare(new BigDecimal("6250.00"))
                .totalErShare(new BigDecimal("2750.00"))
                .generatedAt(Instant.now())
                .build();

        when(ecrExporterService.generateEcrFile(payrollRunId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/exporters/ecr/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.establishmentCode").value("BGM00123456"))
                .andExpect(jsonPath("$.data.memberCount").value(5))
                .andExpect(jsonPath("$.data.fileDownloadUrl").value("https://storage/ecr.txt"));
    }

    @Test
    @DisplayName("ROLE_FINANCE can trigger bank payout export")
    @WithMockUser(roles = "FINANCE")
    void testExportBankPayoutEndpoint() throws Exception {
        BankExportRequest request = BankExportRequest.builder()
                .payrollRunId(payrollRunId)
                .bankFormat("HDFC")
                .build();

        BankExportResponse response = BankExportResponse.builder()
                .payrollRunId(payrollRunId)
                .bankFormat("HDFC")
                .fileName("bank_payout_hdfc_11_2026.csv")
                .fileDownloadUrl("https://storage/bank.csv")
                .totalRecords(10)
                .totalAmount(new BigDecimal("450000.00"))
                .generatedAt(Instant.now())
                .build();

        when(bankExporterService.exportBankPayout(any(BankExportRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/exporters/bank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bankFormat").value("HDFC"))
                .andExpect(jsonPath("$.data.totalRecords").value(10))
                .andExpect(jsonPath("$.data.totalAmount").value(450000.00));
    }

    @Test
    @DisplayName("ROLE_HR can trigger async batch PDF payslips generation")
    @WithMockUser(roles = "HR")
    void testGeneratePayslipsEndpoint() throws Exception {
        PayslipGenerationRequest request = PayslipGenerationRequest.builder()
                .payrollRunId(payrollRunId)
                .build();

        UUID jobId = UUID.randomUUID();
        PayslipGenerationResponse response = PayslipGenerationResponse.builder()
                .batchJobId(jobId)
                .payrollRunId(payrollRunId)
                .status("PENDING")
                .totalPayslipsScheduled(25)
                .scheduledAt(Instant.now())
                .build();

        when(pdfPayslipService.generatePayslipsAsync(payrollRunId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/exporters/payslips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchJobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalPayslipsScheduled").value(25));
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE can download their own single payslip stream")
    @WithMockUser(roles = "EMPLOYEE")
    void testDownloadPayslipEndpoint() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 mock payslip content %%EOF".getBytes();
        when(pdfPayslipService.generatePayslipPdf(payrollRunId, employeeId)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/exporters/payslips/" + payrollRunId + "/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"payslip_" + payrollRunId + "_" + employeeId + ".pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE is forbidden from triggering ECR generation")
    @WithMockUser(roles = "EMPLOYEE")
    void testGenerateEcrEndpointForbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/exporters/ecr/" + payrollRunId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE is forbidden from triggering bank payout export")
    @WithMockUser(roles = "EMPLOYEE")
    void testExportBankPayoutForbiddenForEmployee() throws Exception {
        BankExportRequest request = BankExportRequest.builder()
                .payrollRunId(payrollRunId)
                .bankFormat("HDFC")
                .build();

        mockMvc.perform(post("/api/v1/exporters/bank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_EMPLOYEE is forbidden from triggering batch payslips generation")
    @WithMockUser(roles = "EMPLOYEE")
    void testGeneratePayslipsForbiddenForEmployee() throws Exception {
        PayslipGenerationRequest request = PayslipGenerationRequest.builder()
                .payrollRunId(payrollRunId)
                .build();

        mockMvc.perform(post("/api/v1/exporters/payslips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_FOUNDER and ROLE_CA can trigger ECR generation")
    @WithMockUser(roles = "FOUNDER")
    void testGenerateEcrEndpointWithFounderRole() throws Exception {
        EcrExportResponse response = EcrExportResponse.builder()
                .payrollRunId(payrollRunId)
                .establishmentCode("BGM00123456")
                .payrollMonth(11)
                .payrollYear(2026)
                .fileName("ECR_BGM00123456_11_2026.txt")
                .fileDownloadUrl("https://storage/ecr.txt")
                .memberCount(1)
                .totalGrossWages(new BigDecimal("50000.00"))
                .totalEpfWages(new BigDecimal("15000.00"))
                .totalEeShare(new BigDecimal("1800.00"))
                .totalEpsShare(new BigDecimal("1250.00"))
                .totalErShare(new BigDecimal("550.00"))
                .generatedAt(Instant.now())
                .build();

        when(ecrExporterService.generateEcrFile(payrollRunId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/exporters/ecr/" + payrollRunId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
