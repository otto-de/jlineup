package de.otto.jlineup.browser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A browser that runs somewhere else than the machine where JLineup was started, e.g. in AWS Lambda functions.
 * <p>
 * Instances are created with {@link CloudBrowserFactory}. If no implementation is available, JLineup uses a
 * locally installed browser instead.
 */
public interface CloudBrowser {

    /**
     * Takes the screenshots for all contexts remotely and stores the results in the local file structure of the run,
     * as if the screenshots had been taken by a local browser.
     *
     * @param screenshotContexts the contexts to take screenshots for
     * @throws ExecutionException   if a remote call fails
     * @throws InterruptedException if the thread is interrupted while waiting for the remote calls
     * @throws IOException          if the results can't be fetched or stored locally
     */
    void takeScreenshots(List<ScreenshotContext> screenshotContexts) throws ExecutionException, InterruptedException, IOException;

}
