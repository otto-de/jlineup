package de.otto.jlineup.lambda;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public class LambdaProperties {

    public static String getProfile() throws IOException {

        Properties appProps = new Properties();
        appProps.load(LambdaProperties.class.getResourceAsStream("/settings.properties"));
        return appProps.getProperty("awsProfile");
    }

}
