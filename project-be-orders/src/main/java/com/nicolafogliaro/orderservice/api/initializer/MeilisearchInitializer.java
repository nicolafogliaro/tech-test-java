package com.nicolafogliaro.orderservice.api.initializer;

import com.nicolafogliaro.orderservice.api.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeilisearchInitializer {

    private final MeilisearchService meilisearchService;

    @EventListener(ApplicationReadyEvent.class)
    @Async("asyncTaskExecutor")
    public void initializeMeilisearchAfterStartup() {
        log.info(">>> Starting Meilisearch initialization...");
        try {
            meilisearchService.initializeIndexes();
            log.info("<<< Meilisearch initialization completed successfully.");
        } catch (Exception e) {
            log.error("*** Failed to initialize Meilisearch: " + e.getMessage(), e);
        }
    }

}
