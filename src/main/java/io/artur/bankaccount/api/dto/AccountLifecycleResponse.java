package io.artur.bankaccount.api.dto;

import java.time.Instant;
import java.util.UUID;

public class AccountLifecycleResponse {
    private String status;
    private String message;
    private UUID accountId;
    private String newStatus;
    private String reason;
    private String performedBy;
    private Instant timestamp;
    
    public AccountLifecycleResponse() {}
    
    public AccountLifecycleResponse(String status, String message, UUID accountId, String newStatus, 
                                   String reason, String performedBy, Instant timestamp) {
        this.status = status;
        this.message = message;
        this.accountId = accountId;
        this.newStatus = newStatus;
        this.reason = reason;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
    }
    
    public static AccountLifecycleResponse success(UUID accountId, String newStatus, String reason, String performedBy) {
        return new AccountLifecycleResponse(
            "SUCCESS",
            String.format("Account %s successfully changed to %s", accountId, newStatus),
            accountId,
            newStatus,
            reason,
            performedBy,
            Instant.now()
        );
    }
    
    public static AccountLifecycleResponse failure(UUID accountId, String errorMessage) {
        return new AccountLifecycleResponse(
            "FAILED",
            errorMessage,
            accountId,
            null,
            null,
            null,
            Instant.now()
        );
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getPerformedBy() {
        return performedBy;
    }
    
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}