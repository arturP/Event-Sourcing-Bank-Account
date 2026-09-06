package io.artur.bankaccount.application.commands.models;

import java.util.Map;
import java.util.UUID;

/** Request context crossing an input port; domain event metadata is created by the use case. */
public record CommandContext(String correlationId,
                             String causationId,
                             String userId,
                             String userAgent,
                             String ipAddress,
                             int version,
                             Map<String, String> additionalProperties) {
    public CommandContext {
        correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        additionalProperties = additionalProperties != null ? Map.copyOf(additionalProperties) : Map.of();
    }

    public CommandContext(int version) {
        this(null, null, null, null, null, version, Map.of());
    }
}
