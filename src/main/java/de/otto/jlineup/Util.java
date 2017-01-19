package de.otto.jlineup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Util {

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

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

    public static ExecutorService createThreadPool(int threads) {
        final ThreadFactory factory = target -> {
            final Thread thread = new Thread(target);
            LOG.debug("Create new worker thread");
            thread.setUncaughtExceptionHandler((t, e) -> LOG.error("Uncaught Exception", e));
            return thread;
        };
        return Executors.newFixedThreadPool(threads, factory);
    }

}
