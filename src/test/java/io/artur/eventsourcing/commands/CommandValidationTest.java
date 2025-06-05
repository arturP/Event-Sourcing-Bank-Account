package io.artur.eventsourcing.commands;

import io.artur.eventsourcing.events.EventMetadata;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommandValidationTest {

    @Test
    void openAccountCommandValidation() {
        EventMetadata metadata = new EventMetadata(1);
        
        // Valid command
        OpenAccountCommand validCommand = new OpenAccountCommand(
                UUID.randomUUID(), "John Doe", BigDecimal.valueOf(100), metadata);
        assertDoesNotThrow(validCommand::validate);
        
        // Null account ID
        assertThrows(IllegalArgumentException.class, () ->
                new OpenAccountCommand(null, "John Doe", BigDecimal.valueOf(100), metadata).validate());
        
        // Null account holder
        assertThrows(IllegalArgumentException.class, () ->
                new OpenAccountCommand(UUID.randomUUID(), null, BigDecimal.valueOf(100), metadata).validate());
        
        // Empty account holder
        assertThrows(IllegalArgumentException.class, () ->
                new OpenAccountCommand(UUID.randomUUID(), "", BigDecimal.valueOf(100), metadata).validate());
        
        // Null overdraft limit
        assertThrows(IllegalArgumentException.class, () ->
                new OpenAccountCommand(UUID.randomUUID(), "John Doe", null, metadata).validate());
        
        // Negative overdraft limit
        assertThrows(IllegalArgumentException.class, () ->
                new OpenAccountCommand(UUID.randomUUID(), "John Doe", BigDecimal.valueOf(-10), metadata).validate());
    }

    @Test
    void depositMoneyCommandValidation() {
        EventMetadata metadata = new EventMetadata(1);
        UUID accountId = UUID.randomUUID();
        
        // Valid command
        DepositMoneyCommand validCommand = new DepositMoneyCommand(accountId, BigDecimal.valueOf(100.50), metadata);
        assertDoesNotThrow(validCommand::validate);
        
        // Null account ID
        assertThrows(IllegalArgumentException.class, () ->
                new DepositMoneyCommand(null, BigDecimal.valueOf(100), metadata).validate());
        
        // Null amount
        assertThrows(IllegalArgumentException.class, () ->
                new DepositMoneyCommand(accountId, null, metadata).validate());
        
        // Zero amount
        assertThrows(IllegalArgumentException.class, () ->
                new DepositMoneyCommand(accountId, BigDecimal.ZERO, metadata).validate());
        
        // Negative amount
        assertThrows(IllegalArgumentException.class, () ->
                new DepositMoneyCommand(accountId, BigDecimal.valueOf(-10), metadata).validate());
        
        // Too many decimal places
        assertThrows(IllegalArgumentException.class, () ->
                new DepositMoneyCommand(accountId, BigDecimal.valueOf(10.123), metadata).validate());
    }

    @Test
    void withdrawMoneyCommandValidation() {
        EventMetadata metadata = new EventMetadata(1);
        UUID accountId = UUID.randomUUID();
        
        // Valid command
        WithdrawMoneyCommand validCommand = new WithdrawMoneyCommand(accountId, BigDecimal.valueOf(50.25), metadata);
        assertDoesNotThrow(validCommand::validate);
        
        // Null account ID
        assertThrows(IllegalArgumentException.class, () ->
                new WithdrawMoneyCommand(null, BigDecimal.valueOf(100), metadata).validate());
        
        // Null amount
        assertThrows(IllegalArgumentException.class, () ->
                new WithdrawMoneyCommand(accountId, null, metadata).validate());
        
        // Zero amount
        assertThrows(IllegalArgumentException.class, () ->
                new WithdrawMoneyCommand(accountId, BigDecimal.ZERO, metadata).validate());
        
        // Negative amount
        assertThrows(IllegalArgumentException.class, () ->
                new WithdrawMoneyCommand(accountId, BigDecimal.valueOf(-10), metadata).validate());
        
        // Too many decimal places
        assertThrows(IllegalArgumentException.class, () ->
                new WithdrawMoneyCommand(accountId, BigDecimal.valueOf(10.123), metadata).validate());
    }
}