package de.otto.jlineup.config;

import static de.otto.jlineup.config.JobConfig.DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE;

public class MaxCompressionArtifactColorDistanceFilter {

    @Override
    public boolean equals(Object obj) {
        return obj == null || obj.equals(DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE);
    }
}
