package io.artur.eventsourcing.config;

import io.artur.eventsourcing.cache.CachedReadModelService;
import io.artur.eventsourcing.config.DatabaseConfig;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.JdbcEventStore;
import io.artur.eventsourcing.metrics.PerformanceMetricsCollector;
import io.artur.eventsourcing.repository.EventSourcingBankAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.UUID;

@Configuration
public class ApplicationConfig {
    
    @Bean
    public DatabaseConfig databaseConfig() {
        return new DatabaseConfig();
    }
    
    @Bean
    public DataSource dataSource(DatabaseConfig databaseConfig) {
        return databaseConfig.getDataSource();
    }
    
    @Bean
    public EventStore<AccountEvent, UUID> eventStore(DataSource dataSource) {
        return new JdbcEventStore(dataSource);
    }
    
    @Bean
    public PerformanceMetricsCollector performanceMetricsCollector() {
        return new PerformanceMetricsCollector();
    }
    
    @Bean
    public CachedReadModelService.CacheConfiguration cacheConfiguration() {
        return CachedReadModelService.CacheConfiguration.defaultConfig();
    }
    
    @Bean
    public EventSourcingBankAccountRepository bankAccountRepository(
            EventStore<AccountEvent, UUID> eventStore,
            CachedReadModelService.CacheConfiguration cacheConfig) {
        return new EventSourcingBankAccountRepository(eventStore, cacheConfig);
    }
}