package io.artur.eventsourcing.commands;

import io.artur.eventsourcing.events.EventMetadata;

import java.util.UUID;

public interface Command {
    UUID getAggregateId();
    EventMetadata getMetadata();
    void validate();
}