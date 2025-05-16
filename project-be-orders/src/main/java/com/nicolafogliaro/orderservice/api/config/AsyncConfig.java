package com.nicolafogliaro.orderservice.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
public class AsyncConfig {

    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Initial number of threads
        executor.setMaxPoolSize(10); // Maximum number of threads
        executor.setQueueCapacity(25); // Capacity of the queue for pending tasks
        executor.setThreadNamePrefix("MyAsyncThread-"); // Prefix for thread names for easier debugging

        // Optional: Define how to handle tasks when the queue is full and max threads are active
        // executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setRejectedExecutionHandler((runnable, executingExecutor) -> {
            // Log the rejection
            log.error("Async Task Rejected: " + runnable.toString() + " from " + executingExecutor.toString());
        });

        executor.initialize();

        return executor;
    }
}