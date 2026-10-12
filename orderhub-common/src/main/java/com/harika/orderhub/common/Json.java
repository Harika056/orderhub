package com.harika.orderhub.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Small JSON helper so services exchange plain strings over Kafka. */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise " + value, e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not parse " + type.getSimpleName() + ": " + json, e);
        }
    }

    private Json() {
    }
}
