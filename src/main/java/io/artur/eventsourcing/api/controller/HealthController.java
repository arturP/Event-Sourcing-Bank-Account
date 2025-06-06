package io.artur.eventsourcing.api.controller;

import io.artur.eventsourcing.metrics.PerformanceMetricsCollector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "System health check endpoints")
public class HealthController {
    
    private final PerformanceMetricsCollector metricsCollector;
    
    public HealthController(PerformanceMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    @GetMapping
    @Operation(summary = "Health check", description = "Check if the application is running")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Event Sourcing Bank Account API");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "Performance metrics", description = "Get current performance metrics")
    public ResponseEntity<PerformanceMetricsCollector.PerformanceSummary> metrics() {
        return ResponseEntity.ok(metricsCollector.getPerformanceSummary());
    }
}