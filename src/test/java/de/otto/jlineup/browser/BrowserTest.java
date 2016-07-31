package de.otto.jlineup.browser;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BrowserTest {

    @Test
    public void shouldGenerateFilename() throws Exception {

        String outputString = Browser.generateFileName("https://www.otto.de/", "multimedia", 1000, false);

        assertThat(outputString, is("www_otto_de_multimedia_1000_after.png"));

    }

    @Test
    public void shouldConvertRoot() throws Exception {

        String outputString = Browser.generateFileName("https://www.otto.de/", "/", 1000, false);

        assertThat(outputString, is("www_otto_de_root_1000_after.png"));

    }
}