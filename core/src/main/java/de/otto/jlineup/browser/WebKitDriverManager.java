package de.otto.jlineup.browser;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Manages WebKitWebDriver and Xvfb processes for WebKit browser automation.
 * <p>
 * WebKit (via WebKitGTK) has no native headless mode, so Xvfb is always required.
 * WebKitWebDriver is started as a separate process and connected to via Selenium RemoteWebDriver.
 */
class WebKitDriverManager {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private static final int DRIVER_STARTUP_TIMEOUT_SECONDS = 30;
    private static final int DRIVER_POLL_INTERVAL_MS = 200;

    /**
     * Creates a RemoteWebDriver connected to a freshly started WebKitWebDriver process.
     * An Xvfb display is automatically started for rendering.
     * <p>
     * The returned driver's {@code quit()} method is overridden to also clean up the
     * WebKitWebDriver and Xvfb processes.
     */
    static RemoteWebDriver createWebKitDriver() {
        int xvfbDisplay = findFreeDisplay();
        int webDriverPort = findFreePort();

        Process dbusProcess = startDbusSession();
        Process xvfbProcess = startXvfb(xvfbDisplay);
        Process webKitDriverProcess = startWebKitWebDriver(webDriverPort, xvfbDisplay);

        waitForDriverReady(webDriverPort);

        try {
            URL driverUrl = new URL("http://localhost:" + webDriverPort);
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setBrowserName("MiniBrowser");

            RemoteWebDriver driver = new RemoteWebDriver(driverUrl, capabilities) {
                @Override
                public void quit() {
                    try {
                        super.quit();
                    } catch (Exception e) {
                        LOG.debug("Exception during WebKit RemoteWebDriver quit: {}", e.getMessage());
                    } finally {
                        destroyProcess(webKitDriverProcess, "WebKitWebDriver");
                        destroyProcess(xvfbProcess, "Xvfb");
                        destroyProcess(dbusProcess, "dbus-daemon");
                    }
                }
            };

            LOG.info("WebKit browser started (Xvfb display :{}, WebKitWebDriver port {})", xvfbDisplay, webDriverPort);
            return driver;
        } catch (Exception e) {
            // Clean up if driver connection fails
            destroyProcess(webKitDriverProcess, "WebKitWebDriver");
            destroyProcess(xvfbProcess, "Xvfb");
            destroyProcess(dbusProcess, "dbus-daemon");
            throw new RuntimeException("Failed to create WebKit RemoteWebDriver on port " + webDriverPort, e);
        }
    }

    private static Process startDbusSession() {
        try {
            // Start a private D-Bus session daemon; MiniBrowser needs D-Bus for IPC
            ProcessBuilder pb = new ProcessBuilder(
                    "dbus-daemon", "--session", "--nofork", "--print-address"
            );
            pb.environment().put("DBUS_SESSION_BUS_ADDRESS", "unix:path=/tmp/dbus-session-" + ProcessHandle.current().pid());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // Give dbus a moment to start
            Thread.sleep(300);
            if (!process.isAlive()) {
                LOG.warn("dbus-daemon failed to start, continuing without it");
                return null;
            }
            LOG.debug("dbus-daemon session started (pid {})", process.pid());
            return process;
        } catch (IOException | InterruptedException e) {
            LOG.warn("Failed to start dbus-daemon, continuing without it: {}", e.getMessage());
            return null;
        }
    }

    private static Process startXvfb(int display) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "Xvfb", ":" + display, "-screen", "0", "1920x1200x24", "-nolisten", "tcp"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // Give Xvfb a moment to start
            Thread.sleep(500);
            if (!process.isAlive()) {
                throw new RuntimeException("Xvfb failed to start on display :" + display);
            }
            LOG.debug("Xvfb started on display :{}", display);
            return process;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to start Xvfb on display :" + display, e);
        }
    }

    private static Process startWebKitWebDriver(int port, int xvfbDisplay) {
        try {
            ProcessBuilder pb = new ProcessBuilder("WebKitWebDriver", "--port=" + port);
            pb.environment().put("DISPLAY", ":" + xvfbDisplay);
            pb.environment().put("GDK_BACKEND", "x11");
            pb.environment().put("DBUS_SESSION_BUS_ADDRESS", "unix:path=/tmp/dbus-session-" + ProcessHandle.current().pid());
            // Disable WebKit's multiprocess mode — use single-process to avoid sandbox issues in Lambda
            pb.environment().put("WEBKIT_DISABLE_SANDBOX_THIS_IS_DANGEROUS", "1");
            pb.environment().put("GTK_OVERLAY_SCROLLING", "0");
            pb.environment().put("WEBKIT_DISABLE_COMPOSITING_MODE", "1");
            pb.environment().put("WEBKIT_DISABLE_GPU_PROCESS", "1");
            pb.environment().put("WEBKIT_DISABLE_ACCELERATED_2D_CANVAS", "1");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            LOG.debug("WebKitWebDriver starting on port {} with DISPLAY :{}", port, xvfbDisplay);
            return process;
        } catch (IOException e) {
            throw new RuntimeException("Failed to start WebKitWebDriver on port " + port, e);
        }
    }

    private static void waitForDriverReady(int port) {
        long deadline = System.currentTimeMillis() + DRIVER_STARTUP_TIMEOUT_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                var connection = new URL("http://localhost:" + port + "/status").openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                int responseCode = ((java.net.HttpURLConnection) connection).getResponseCode();
                if (responseCode == 200) {
                    LOG.debug("WebKitWebDriver is ready on port {}", port);
                    return;
                }
            } catch (IOException ignored) {
                // Not ready yet
            }
            try {
                Thread.sleep(DRIVER_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for WebKitWebDriver", e);
            }
        }
        throw new RuntimeException("WebKitWebDriver did not become ready within " + DRIVER_STARTUP_TIMEOUT_SECONDS + " seconds on port " + port);
    }

    private static void destroyProcess(Process process, String name) {
        if (process != null && process.isAlive()) {
            LOG.debug("Stopping {} process (pid {})", name, process.pid());
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    LOG.warn("{} did not stop gracefully, force killing", name);
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find a free port for WebKitWebDriver", e);
        }
    }

    private static int findFreeDisplay() {
        // Start from display 99 and go up to avoid conflicts
        for (int display = 99; display < 200; display++) {
            // Check if the X11 lock file exists
            java.io.File lockFile = new java.io.File("/tmp/.X" + display + "-lock");
            if (!lockFile.exists()) {
                return display;
            }
        }
        throw new RuntimeException("Could not find a free X11 display number");
    }
}
