package io.artur.bankaccount.infrastructure.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod", "docker"})
public class HealthConfig {

    @Bean
    public HealthIndicator bankAccountServiceHealthIndicator() {
        return () -> {
            try {
                // Add your custom health checks here
                // For example: check database connectivity, external services, etc.
                
                // Simulate health check
                boolean isHealthy = checkApplicationHealth();
                
                if (isHealthy) {
                    return Health.up()
                        .withDetail("service", "Bank Account Service")
                        .withDetail("status", "Operational")
                        .withDetail("version", "1.0-SNAPSHOT")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("service", "Bank Account Service")
                        .withDetail("status", "Degraded")
                        .withDetail("error", "Service unavailable")
                        .build();
                }
            } catch (Exception e) {
                return Health.down()
                    .withDetail("service", "Bank Account Service")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
    
    private boolean checkApplicationHealth() {
        // Implement your health check logic here
        // This could include:
        // - Database connectivity check
        // - External service availability
        // - Memory/CPU usage checks
        // - Business logic validation
        
        return true; // Simplified for demo
    }
}