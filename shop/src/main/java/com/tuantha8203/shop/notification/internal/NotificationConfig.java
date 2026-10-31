package com.tuantha8203.shop.notification.internal;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
class NotificationConfig {

    static final String EXECUTOR = "notificationExecutor";

    /**
     * A dedicated, BOUNDED pool: a slow mail server can only ever tie up these few threads,
     * never the threads serving customers.
     */
    @Bean(name = EXECUTOR)
    ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notify-");
        // Queue full: the calling thread sends the mail itself. Slower for that one request,
        // but no email is silently dropped (see ADR).
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // On shutdown, finish the mails already queued instead of losing them
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
