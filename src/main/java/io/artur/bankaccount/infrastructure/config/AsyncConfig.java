package io.artur.bankaccount.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "eventProcessingExecutor")
    public Executor eventProcessingExecutor() {
        // Use cached thread pool for event processing
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EventProcessor");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Bean(name = "projectionExecutor")
    public Executor projectionExecutor() {
        // Use cached thread pool for projection updates
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Projection");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Bean(name = "dbOperationExecutor")
    public Executor dbOperationExecutor() {
        // Use cached thread pool for database operations
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "DbOperation");
            t.setDaemon(true);
            return t;
        });
    }
}