package de.otto.jlineup.browser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CloudBrowser {

    void takeScreenshots(List<ScreenshotContext> screenshotContexts) throws ExecutionException, InterruptedException, IOException;

}
