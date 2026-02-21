package de.otto.jlineup.browser;

import de.otto.jlineup.config.JobConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.time.Instant;
import java.util.logging.Level;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogErrorCheckerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private WebDriver webDriver;

    @Test
    void shouldCheckForErrors() {
        //given
        when(webDriver.manage().logs().get(LogType.BROWSER)).thenReturn(new LogEntries(of(new LogEntry(Level.SEVERE, Instant.now().toEpochMilli(), "Fehler!"))));

        //when/then
        assertThrows(WebDriverException.class, () ->
            new LogErrorChecker().checkForErrors(webDriver, JobConfig.exampleConfig())
        );
    }

    @Test
    void shouldNotCheckForErrorsIfDisabled() {
        //given
        when(webDriver.manage().logs().get(LogType.BROWSER)).thenReturn(new LogEntries(of(new LogEntry(Level.SEVERE, Instant.now().toEpochMilli(), "Fehler!"))));
        JobConfig jobConfig = JobConfig.exampleConfigBuilder().withCheckForErrorsInLog(false).build();

        //when
        new LogErrorChecker().checkForErrors(webDriver, jobConfig);

        //then expect no exception

    }
}