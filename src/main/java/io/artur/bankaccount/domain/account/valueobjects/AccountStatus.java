package io.artur.bankaccount.domain.account.valueobjects;

import java.time.Instant;
import java.util.Objects;

public class AccountStatus {
    
    public enum Status {
        ACTIVE("Account is active and operational"),
        FROZEN("Account is temporarily frozen, transactions blocked"),
        CLOSED("Account is permanently closed"),
        DORMANT("Account is inactive due to no activity");
        
        private final String description;
        
        Status(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean canPerformTransactions() {
            return this == ACTIVE;
        }
        
        public boolean canBeFrozen() {
            return this == ACTIVE || this == DORMANT;
        }
        
        public boolean canBeReactivated() {
            return this == FROZEN || this == DORMANT;
        }
        
        public boolean canBeClosed() {
            return this == ACTIVE || this == FROZEN || this == DORMANT;
        }
    }
    
    private final Status status;
    private final String reason;
    private final Instant lastStatusChange;
    private final String changedBy;
    
    public AccountStatus(Status status, String reason, Instant lastStatusChange, String changedBy) {
        if (status == null) {
            throw new IllegalArgumentException("Account status cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Status change reason cannot be null or empty");
        }
        if (lastStatusChange == null) {
            throw new IllegalArgumentException("Last status change timestamp cannot be null");
        }
        if (changedBy == null || changedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Changed by cannot be null or empty");
        }
        
        this.status = status;
        this.reason = reason.trim();
        this.lastStatusChange = lastStatusChange;
        this.changedBy = changedBy.trim();
    }
    
    public static AccountStatus createActive(String changedBy) {
        return new AccountStatus(
            Status.ACTIVE, 
            "Account opened", 
            Instant.now(), 
            changedBy
        );
    }
    
    public static AccountStatus createFrozen(String reason, String changedBy) {
        return new AccountStatus(
            Status.FROZEN, 
            reason, 
            Instant.now(), 
            changedBy
        );
    }
    
    public static AccountStatus createClosed(String reason, String changedBy) {
        return new AccountStatus(
            Status.CLOSED, 
            reason, 
            Instant.now(), 
            changedBy
        );
    }
    
    public static AccountStatus createDormant(String changedBy) {
        return new AccountStatus(
            Status.DORMANT, 
            "Account marked dormant due to inactivity", 
            Instant.now(), 
            changedBy
        );
    }
    
    public AccountStatus transitionTo(Status newStatus, String reason, String changedBy) {
        validateTransition(newStatus);
        return new AccountStatus(newStatus, reason, Instant.now(), changedBy);
    }
    
    private void validateTransition(Status newStatus) {
        switch (newStatus) {
            case FROZEN:
                if (!status.canBeFrozen()) {
                    throw new IllegalStateException(
                        String.format("Cannot freeze account with status %s", status)
                    );
                }
                break;
            case ACTIVE:
                if (!status.canBeReactivated() && status != Status.ACTIVE) {
                    throw new IllegalStateException(
                        String.format("Cannot reactivate account with status %s", status)
                    );
                }
                break;
            case CLOSED:
                if (!status.canBeClosed()) {
                    throw new IllegalStateException(
                        String.format("Cannot close account with status %s", status)
                    );
                }
                break;
            case DORMANT:
                if (status == Status.CLOSED) {
                    throw new IllegalStateException("Cannot mark closed account as dormant");
                }
                break;
        }
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getReason() {
        return reason;
    }
    
    public Instant getLastStatusChange() {
        return lastStatusChange;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
    
    public boolean isFrozen() {
        return status == Status.FROZEN;
    }
    
    public boolean isClosed() {
        return status == Status.CLOSED;
    }
    
    public boolean isDormant() {
        return status == Status.DORMANT;
    }
    
    public boolean canPerformTransactions() {
        return status.canPerformTransactions();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountStatus that = (AccountStatus) o;
        return status == that.status &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(lastStatusChange, that.lastStatusChange) &&
               Objects.equals(changedBy, that.changedBy);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, reason, lastStatusChange, changedBy);
    }
    
    @Override
    public String toString() {
        return String.format("AccountStatus{status=%s, reason='%s', lastStatusChange=%s, changedBy='%s'}", 
                           status, reason, lastStatusChange, changedBy);
    }
}