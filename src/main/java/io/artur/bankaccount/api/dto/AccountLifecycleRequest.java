package io.artur.bankaccount.api.dto;

public class AccountLifecycleRequest {
    private String reason;
    private String performedBy;
    
    public AccountLifecycleRequest() {}
    
    public AccountLifecycleRequest(String reason, String performedBy) {
        this.reason = reason;
        this.performedBy = performedBy;
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
}