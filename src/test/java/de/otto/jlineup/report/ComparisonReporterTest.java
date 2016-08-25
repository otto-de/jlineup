package de.otto.jlineup.report;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.config.Parameters;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static de.otto.jlineup.image.ImageUtils.AFTER;
import static de.otto.jlineup.image.ImageUtils.BEFORE;
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

    @Test
    public void shouldFindBeforeImagesInDirectory() throws IOException {
        Parameters parameters = new Parameters();
        new JCommander(parameters, "-d", "src/test/resources/");
        final List<String> beforeFileNames = ComparisonReporter.getFilenamesForStep(parameters, "/", "http://url", BEFORE);
        assertThat(beforeFileNames, is(Arrays.asList("url_root_1001_2002_before.png")));
    }

    @Test
    public void shouldFindAfterImagesInDirectory() throws IOException {
        Parameters parameters = new Parameters();
        new JCommander(parameters, "-d", "src/test/resources/");
        final List<String> beforeFileNames = ComparisonReporter.getFilenamesForStep(parameters, "/", "http://url", AFTER);
        assertThat(beforeFileNames, is(Arrays.asList("url_root_1001_2002_after.png","url_root_1001_3003_after.png")));
    }
}