package de.otto.jlineup.report;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ComparisonReporterTest {

    @Test
    public void shouldFindVerticalScrollPositionInImageFileName() throws Exception {
        String fileName = "url_root_1001_2002_after.png";
        int yPos = ComparisonReporter.extractVerticalScrollPositionFromFileName(fileName);
        assertThat(yPos, is(2002));
    }

    @Test
    public void shouldFindWindowWidthInImageFileName() throws Exception {
        String fileName = "url_root_1001_2002_after.png";
        int yPos = ComparisonReporter.extractWindowWidthFromFileName(fileName);
        assertThat(yPos, is(1001));
    }
}