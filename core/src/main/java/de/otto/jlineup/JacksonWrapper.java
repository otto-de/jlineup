package de.otto.jlineup;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileTracker;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.Reader;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public class JacksonWrapper {

    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                .configure(INDENT_OUTPUT, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .build();

    public static JsonMapper jsonMapper() {
        return jsonMapper;
    }

    private static final JsonMapper jsonMapperForLambdaHandler = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    public static JsonMapper jsonMapperForLambdaHandler() {
        return jsonMapperForLambdaHandler;
    }

    public static String serializeObject(Object object) {
        try {
            return jsonMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException("There is a problem while writing the " + object.getClass().getCanonicalName() + " with Jackson.", e);
        }
    }

    public static String serializeObjectWithPropertyNamingStrategy(Object object, PropertyNamingStrategy propertyNamingStrategy) {
        try {
            return jsonMapper().rebuild().propertyNamingStrategy(propertyNamingStrategy).build().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException("There is a problem while writing the " + object.getClass().getCanonicalName() + " with Jackson.", e);
        }
    }

    public static JobConfig deserializeConfig(Reader reader) {
        try {
            return jsonMapper().readValue(reader, JobConfig.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Error reading config into object.", e);
        }
    }

    public static FileTracker readFileTrackerFile(File file) {
        try {
            return jsonMapper().readValue(file, FileTracker.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Could not read FileTracker file.", e);
        }
    }

}
