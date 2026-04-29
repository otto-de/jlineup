package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a path entry in a URL configuration.
 * Can be deserialized from either a plain string ({@code "/"}) or an object
 * ({@code {path: "/damen", title: "Damenmode"}}).
 * Serializes back as a plain string when no title is set, preserving backwards compatibility.
 */
@JsonDeserialize(using = PathConfig.PathConfigDeserializer.class)
@JsonSerialize(using = PathConfig.PathConfigSerializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PathConfig {

    public final String path;
    public final String title;

    public PathConfig(String path, String title) {
        this.path = path;
        this.title = title;
    }

    /** Convenience constructor for plain string paths (no title). */
    public static PathConfig of(String path) {
        return new PathConfig(path, null);
    }

    /** Convenience constructor with title. */
    public static PathConfig of(String path, String title) {
        return new PathConfig(path, title);
    }

    /** Returns the title if set, otherwise the path. */
    public String getDisplayName() {
        return title != null && !title.isEmpty() ? title : path;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathConfig that = (PathConfig) o;
        return Objects.equals(path, that.path) && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, title);
    }

    @Override
    public String toString() {
        if (title != null) {
            return "PathConfig{path='" + path + "', title='" + title + "'}";
        }
        return "PathConfig{path='" + path + "'}";
    }

    /**
     * Custom deserializer that handles both:
     * - Plain string: "/"  →  PathConfig("/", null)
     * - Object: {path: "/damen", title: "Damenmode"}  →  PathConfig("/damen", "Damenmode")
     */
    public static class PathConfigDeserializer extends StdDeserializer<PathConfig> {

        public PathConfigDeserializer() {
            super(PathConfig.class);
        }

        @Override
        public PathConfig deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                return PathConfig.of(p.getText());
            }
            // Object form
            String path = null;
            String title = null;
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();
                if ("path".equals(fieldName)) {
                    path = p.getText();
                } else if ("title".equals(fieldName)) {
                    title = p.getText();
                }
                // ignore unknown fields
            }
            return new PathConfig(path, title);
        }
    }

    /**
     * Custom deserializer for List&lt;PathConfig&gt; that also handles
     * ACCEPT_SINGLE_VALUE_AS_ARRAY (single string or object → list of one).
     */
    public static class PathConfigListDeserializer extends StdDeserializer<List<PathConfig>> {

        public PathConfigListDeserializer() {
            super(List.class);
        }

        @Override
        public List<PathConfig> deserialize(JsonParser p, DeserializationContext ctxt) {
            List<PathConfig> result = new ArrayList<>();
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                // Single string value
                result.add(PathConfig.of(p.getText()));
                return result;
            }
            if (p.currentToken() == JsonToken.START_OBJECT) {
                // Single object value
                PathConfigDeserializer itemDeser = new PathConfigDeserializer();
                result.add(itemDeser.deserialize(p, ctxt));
                return result;
            }
            if (p.currentToken() == JsonToken.START_ARRAY) {
                PathConfigDeserializer itemDeser = new PathConfigDeserializer();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    result.add(itemDeser.deserialize(p, ctxt));
                }
            }
            return result;
        }
    }

    /**
     * Custom serializer: writes as plain string when no title, as object when title is present.
     */
    public static class PathConfigSerializer extends StdSerializer<PathConfig> {

        public PathConfigSerializer() {
            super(PathConfig.class);
        }

        @Override
        public void serialize(PathConfig value, JsonGenerator gen, SerializationContext provider) {
            if (value.title == null || value.title.isEmpty()) {
                gen.writeString(value.path);
            } else {
                gen.writeStartObject();
                gen.writeStringProperty("path", value.path);
                gen.writeStringProperty("title", value.title);
                gen.writeEndObject();
            }
        }
    }
}
