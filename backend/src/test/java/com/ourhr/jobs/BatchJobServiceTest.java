package com.ourhr.jobs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchJobServiceTest {

    @Mock
    private BatchJobRepository batchJobRepository;

    @InjectMocks
    private BatchJobService batchJobService;

    private UUID companyId;
    private UUID userId;
    private UUID jobId;
    private BatchJob batchJob;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        batchJob = BatchJob.builder()
                .jobType("PAYSLIP_PDF_GENERATION")
                .status("PENDING")
                .totalItems(100)
                .processedItems(0)
                .failedItems(0)
                .initiatedBy(userId)
                .build();
        batchJob.setId(jobId);
        batchJob.setCompanyId(companyId);
    }

    @Test
    @DisplayName("Should create batch job in PENDING state")
    void shouldCreateBatchJob() {
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(batchJob);

        BatchJob created = batchJobService.createJob(companyId, "PAYSLIP_PDF_GENERATION", userId, 100, "{}");

        assertNotNull(created);
        assertEquals("PENDING", created.getStatus());
        assertEquals(100, created.getTotalItems());
        assertEquals(0, created.getProcessedItems());
        verify(batchJobRepository).save(any(BatchJob.class));
    }

    @Test
    @DisplayName("Should update progress and transition to IN_PROGRESS")
    void shouldUpdateProgress() {
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(batchJob);

        batchJobService.updateProgress(jobId, 45, 2);

        assertEquals("IN_PROGRESS", batchJob.getStatus());
        assertEquals(45, batchJob.getProcessedItems());
        assertEquals(2, batchJob.getFailedItems());
        verify(batchJobRepository).save(batchJob);
    }

    @Test
    @DisplayName("Should mark batch job as COMPLETED")
    void shouldCompleteJob() {
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(batchJob);

        batchJobService.completeJob(jobId);

        assertEquals("COMPLETED", batchJob.getStatus());
        verify(batchJobRepository).save(batchJob);
    }

    @Test
    @DisplayName("Should mark batch job as FAILED with error summary")
    void shouldFailJob() {
        when(batchJobRepository.findById(jobId)).thenReturn(Optional.of(batchJob));
        when(batchJobRepository.save(any(BatchJob.class))).thenReturn(batchJob);

        batchJobService.failJob(jobId, "S3 upload timeout");

        assertEquals("FAILED", batchJob.getStatus());
        assertEquals("S3 upload timeout", batchJob.getErrorSummary());
        verify(batchJobRepository).save(batchJob);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when job is not found")
    void shouldThrowWhenJobNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(batchJobRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> batchJobService.getJob(unknownId));
    }
}
