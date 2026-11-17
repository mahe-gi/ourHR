package com.ourhr.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should set and retrieve current tenant ID on the same thread")
    void shouldSetAndGetTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        assertEquals(tenantId, TenantContext.getTenantId());

        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("Should isolate tenant IDs across distinct execution threads")
    void shouldIsolateTenantAcrossThreads() throws InterruptedException {
        UUID mainTenantId = UUID.randomUUID();
        UUID workerTenantId = UUID.randomUUID();

        TenantContext.setTenantId(mainTenantId);

        AtomicReference<UUID> workerObservedTenant = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            TenantContext.setTenantId(workerTenantId);
            workerObservedTenant.set(TenantContext.getTenantId());
            latch.countDown();
        });

        worker.start();
        latch.await();

        assertEquals(workerTenantId, workerObservedTenant.get());
        assertEquals(mainTenantId, TenantContext.getTenantId());
    }
}
