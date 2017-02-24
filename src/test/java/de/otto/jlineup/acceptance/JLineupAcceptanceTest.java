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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;


public class JLineupAcceptanceTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private final ByteArrayOutputStream sysOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream sysErr = new ByteArrayOutputStream();
    public static final String CWD = Paths.get(".").toAbsolutePath().normalize().toString();

    private PrintStream stdout = System.out;
    private PrintStream stderr = System.err;

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
        exit.checkAssertionAfterwards(() -> assertThat(sysErr.toString(), containsString("No urls are configured in the config.")));
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
        exit.checkAssertionAfterwards(() -> assertThat(sysOut.toString(), containsString("http://www.example.com")));
        exit.expectSystemExitWithStatus(0);
        final Path tempDirectory = Files.createTempDirectory("jlineup-acceptance-test");
        Main.main(new String[]{"--working-dir",tempDirectory.toString(),"--print-config"});
    }

}
