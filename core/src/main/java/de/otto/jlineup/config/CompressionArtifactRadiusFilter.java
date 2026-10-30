package de.otto.jlineup.config;

import static de.otto.jlineup.config.JobConfig.DEFAULT_COMPRESSION_ARTIFACT_RADIUS;

public class CompressionArtifactRadiusFilter {

    @Override
    public boolean equals(Object obj) {
        return obj == null || obj.equals(DEFAULT_COMPRESSION_ARTIFACT_RADIUS);
    }
}
