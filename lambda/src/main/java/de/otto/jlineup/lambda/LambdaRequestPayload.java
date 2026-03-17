package de.otto.jlineup.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;

/**
 * @param urlKey This is here additionally because it has JsonIgnore annotation in ScreenshotContext
 * @param step   Also step is not in ScreenshotContext because of to JsonIgnore
 */
public record LambdaRequestPayload(String runId, JobConfig jobConfig, ScreenshotContext screenshotContext, RunStep step,
                                   String urlKey) {

    @JsonCreator
    public LambdaRequestPayload {
    }


}
