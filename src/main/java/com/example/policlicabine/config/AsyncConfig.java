package com.example.policlicabine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous task execution.
 * Provides separate thread pools for audit logging and alerting.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Thread pool executor for audit log operations.
     * Uses bounded queue to prevent memory issues under high load.
     */
    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum number of threads
        executor.setCorePoolSize(2);

        // Maximum pool size - scales up to this under load
        executor.setMaxPoolSize(5);

        // Queue capacity - how many tasks can wait before rejection
        executor.setQueueCapacity(500);

        // Thread naming pattern
        executor.setThreadNamePrefix("audit-log-");

        // Rejection policy - caller runs the task if queue is full (prevents data loss)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(60);

        // Allow core threads to timeout
        executor.setAllowCoreThreadTimeOut(true);

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Initialized audit log executor: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Thread pool executor for security alert operations.
     * Separate pool to isolate alerting from audit logging.
     */
    @Bean(name = "securityAlertExecutor")
    public Executor securityAlertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Smaller pool for alerting (less frequent than logging)
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("security-alert-");

        // Caller runs policy to ensure alerts are not lost
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Initialized security alert executor: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
