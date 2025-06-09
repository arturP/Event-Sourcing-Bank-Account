package io.artur.bankaccount.api.dto;

import jakarta.validation.constraints.*;

public class AccountLifecycleRequest {
    
    @NotBlank(message = "Reason is required for account lifecycle operations")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
    
    @NotBlank(message = "Performed by field is required")
    @Size(min = 2, max = 100, message = "Performed by field must be between 2 and 100 characters")
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