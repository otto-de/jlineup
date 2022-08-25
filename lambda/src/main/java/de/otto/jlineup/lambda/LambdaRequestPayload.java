package de.otto.jlineup.lambda;

import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;

public class LambdaRequestPayload {

    public final String runId;
    public final JobConfig jobConfig;
    public final ScreenshotContext screenshotContext;
    public final RunStep step;

    public LambdaRequestPayload(String runId, JobConfig jobConfig, ScreenshotContext screenshotContext, RunStep step) {
        this.runId = runId;
        this.jobConfig = jobConfig;
        this.screenshotContext = screenshotContext;
        this.step = step;
    }

    public LambdaRequestPayload() {
        runId = null;
        jobConfig = null;
        screenshotContext = null;
        step = null;
    }

}
