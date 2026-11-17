package com.ourhr.exporter.storage;

import com.ourhr.exporter.storage.dto.FileUploadResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/v1/storage/upload returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldAllowUploadForHr() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pan.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4 sample content".getBytes()
        );

        when(fileStorageService.uploadFile(eq("kyc_pan"), eq("pan.pdf"), any(), eq(MediaType.APPLICATION_PDF_VALUE)))
                .thenReturn("tenant123/kyc_pan/pan_uuid.pdf");
        when(fileStorageService.generatePresignedUrl(eq("tenant123/kyc_pan/pan_uuid.pdf"), anyLong()))
                .thenReturn("/api/v1/storage/download?fileKey=tenant123%2Fkyc_pan%2Fpan_uuid.pdf");

        mockMvc.perform(multipart("/api/v1/storage/upload")
                        .file(file)
                        .param("docCategory", "KYC_PAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileKey").value("tenant123/kyc_pan/pan_uuid.pdf"))
                .andExpect(jsonPath("$.data.contentType").value(MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    @DisplayName("POST /api/v1/storage/upload returns 200 for ROLE_EMPLOYEE (uploading own document)")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldAllowUploadForEmployee() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cheque.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy cheque content".getBytes()
        );

        when(fileStorageService.uploadFile(any(), any(), any(), any()))
                .thenReturn("tenant/general/cheque.pdf");
        when(fileStorageService.generatePresignedUrl(any(), anyLong()))
                .thenReturn("/api/v1/storage/download?fileKey=sample");

        mockMvc.perform(multipart("/api/v1/storage/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/storage/upload returns 403 for ROLE_FINANCE and ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldDenyUploadForCa() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/storage/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/storage/upload returns 403 when unauthenticated")
    void shouldDenyUploadWhenUnauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/storage/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/storage/download returns 200 with raw bytes for ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldAllowDownloadForCa() throws Exception {
        String fileKey = "tenant123/kyc/form12b.pdf";
        byte[] content = "%PDF-1.4 form 12b".getBytes();

        when(fileStorageService.downloadFile(eq(fileKey))).thenReturn(content);

        mockMvc.perform(get("/api/v1/storage/download")
                        .param("fileKey", fileKey))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(content))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"form12b.pdf\""));
    }

    @Test
    @DisplayName("GET /api/v1/storage/download returns 200 with inline Content-Disposition when requested")
    @WithMockUser(roles = "HR")
    void shouldAllowInlineDownloadForHr() throws Exception {
        String fileKey = "tenant123/kyc/pan.jpg";
        byte[] imageBytes = new byte[]{1, 2, 3, 4};

        when(fileStorageService.downloadFile(eq(fileKey))).thenReturn(imageBytes);

        mockMvc.perform(get("/api/v1/storage/download")
                        .param("fileKey", fileKey)
                        .param("inline", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"pan.jpg\""));
    }

    @Test
    @DisplayName("GET /api/v1/storage/download returns 403 when unauthenticated")
    void shouldDenyDownloadWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/storage/download")
                        .param("fileKey", "tenant123/doc.pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/storage returns 200 for ROLE_FOUNDER and ROLE_HR")
    @WithMockUser(roles = "FOUNDER")
    void shouldAllowDeleteForFounder() throws Exception {
        String fileKey = "tenant123/kyc/temp.pdf";

        mockMvc.perform(delete("/api/v1/storage")
                        .param("fileKey", fileKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(fileStorageService).deleteFile(eq(fileKey));
    }

    @Test
    @DisplayName("DELETE /api/v1/storage returns 403 for ROLE_EMPLOYEE, ROLE_FINANCE, and ROLE_CA")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldDenyDeleteForEmployee() throws Exception {
        mockMvc.perform(delete("/api/v1/storage")
                        .param("fileKey", "tenant123/doc.pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/storage returns 403 when unauthenticated")
    void shouldDenyDeleteWhenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/storage")
                        .param("fileKey", "tenant123/doc.pdf"))
                .andExpect(status().isForbidden());
    }
}
