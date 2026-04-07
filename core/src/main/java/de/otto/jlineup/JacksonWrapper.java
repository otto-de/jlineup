package de.otto.jlineup;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileTracker;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.io.File;
import java.io.Reader;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public class JacksonWrapper {

    public enum ConfigFormat {
        JSON, YAML;

        public static ConfigFormat fromFilename(String filename) {
            if (filename == null) return JSON;
            String lower = filename.toLowerCase();
            if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
                return YAML;
            }
            return JSON;
        }
    }

    private static final JsonMapper jsonMapper = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                .configure(INDENT_OUTPUT, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .build();

    private static final YAMLMapper yamlMapper = YAMLMapper.builder()
                .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(INDENT_OUTPUT, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .configure(YAMLWriteFeature.MINIMIZE_QUOTES, true)
                .configure(YAMLWriteFeature.WRITE_DOC_START_MARKER, false)
                .configure(YAMLWriteFeature.INDENT_ARRAYS_WITH_INDICATOR, true)
                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .build();

    public static JsonMapper jsonMapper() {
        return jsonMapper;
    }

    public static YAMLMapper yamlMapper() {
        return yamlMapper;
    }

    public static ObjectMapper mapperForFormat(ConfigFormat format) {
        return format == ConfigFormat.YAML ? yamlMapper : jsonMapper;
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

    public static String serializeObject(Object object, ConfigFormat format) {
        try {
            return mapperForFormat(format).writeValueAsString(object);
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
        return deserializeConfig(reader, ConfigFormat.JSON);
    }

    public static JobConfig deserializeConfig(Reader reader, ConfigFormat format) {
        try {
            return mapperForFormat(format).readValue(reader, JobConfig.class);
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
