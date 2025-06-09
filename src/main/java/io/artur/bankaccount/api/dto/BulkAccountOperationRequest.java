package io.artur.bankaccount.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public class BulkAccountOperationRequest {
    
    @NotNull(message = "Account IDs list is required")
    @Size(min = 1, max = 50, message = "Bulk operations can handle between 1 and 50 accounts")
    private List<UUID> accountIds;
    
    @NotBlank(message = "Operation type is required")
    @Pattern(regexp = "FREEZE|UNFREEZE|SUSPEND|CLOSE|REACTIVATE|MARK_DORMANT", message = "Invalid operation type")
    private String operationType;
    
    @NotBlank(message = "Reason is required for bulk operations")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
    
    @NotBlank(message = "Performed by field is required")
    @Size(min = 2, max = 100, message = "Performed by field must be between 2 and 100 characters")
    private String performedBy;
    
    @Pattern(regexp = "STRICT|BEST_EFFORT", message = "Execution mode must be STRICT or BEST_EFFORT")
    private String executionMode = "BEST_EFFORT";
    
    public BulkAccountOperationRequest() {}
    
    public BulkAccountOperationRequest(List<UUID> accountIds, String operationType, String reason, String performedBy) {
        this.accountIds = accountIds;
        this.operationType = operationType;
        this.reason = reason;
        this.performedBy = performedBy;
    }
    
    public List<UUID> getAccountIds() {
        return accountIds;
    }
    
    public void setAccountIds(List<UUID> accountIds) {
        this.accountIds = accountIds;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
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
    
    public String getExecutionMode() {
        return executionMode;
    }
    
    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }
}