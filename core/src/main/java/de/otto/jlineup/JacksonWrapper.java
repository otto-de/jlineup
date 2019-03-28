package de.otto.jlineup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.otto.jlineup.config.JobConfig;

import java.io.IOException;
import java.io.Reader;

public class JacksonWrapper {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private static ObjectMapper objectMapper() {
        return objectMapper;
    }

    public static String serializeObject(Object object) {
        try {
            return objectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("There is a problem while writing the " + object.getClass().getCanonicalName() + " with Jackson.", e);
        }
    }

    public static JobConfig deserializeConfig(Reader reader) {
        try {
            return objectMapper().readValue(reader, JobConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading config into object.", e);
        }
    }

}
