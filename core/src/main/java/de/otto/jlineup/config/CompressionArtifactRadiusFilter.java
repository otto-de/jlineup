package de.otto.jlineup.config;

import static de.otto.jlineup.config.JobConfig.DEFAULT_COMPRESSION_ARTIFACT_RADIUS;

/**
 * Jackson value filter for 'compression-artifact-radius': the radius is left out of the serialized config if it is
 * the default value.
 * <p>
 * It is used through {@code @JsonInclude(value = CUSTOM, valueFilter = ...)}. Jackson calls {@link #equals} with the
 * value of the property and omits the property if it returns true.
 */
public class CompressionArtifactRadiusFilter {

    /**
     * @param obj the radius that is about to be serialized
     * @return true if the value is null or the default radius, which means that it should not be serialized
     */
    @Override
    public boolean equals(Object obj) {
        return obj == null || obj.equals(DEFAULT_COMPRESSION_ARTIFACT_RADIUS);
    }
}
