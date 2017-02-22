package de.otto.jlineup.acceptance;

import de.otto.jlineup.Main;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class JLineupAcceptanceTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private final ByteArrayOutputStream sysOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream sysErr = new ByteArrayOutputStream();
    public static final String CWD = Paths.get(".").toAbsolutePath().normalize().toString();

    PrintStream stdout = System.out;
    PrintStream stderr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(sysOut));
        System.setErr(new PrintStream(sysErr));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(stdout);
        System.setErr(stderr);
    }

    @Test
    public void shouldExitWithExitStatus1IfConfigHasNoUrls() throws Exception {
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(() -> assertThat(sysErr.toString(), is("No urls are configured in the config.\n")));
        final Path tempDirectory = Files.createTempDirectory("jlineup-acceptance-test");
        Main.main(new String[]{"--working-dir",tempDirectory.toString(),"--config","src/test/resources/acceptance/acceptance_no_urls.lineup.json"});
    }

    @Test
    public void shouldOpenTestPage() throws Exception {
        final Path tempDirectory = Files.createTempDirectory("jlineup-acceptance-test");
        Main.main(new String[]{"--working-dir",tempDirectory.toString(),"--url","file://"+ CWD +"/src/test/resources/acceptance/webpage/test.html"});
    }

    @Test
    public void shouldPrintConfig() throws Exception {
        String expectedConfig = "{\n" +
                "  \"urls\": {\n" +
                "    \"http://www.example.com\": {\n" +
                "      \"paths\": [\n" +
                "        \"/\",\n" +
                "        \"someOtherPath\"\n" +
                "      ],\n" +
                "      \"max-diff\": 0.0,\n" +
                "      \"cookies\": [\n" +
                "        {\n" +
                "          \"name\": \"exampleCookieName\",\n" +
                "          \"value\": \"exampleValue\",\n" +
                "          \"domain\": \"http://www.example.com\",\n" +
                "          \"path\": \"/\",\n" +
                "          \"expiry\": \"Jan 1, 1970 1:00:01 AM\",\n" +
                "          \"secure\": true\n" +
                "        }\n" +
                "      ],\n" +
                "      \"env-mapping\": {\n" +
                "        \"live\": \"www\"\n" +
                "      },\n" +
                "      \"local-storage\": {\n" +
                "        \"exampleLocalStorageKey\": \"value\"\n" +
                "      },\n" +
                "      \"session-storage\": {\n" +
                "        \"exampleSessionStorageKey\": \"value\"\n" +
                "      },\n" +
                "      \"window-widths\": [\n" +
                "        600,\n" +
                "        800,\n" +
                "        1000\n" +
                "      ],\n" +
                "      \"max-scroll-height\": 100000,\n" +
                "      \"wait-after-page-load\": 0,\n" +
                "      \"wait-after-scroll\": 0,\n" +
                "      \"wait-for-no-animation-after-scroll\": 0.0,\n" +
                "      \"warmup-browser-cache-time\": 0,\n" +
                "      \"wait-for-fonts-time\": 0,\n" +
                "      \"javascript\": \"console.log(\\u0027This is JavaScript!\\u0027)\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"browser\": \"PhantomJS\",\n" +
                "  \"wait-after-page-load\": 0.0,\n" +
                "  \"window-height\": 800,\n" +
                "  \"debug\": false,\n" +
                "  \"threads\": 1\n" +
                "}\n";
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(() -> assertThat(sysOut.toString(), is(expectedConfig)));
        final Path tempDirectory = Files.createTempDirectory("jlineup-acceptance-test");
        Main.main(new String[]{"--working-dir",tempDirectory.toString(),"--print-config"});
    }

}
