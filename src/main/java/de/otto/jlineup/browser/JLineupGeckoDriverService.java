package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.firefox.internal.Executable;
import org.openqa.selenium.remote.service.DriverService;

import java.io.File;
import java.io.IOException;

class JLineupGeckoDriverService extends GeckoDriverService {

    public JLineupGeckoDriverService(File executable, int port, ImmutableList<String> args, ImmutableMap<String, String> environment) throws IOException {
        super(executable, port, args, environment);
    }

    /**
     * Builder used to configure new {@link org.openqa.selenium.firefox.GeckoDriverService} instances.
     */
    static class Builder extends DriverService.Builder<
            org.openqa.selenium.firefox.GeckoDriverService, org.openqa.selenium.firefox.GeckoDriverService.Builder> {

        @Override
        protected File findDefaultExecutable() {
            return findExecutable("wires", GECKO_DRIVER_EXE_PROPERTY,
                    "https://github.com/jgraham/wires",
                    "https://github.com/jgraham/wires");
        }

        @Override
        protected ImmutableList<String> createArgs() {
            ImmutableList.Builder<String> argsBuilder = ImmutableList.builder();
            argsBuilder.add(String.format("--webdriver-port=%d", getPort()));
            if (getLogFile() != null) {
                argsBuilder.add(String.format("--log-file=\"%s\"", getLogFile().getAbsolutePath()));
            }
            argsBuilder.add("-b");
            argsBuilder.add(new Executable(null).getPath());
            return argsBuilder.build();
        }

        @Override
        protected org.openqa.selenium.firefox.GeckoDriverService createDriverService(File exe, int port,
                                                                                     ImmutableList<String> args,
                                                                                     ImmutableMap<String, String> environment) {
            try {
                return new org.openqa.selenium.firefox.GeckoDriverService(exe, port, args, environment);
            } catch (IOException e) {
                throw new WebDriverException(e);
            }
        }
    }
}
