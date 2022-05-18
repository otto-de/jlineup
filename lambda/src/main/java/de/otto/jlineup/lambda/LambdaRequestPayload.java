package de.otto.jlineup.lambda;

import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;

public class LambdaRequestPayload {

    public final String runId;
    public final JobConfig jobConfig;
    public final ScreenshotContext screenshotContext;

    public LambdaRequestPayload(String runId, JobConfig jobConfig, ScreenshotContext screenshotContext) {
        this.runId = runId;
        this.jobConfig = jobConfig;
        this.screenshotContext = screenshotContext;
    }
}
