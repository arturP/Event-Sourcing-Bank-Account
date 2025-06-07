package io.artur.bankaccount.infrastructure.config;

import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.application.ports.outgoing.CachePort;
import io.artur.bankaccount.application.ports.outgoing.EventStorePort;
import io.artur.bankaccount.application.ports.outgoing.MetricsPort;
import io.artur.bankaccount.application.services.AccountApplicationService;
import io.artur.bankaccount.infrastructure.monitoring.NativeMetricsCollector;
import io.artur.bankaccount.infrastructure.persistence.cache.NativeCacheService;
import io.artur.bankaccount.infrastructure.persistence.eventstore.NativeEventStore;
import io.artur.bankaccount.infrastructure.persistence.eventstore.serialization.EventSerializer;
import io.artur.bankaccount.infrastructure.persistence.repositories.NativeAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Configuration for native infrastructure components that implement ports directly
 * without depending on legacy infrastructure
 */
@Configuration
@ConditionalOnProperty(
    name = "bankaccount.infrastructure.native.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class NativeInfrastructureConfig {
    
    /**
     * DataSource configuration for native infrastructure
     */
    @Bean
    public DataSource dataSource(Environment env) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(env.getProperty("db.url", "jdbc:h2:mem:bankaccount;DB_CLOSE_DELAY=-1"));
        dataSource.setUsername(env.getProperty("db.username", "sa"));
        dataSource.setPassword(env.getProperty("db.password", ""));
        dataSource.setDriverClassName(env.getProperty("db.driver", "org.h2.Driver"));
        return dataSource;
    }
    
    /**
     * Database schema initializer
     */
    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db-schema.sql"));
        initializer.setDatabasePopulator(populator);
        
        return initializer;
    }
    
    /**
     * Native event serializer for domain events
     */
    @Bean
    public EventSerializer eventSerializer() {
        return new EventSerializer();
    }
    
    /**
     * Native event store that implements EventStorePort directly
     */
    @Bean
    @Primary
    public EventStorePort nativeEventStore(DataSource dataSource, EventSerializer eventSerializer) {
        return new NativeEventStore(dataSource, eventSerializer);
    }
    
    /**
     * Native cache service that implements CachePort directly
     */
    @Bean
    @Primary
    public CachePort nativeCacheService() {
        return new NativeCacheService();
    }
    
    /**
     * Native metrics collector that implements MetricsPort directly
     */
    @Bean
    @Primary
    public MetricsPort nativeMetricsCollector() {
        return new NativeMetricsCollector();
    }
    
    /**
     * Native account repository that uses native event store
     */
    @Bean
    @Primary
    public AccountRepository nativeAccountRepository(EventStorePort eventStore) {
        return new NativeAccountRepository(eventStore);
    }
    
    /**
     * Enhanced application service that uses native infrastructure components
     */
    @Bean
    @Primary
    public AccountApplicationService nativeAccountApplicationService(
            AccountRepository accountRepository,
            CachePort cachePort,
            MetricsPort metricsPort) {
        
        return new AccountApplicationService(accountRepository, cachePort, metricsPort);
    }
    
    /**
     * Configuration properties for native infrastructure behavior
     */
    @Bean
    public NativeInfrastructureProperties nativeInfrastructureProperties() {
        return new NativeInfrastructureProperties();
    }
    
    /**
     * Properties for configuring native infrastructure behavior
     */
    public static class NativeInfrastructureProperties {
        private boolean enableCaching = true;
        private boolean enableMetrics = true;
        private boolean enableEventStoreOptimizations = true;
        private long cacheExpirationMinutes = 30;
        private int maxCacheSize = 1000;
        private int eventBatchSize = 100;
        private boolean enablePeriodicMetricsReporting = true;
        private int metricsReportingIntervalSeconds = 30;
        
        // Getters and setters
        public boolean isEnableCaching() { 
            return enableCaching; 
        }
        
        public void setEnableCaching(boolean enableCaching) { 
            this.enableCaching = enableCaching; 
        }
        
        public boolean isEnableMetrics() { 
            return enableMetrics; 
        }
        
        public void setEnableMetrics(boolean enableMetrics) { 
            this.enableMetrics = enableMetrics; 
        }
        
        public boolean isEnableEventStoreOptimizations() { 
            return enableEventStoreOptimizations; 
        }
        
        public void setEnableEventStoreOptimizations(boolean enableEventStoreOptimizations) { 
            this.enableEventStoreOptimizations = enableEventStoreOptimizations; 
        }
        
        public long getCacheExpirationMinutes() { 
            return cacheExpirationMinutes; 
        }
        
        public void setCacheExpirationMinutes(long cacheExpirationMinutes) { 
            this.cacheExpirationMinutes = cacheExpirationMinutes; 
        }
        
        public int getMaxCacheSize() { 
            return maxCacheSize; 
        }
        
        public void setMaxCacheSize(int maxCacheSize) { 
            this.maxCacheSize = maxCacheSize; 
        }
        
        public int getEventBatchSize() { 
            return eventBatchSize; 
        }
        
        public void setEventBatchSize(int eventBatchSize) { 
            this.eventBatchSize = eventBatchSize; 
        }
        
        public boolean isEnablePeriodicMetricsReporting() { 
            return enablePeriodicMetricsReporting; 
        }
        
        public void setEnablePeriodicMetricsReporting(boolean enablePeriodicMetricsReporting) { 
            this.enablePeriodicMetricsReporting = enablePeriodicMetricsReporting; 
        }
        
        public int getMetricsReportingIntervalSeconds() { 
            return metricsReportingIntervalSeconds; 
        }
        
        public void setMetricsReportingIntervalSeconds(int metricsReportingIntervalSeconds) { 
            this.metricsReportingIntervalSeconds = metricsReportingIntervalSeconds; 
        }
    }
}