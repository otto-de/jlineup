package de.otto.jlineup.lambda;

import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;

public class LambdaRequestPayload {

    public final String runId;
    public final JobConfig jobConfig;
    public final ScreenshotContext screenshotContext;

    //This is here additionally because it has JsonIgnore annotation in ScreenshotContext
    public final String urlKey;

    //Also step is not in ScreenshotContext because of to JsonIgnore
    public final RunStep step;


    public LambdaRequestPayload(String runId, JobConfig jobConfig, ScreenshotContext screenshotContext, RunStep step, String urlKey) {
        this.runId = runId;
        this.jobConfig = jobConfig;
        this.screenshotContext = screenshotContext;
        this.urlKey = urlKey;
        this.step = step;
    }

    public LambdaRequestPayload() {
        runId = null;
        jobConfig = null;
        screenshotContext = null;
        urlKey = null;
        step = null;
    }

}
