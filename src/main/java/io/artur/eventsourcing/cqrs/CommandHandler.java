package io.artur.eventsourcing.cqrs;

import io.artur.eventsourcing.commands.Command;

public interface CommandHandler<T extends Command> {
    void handle(T command);
}