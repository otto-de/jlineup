package de.otto.jlineup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.lookup;

public class Utils {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final static AtomicInteger threadCounter = new AtomicInteger();
    public static final String JLINEUP_FILE_APPENDER = "JLineupFileAppender";
    public static final String SIFTING_APPENDER_NAME_IN_LOGBACK_XML = "SIFT";

    // Pattern to extract errorMessage from Lambda JSON response
    // Matches: "errorMessage":"<message>" or "errorMessage": "<message>"
    private static final Pattern LAMBDA_ERROR_MESSAGE_PATTERN = 
            Pattern.compile("\"errorMessage\"\\s*:\\s*\"([^\"]+)\"");
    
    // Pattern to extract JLineupException message from nested exception strings
    // Matches: JLineupException: <message>
    private static final Pattern JLINEUP_EXCEPTION_PATTERN = 
            Pattern.compile("JLineupException:\\s*(.+?)(?:\"|}|$)");

    public static String readVersion() {
        Properties prop = new Properties();
        try {
            prop.load(Utils.class.getClassLoader().getResourceAsStream("version.properties"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return prop.getProperty("jlineup.version");
    }

    public static String readCommit() {
        Properties prop = new Properties();
        try {
            prop.load(Utils.class.getClassLoader().getResourceAsStream("version.properties"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return prop.getProperty("jlineup.commit");
    }

    public static ExecutorService createThreadPool(int threads, final String baseName) {

        if (threads < 1) {
            threads = 1;
        }

        final ThreadFactory factory = createThreadFactory(baseName);

        LOG.info("Using a thread pool with {} thread(s) to make screenshots.", threads);

        return Executors.newFixedThreadPool(threads, factory);
    }

    public static ThreadFactory createThreadFactory(String baseName) {
        return target -> {
            String name = String.format("%s-%d", baseName, threadCounter.getAndIncrement());
            final Thread thread = new Thread(target, name);
            LOG.debug("Created new worker thread named '{}'.", name);
            thread.setUncaughtExceptionHandler((t, e) -> LOG.error("Exception", e));
            return thread;
        };
    }

    public static void setLogLevelToDebug() {
        //ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        //root.setLevel(Level.DEBUG);
        ch.qos.logback.classic.Logger otto = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("de.otto");
        otto.setLevel(Level.DEBUG);
    }

    public static void setDebugLogLevelsOfSelectedThirdPartyLibsToWarn() {
        ch.qos.logback.classic.Logger apache = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.apache.hc");
        apache.setLevel(Level.WARN);
    }

    public static void logToFile(String workingDir) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setName(JLINEUP_FILE_APPENDER);
        fileAppender.setFile(workingDir + "/jlineup.log");
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        logger.addAppender(fileAppender);
        logger.setLevel(Level.DEBUG);
    }

    public static void stopFileLoggers() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

        Appender<ILoggingEvent> appender = logger.getAppender(JLINEUP_FILE_APPENDER);
        if (appender != null) {
            appender.stop();
        }

        appender = logger.getAppender(SIFTING_APPENDER_NAME_IN_LOGBACK_XML);
        if (appender != null) {
            appender.stop();
        }
    }

    public static String getVersion() {
        return String.format("%s [%s]%n", readVersion(), readCommit());
    }

    public static void writeInfosForCommonErrors(String message) {
        if (message.contains("session deleted because of page crash")) {
            System.err.println("\n=====\n");
            System.err.println("It looks like you're using Google Chrome or Chromium and it crashes while browsing to your configured page.");
            System.err.println("Are you running inside a Docker container? Try to run JLineup with --chrome-parameter \"--disable-dev-shm-usage\" and this error might be gone.");
            System.err.println("\n=====\n");
        }
    }

    /**
     * Extracts a user-friendly error message from an exception chain.
     * <p>
     * This method traverses the exception chain and extracts the most relevant
     * error message for the user, handling special cases like:
     * <ul>
     *   <li>Lambda JSON error responses (extracts errorMessage field)</li>
     *   <li>JLineupException messages (HTTP check failures, timeouts, etc.)</li>
     *   <li>Nested RuntimeException wrappers</li>
     * </ul>
     *
     * @param throwable the exception to extract the message from
     * @return a user-friendly error message
     */
    public static String extractUserFriendlyErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        String message = throwable.getMessage();
        if (message == null) {
            message = throwable.getClass().getSimpleName();
        }

        // Try to extract JLineupException message from the string (handles Lambda JSON responses)
        String jlineupMessage = extractJLineupExceptionMessage(message);
        if (jlineupMessage != null) {
            return jlineupMessage;
        }

        // Try to extract errorMessage from Lambda JSON response
        String lambdaMessage = extractLambdaErrorMessage(message);
        if (lambdaMessage != null) {
            // The lambda message might itself contain a nested JLineupException
            String nestedJlineupMessage = extractJLineupExceptionMessage(lambdaMessage);
            if (nestedJlineupMessage != null) {
                return nestedJlineupMessage;
            }
            return lambdaMessage;
        }

        // Traverse the cause chain to find the root cause message
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
            String causeMessage = current.getMessage();
            if (causeMessage != null) {
                // Check if the cause has a JLineupException message
                jlineupMessage = extractJLineupExceptionMessage(causeMessage);
                if (jlineupMessage != null) {
                    return jlineupMessage;
                }
                // Check if the cause has a Lambda error message
                lambdaMessage = extractLambdaErrorMessage(causeMessage);
                if (lambdaMessage != null) {
                    String nested = extractJLineupExceptionMessage(lambdaMessage);
                    return nested != null ? nested : lambdaMessage;
                }
            }
        }

        // If we found a root cause with a message, use it
        if (current != throwable && current.getMessage() != null) {
            return current.getMessage();
        }

        return message;
    }

    /**
     * Extracts the errorMessage field from a Lambda JSON error response.
     *
     * @param text the text that might contain a Lambda JSON error
     * @return the extracted error message, or null if not found
     */
    public static String extractLambdaErrorMessage(String text) {
        if (text == null || !text.contains("errorMessage")) {
            return null;
        }
        Matcher matcher = LAMBDA_ERROR_MESSAGE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extracts the message from a JLineupException string representation.
     *
     * @param text the text that might contain a JLineupException
     * @return the extracted message, or null if not found
     */
    public static String extractJLineupExceptionMessage(String text) {
        if (text == null || !text.contains("JLineupException")) {
            return null;
        }
        Matcher matcher = JLINEUP_EXCEPTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Gets the root cause of an exception chain.
     *
     * @param throwable the exception
     * @return the root cause, or the original exception if there is no cause
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
