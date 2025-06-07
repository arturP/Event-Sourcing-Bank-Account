package io.artur.bankaccount.domain.shared.events;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class EventMetadata {
    
    private String correlationId;
    private String causationId;
    private String userId;
    private String userAgent;
    private String ipAddress;
    private LocalDateTime timestamp;
    private int version;
    private Map<String, String> additionalProperties;
    
    public EventMetadata() {
    }
    
    public EventMetadata(String correlationId, String causationId, String userId, 
                        String userAgent, String ipAddress, int version, 
                        Map<String, String> additionalProperties) {
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.causationId = causationId;
        this.userId = userId;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
        this.version = version;
        this.additionalProperties = additionalProperties != null ? additionalProperties : Map.of();
    }
    
    public EventMetadata(int version) {
        this(null, null, null, null, null, version, null);
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public String getCausationId() {
        return causationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public int getVersion() {
        return version;
    }
    
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}