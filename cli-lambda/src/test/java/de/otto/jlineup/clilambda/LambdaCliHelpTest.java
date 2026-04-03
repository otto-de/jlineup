package de.otto.jlineup.clilambda;

import de.otto.jlineup.cli.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class LambdaCliHelpTest {

    private ByteArrayOutputStream systemOutCaptor;
    private final PrintStream stdout = System.out;

    @BeforeEach
    void setUpStreams() {
        systemOutCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(systemOutCaptor));
    }

    @AfterEach
    void cleanUpStreams() {
        System.setOut(stdout);
    }

    @Test
    public void shouldShowLambdaAndCommonParametersInCliHelp() throws Exception {
        catchSystemExit(() -> Main.main(new String[]{"--help"}));

        String helpOutput = systemOutCaptor.toString();
        assertThat(helpOutput, containsString("--help"));
        assertThat(helpOutput, containsString("--step"));
        assertThat(helpOutput, containsString("--lambda-function-name"));
        assertThat(helpOutput, containsString("--lambda-aws-profile"));
        assertThat(helpOutput, containsString("--lambda-aws-region"));

        System.out.println(helpOutput);
    }
}
