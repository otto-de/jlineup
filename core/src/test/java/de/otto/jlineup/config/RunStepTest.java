package de.otto.jlineup.config;

import de.otto.jlineup.browser.BrowserStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class RunStepTest {

    @Test
    void shouldConvertBeforeToBrowserStepBefore() {
        BrowserStep browserStep = RunStep.before.toBrowserStep();
        assertEquals(BrowserStep.before, browserStep);
    }

    @Test
    void shouldConvertAfterToBrowserStepAfter() {
        BrowserStep browserStep = RunStep.after.toBrowserStep();
        assertEquals(BrowserStep.after, browserStep);
    }

    @Test
    void shouldConvertAfterOnlyToBrowserStepAfter() {
        BrowserStep browserStep = RunStep.after_only.toBrowserStep();
        assertEquals(BrowserStep.after, browserStep);
    }

    @Test
    void shouldConvertCompareToBrowserStepCompare() {
        BrowserStep browserStep = RunStep.compare.toBrowserStep();
        assertEquals(BrowserStep.compare, browserStep);
    }

    @ParameterizedTest
    @EnumSource(RunStep.class)
    void shouldConvertAllRunStepsWithoutException(RunStep runStep) {
        assertDoesNotThrow(runStep::toBrowserStep);
    }

    @ParameterizedTest
    @EnumSource(RunStep.class)
    void shouldReturnNonNullBrowserStepForAllRunSteps(RunStep runStep) {
        BrowserStep browserStep = runStep.toBrowserStep();
        assertNotNull(browserStep);
    }

    @Test
    void shouldBeAbleToGetRunStepByName() {
        assertEquals(RunStep.before, RunStep.valueOf("before"));
        assertEquals(RunStep.after, RunStep.valueOf("after"));
        assertEquals(RunStep.after_only, RunStep.valueOf("after_only"));
        assertEquals(RunStep.compare, RunStep.valueOf("compare"));
    }

    @Test
    void shouldThrowExceptionForInvalidRunStepName() {
        assertThrows(IllegalArgumentException.class, () -> RunStep.valueOf("invalid"));
    }
}