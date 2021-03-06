package de.otto.jlineup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileTracker;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import static com.fasterxml.jackson.core.JsonParser.Feature.*;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public class JacksonWrapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(INDENT_OUTPUT)
            .enable(ALLOW_COMMENTS)
            .enable(ALLOW_TRAILING_COMMA)
            .enable(ALLOW_UNQUOTED_CONTROL_CHARS)
            .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

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

    public static String serializeObjectWithPropertyNamingStrategy(Object object, PropertyNamingStrategy propertyNamingStrategy) {
        try {
            return objectMapper().copy().setPropertyNamingStrategy(propertyNamingStrategy).writeValueAsString(object);
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

    public static FileTracker readFileTrackerFile(File file) {
        try {
            return objectMapper().readValue(file, FileTracker.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not read FileTracker file.", e);
        }
    }

}
