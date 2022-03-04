package de.otto.jlineup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileTracker;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import static com.fasterxml.jackson.core.JsonParser.Feature.*;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public class JacksonWrapper {

    private static final JsonMapper objectMapper = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(ALLOW_COMMENTS, true)
                .configure(INDENT_OUTPUT, true)
                .build();

    static {
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
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
