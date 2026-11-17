package com.ourhr.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final BatchJobRepository batchJobRepository;

    @Transactional
    public BatchJob createJob(UUID companyId, String jobType, UUID initiatedBy, int totalItems, String payload) {
        BatchJob job = BatchJob.builder()
                .jobType(jobType)
                .status("PENDING")
                .totalItems(totalItems)
                .processedItems(0)
                .failedItems(0)
                .jobPayload(payload)
                .initiatedBy(initiatedBy)
                .build();
        job.setCompanyId(companyId);
        return batchJobRepository.save(job);
    }

    @Transactional
    public void updateProgress(UUID jobId, int processedCount, int failedCount) {
        batchJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("IN_PROGRESS");
            job.setProcessedItems(processedCount);
            job.setFailedItems(failedCount);
            batchJobRepository.save(job);
        });
    }

    @Transactional
    public void completeJob(UUID jobId) {
        batchJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("COMPLETED");
            batchJobRepository.save(job);
        });
    }

    @Transactional
    public void failJob(UUID jobId, String errorSummary) {
        batchJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("FAILED");
            job.setErrorSummary(errorSummary);
            batchJobRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public BatchJob getJob(UUID jobId) {
        return batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Batch job not found with ID: " + jobId));
    }
}
