package io.artur.eventsourcing.serialization;

public class SerializationException extends RuntimeException {

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}