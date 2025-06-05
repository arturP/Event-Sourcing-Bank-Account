package io.artur.eventsourcing.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.artur.eventsourcing.events.AccountEvent;

import java.io.IOException;

public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(AccountEvent.class)
                .allowIfSubType("io.artur.eventsourcing.events")
                .build();

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
    }

    public String serialize(AccountEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to serialize event", e);
        }
    }

    public AccountEvent deserialize(String json) {
        try {
            return objectMapper.readValue(json, AccountEvent.class);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize event", e);
        }
    }
}