package de.otto.jlineup.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;

/**
 * The event {@link LambdaBrowser} sends to a JLineup Lambda function, which takes the screenshots of exactly one
 * screenshot context. It is handled by {@link JLineupHandler}.
 *
 * @param runId             identifies the whole run; all Lambda calls of one run share it, and it is part of the S3
 *                          location of the results
 * @param jobConfig         the complete job configuration
 * @param screenshotContext the context to take screenshots for
 * @param urlKey            the key of the URL in the job config; sent separately because it is
 *                          {@code @JsonIgnore}d in {@link ScreenshotContext}
 * @param step              the run step to execute; sent separately because it is {@code @JsonIgnore}d in
 *                          {@link ScreenshotContext}
 */
public record LambdaRequestPayload(String runId, JobConfig jobConfig, ScreenshotContext screenshotContext, RunStep step,
                                   String urlKey) {

    /**
     * Marks the canonical constructor as the one Jackson uses to deserialize the payload.
     */
    @JsonCreator
    public LambdaRequestPayload {
    }


}
