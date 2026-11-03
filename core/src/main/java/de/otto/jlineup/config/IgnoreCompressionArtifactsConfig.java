package de.otto.jlineup.config;

import de.otto.jlineup.image.CompressionArtifactDetector;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Objects;

import static de.otto.jlineup.config.JobConfig.DEFAULT_COMPRESSION_ARTIFACT_RADIUS;
import static de.otto.jlineup.config.JobConfig.DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE;
import static de.otto.jlineup.config.JobConfig.DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE;

/**
 * Settings of 'ignore-compression-artifacts'. Can be written as a boolean ({@code true}) or as an object
 * ({@code {radius: 12, max-pixel-color-distance: 15, texture-tolerance: 0.15}}). An object is enabled unless it
 * contains {@code enabled: false}.
 * <p>
 * Serializes as {@code true} if enabled with default values, as an object with only the non-default values
 * otherwise, and is left out of the serialized config if disabled with default values.
 */
@JsonDeserialize(using = IgnoreCompressionArtifactsConfig.Deserializer.class)
@JsonSerialize(using = IgnoreCompressionArtifactsConfig.Serializer.class)
public class IgnoreCompressionArtifactsConfig {

    public static final IgnoreCompressionArtifactsConfig DISABLED = new IgnoreCompressionArtifactsConfig(false);
    public static final IgnoreCompressionArtifactsConfig ENABLED = new IgnoreCompressionArtifactsConfig(true);

    public final boolean enabled;
    public final int radius;
    public final double maxPixelColorDistance;
    public final double textureTolerance;

    public IgnoreCompressionArtifactsConfig(boolean enabled, int radius, double maxPixelColorDistance, double textureTolerance) {
        this.enabled = enabled;
        this.radius = radius;
        this.maxPixelColorDistance = maxPixelColorDistance;
        this.textureTolerance = textureTolerance;
    }

    public IgnoreCompressionArtifactsConfig(boolean enabled) {
        this(enabled, DEFAULT_COMPRESSION_ARTIFACT_RADIUS, DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRadius() {
        return radius;
    }

    public double getMaxPixelColorDistance() {
        return maxPixelColorDistance;
    }

    public double getTextureTolerance() {
        return textureTolerance;
    }

    public CompressionArtifactDetector toDetector() {
        return new CompressionArtifactDetector(radius, maxPixelColorDistance, textureTolerance);
    }

    private boolean hasDefaultValues() {
        return radius == DEFAULT_COMPRESSION_ARTIFACT_RADIUS
                && Double.compare(maxPixelColorDistance, DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE) == 0
                && Double.compare(textureTolerance, DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE) == 0;
    }

    /**
     * Enabled if one of them is enabled, every value is taken from this config unless it is the default value.
     */
    public IgnoreCompressionArtifactsConfig mergeWith(IgnoreCompressionArtifactsConfig other) {
        return new IgnoreCompressionArtifactsConfig(
                enabled || other.enabled,
                radius != DEFAULT_COMPRESSION_ARTIFACT_RADIUS ? radius : other.radius,
                Double.compare(maxPixelColorDistance, DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE) != 0 ? maxPixelColorDistance : other.maxPixelColorDistance,
                Double.compare(textureTolerance, DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE) != 0 ? textureTolerance : other.textureTolerance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IgnoreCompressionArtifactsConfig that = (IgnoreCompressionArtifactsConfig) o;
        return enabled == that.enabled && radius == that.radius
                && Double.compare(maxPixelColorDistance, that.maxPixelColorDistance) == 0
                && Double.compare(textureTolerance, that.textureTolerance) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, radius, maxPixelColorDistance, textureTolerance);
    }

    @Override
    public String toString() {
        return "IgnoreCompressionArtifactsConfig{" +
                "enabled=" + enabled +
                ", radius=" + radius +
                ", maxPixelColorDistance=" + maxPixelColorDistance +
                ", textureTolerance=" + textureTolerance +
                '}';
    }

    public static class Deserializer extends StdDeserializer<IgnoreCompressionArtifactsConfig> {

        public Deserializer() {
            super(IgnoreCompressionArtifactsConfig.class);
        }

        @Override
        public IgnoreCompressionArtifactsConfig deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.currentToken() != JsonToken.START_OBJECT) {
                return new IgnoreCompressionArtifactsConfig(Boolean.parseBoolean(p.getText()));
            }
            boolean enabled = true;
            int radius = DEFAULT_COMPRESSION_ARTIFACT_RADIUS;
            double maxPixelColorDistance = DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE;
            double textureTolerance = DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE;
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();
                switch (fieldName) {
                    case "enabled" -> enabled = Boolean.parseBoolean(p.getText());
                    case "radius" -> radius = p.getIntValue();
                    case "max-pixel-color-distance" -> maxPixelColorDistance = p.getDoubleValue();
                    case "texture-tolerance" -> textureTolerance = p.getDoubleValue();
                    default -> p.skipChildren();
                }
            }
            return new IgnoreCompressionArtifactsConfig(enabled, radius, maxPixelColorDistance, textureTolerance);
        }
    }

    public static class Serializer extends StdSerializer<IgnoreCompressionArtifactsConfig> {

        public Serializer() {
            super(IgnoreCompressionArtifactsConfig.class);
        }

        @Override
        public boolean isEmpty(SerializationContext provider, IgnoreCompressionArtifactsConfig value) {
            return value == null || (!value.enabled && value.hasDefaultValues());
        }

        @Override
        public void serialize(IgnoreCompressionArtifactsConfig value, JsonGenerator gen, SerializationContext provider) {
            if (value.hasDefaultValues()) {
                gen.writeBoolean(value.enabled);
                return;
            }
            gen.writeStartObject();
            if (!value.enabled) {
                gen.writeBooleanProperty("enabled", false);
            }
            if (value.radius != DEFAULT_COMPRESSION_ARTIFACT_RADIUS) {
                gen.writeNumberProperty("radius", value.radius);
            }
            if (Double.compare(value.maxPixelColorDistance, DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE) != 0) {
                gen.writeNumberProperty("max-pixel-color-distance", value.maxPixelColorDistance);
            }
            if (Double.compare(value.textureTolerance, DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE) != 0) {
                gen.writeNumberProperty("texture-tolerance", value.textureTolerance);
            }
            gen.writeEndObject();
        }
    }
}
