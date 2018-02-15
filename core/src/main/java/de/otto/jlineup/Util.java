package de.otto.jlineup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.google.gson.GsonBuilder;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Util {

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    private final static AtomicInteger threadCounter = new AtomicInteger();

    public static String readVersion() {
        Properties prop = new Properties();
        try {
            prop.load(Main.class.getClassLoader().getResourceAsStream("version.properties"));
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return prop.getProperty("jlineup.version");
    }

    public static String readCommit() {
        Properties prop = new Properties();
        try {
            prop.load(Main.class.getClassLoader().getResourceAsStream("version.properties"));
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return prop.getProperty("jlineup.commit");
    }

    public static ExecutorService createThreadPool(int threads, String baseName) {
        final ThreadFactory factory = target -> {
            String name = String.format("%s-%d", baseName, threadCounter.getAndIncrement());
            final Thread thread = new Thread(target, name);
            LOG.debug("Created new worker thread.");
            thread.setUncaughtExceptionHandler((t, e) -> LOG.error("Exception", e));
            return thread;
        };
        return Executors.newFixedThreadPool(threads, factory);
    }

    static void setLogLevelToDebug() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);
    }

    static void logToFile(Parameters parameters) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setFile(parameters.getWorkingDirectory() + "/jlineup.log");
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        logger.addAppender(fileAppender);
        logger.setLevel(Level.DEBUG);
    }

    static boolean shouldUseLegacyReportFormat(Config config) {
        return (config.reportFormat != null && config.reportFormat == 1) || (config.reportFormat == null && Config.DEFAULT_REPORT_FORMAT == 1);
    }

    static String getVersion() {
        return String.format("%s [%s]%n", readVersion(), readCommit());
    }

    static String createPrettyConfigJson(Config config) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(config);
    }
}
