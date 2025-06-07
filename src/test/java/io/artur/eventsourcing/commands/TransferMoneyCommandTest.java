package io.artur.eventsourcing.commands;

import io.artur.eventsourcing.events.EventMetadata;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferMoneyCommandTest {

    private final UUID fromAccountId = UUID.randomUUID();
    private final UUID toAccountId = UUID.randomUUID();
    private final BigDecimal validAmount = BigDecimal.valueOf(100.50);
    private final String description = "Test transfer";
    private final EventMetadata metadata = new EventMetadata(1);

    @Test
    void shouldCreateValidTransferCommand() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, validAmount, description, metadata);
        
        assertEquals(fromAccountId, command.getAggregateId());
        assertEquals(fromAccountId, command.getFromAccountId());
        assertEquals(toAccountId, command.getToAccountId());
        assertEquals(validAmount, command.getAmount());
        assertEquals(description, command.getDescription());
        assertEquals(metadata, command.getMetadata());
        
        assertDoesNotThrow(command::validate);
    }

    @Test
    void shouldThrowExceptionWhenFromAccountIdIsNull() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            null, toAccountId, validAmount, description, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("From account ID cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenToAccountIdIsNull() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, null, validAmount, description, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("To account ID cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFromAndToAccountsAreSame() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, fromAccountId, validAmount, description, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("Cannot transfer money to the same account", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNull() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, null, description, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("Amount cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsZero() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, BigDecimal.ZERO, description, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("Transfer amount must be positive", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, BigDecimal.valueOf(-50), description, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("Transfer amount must be positive", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAmountHasMoreThanTwoDecimalPlaces() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, BigDecimal.valueOf(100.123), description, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("Amount cannot have more than 2 decimal places", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIsTooLong() {
        String longDescription = "a".repeat(256); // 256 characters
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, validAmount, longDescription, metadata);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, command::validate);
        assertEquals("Description cannot exceed 255 characters", exception.getMessage());
    }

    @Test
    void shouldAllowNullDescription() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, validAmount, null, metadata);
        
        assertDoesNotThrow(command::validate);
        assertNull(command.getDescription());
    }

    @Test
    void shouldAllowEmptyDescription() {
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, validAmount, "", metadata);
        
        assertDoesNotThrow(command::validate);
        assertEquals("", command.getDescription());
    }

    @Test
    void shouldAllowValidDescriptionAt255Characters() {
        String maxDescription = "a".repeat(255); // Exactly 255 characters
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, toAccountId, validAmount, maxDescription, metadata);
        
        assertDoesNotThrow(command::validate);
        assertEquals(maxDescription, command.getDescription());
    }
}