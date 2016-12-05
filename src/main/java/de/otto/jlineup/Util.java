package de.otto.jlineup;

import java.io.IOException;
import java.util.Properties;

public class Util {

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

}
