package de.otto.jlineup.report;

import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.file.FileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

import static de.otto.jlineup.config.JobConfig.exampleConfig;
import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;
import static org.mockito.MockitoAnnotations.initMocks;

public class JSONReportWriterV2Test {

    private JSONReportWriter_V2 testee;

    @Mock
    private FileService fileServiceMock;

    @Before
    public void setup() {
        initMocks(this);
        testee = new JSONReportWriter_V2(fileServiceMock);
    }

    @Test
    public void shouldWriteComparisonReportAsJson() throws Exception {

        ScreenshotComparisonResult screenshotComparisonResult =
                new ScreenshotComparisonResult(1887, "url", DeviceConfig.deviceConfig(1337, 1887), 1979, 0d, "before", "after", "differenceImageFileName", 0);
        final Summary globalSummary = new Summary(false, 0d, 0d);
        final Summary localSummary = new Summary(false, 0d, 0d);
        Report report = new Report(globalSummary, Collections.singletonMap("test", new UrlReport(singletonList(screenshotComparisonResult), localSummary)), exampleConfig());

        //language=JSON
        String expectedJSON =
                "{" + lineSeparator() +
                        "  \"summary\" : {" + lineSeparator() +
                        "    \"error\" : false," + lineSeparator() +
                        "    \"differenceSum\" : 0.0," + lineSeparator() +
                        "    \"differenceMax\" : 0.0" + lineSeparator() +
                        "  }," + lineSeparator() +
                        "  \"screenshotComparisonsForUrl\" : {" + lineSeparator() +
                        "    \"test\" : {" + lineSeparator() +
                        "      \"comparisonResults\" : [ {" + lineSeparator() +
                        "        \"contextHash\" : 1887," + lineSeparator() +
                        "        \"url\" : \"url\"," + lineSeparator() +
                        "        \"deviceConfig\" : {" + lineSeparator() +
                        "          \"width\" : 1337," + lineSeparator() +
                        "          \"height\" : 1887," + lineSeparator() +
                        "          \"pixel-ratio\" : 1.0," + lineSeparator() +
                        "          \"device-name\" : \"DESKTOP\"," + lineSeparator() +
                        "          \"user-agent\" : null," + lineSeparator() +
                        "          \"touch\" : false" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"verticalScrollPosition\" : 1979," + lineSeparator() +
                        "        \"difference\" : 0.0," + lineSeparator() +
                        "        \"screenshotBeforeFileName\" : \"before\"," + lineSeparator() +
                        "        \"screenshotAfterFileName\" : \"after\"," + lineSeparator() +
                        "        \"differenceImageFileName\" : \"differenceImageFileName\"," + lineSeparator() +
                        "        \"maxSingleColorDifference\" : 0" + lineSeparator() +
                        "      } ]," + lineSeparator() +
                        "      \"summary\" : {" + lineSeparator() +
                        "        \"error\" : false," + lineSeparator() +
                        "        \"differenceSum\" : 0.0," + lineSeparator() +
                        "        \"differenceMax\" : 0.0" + lineSeparator() +
                        "      }" + lineSeparator() +
                        "    }" + lineSeparator() +
                        "  }," + lineSeparator() +
                        "  \"config\" : {" + lineSeparator() +
                        "    \"urls\" : {" + lineSeparator() +
                        "      \"http://www.example.com\" : {" + lineSeparator() +
                        "        \"paths\" : [ \"/\", \"someOtherPath\" ]," + lineSeparator() +
                        "        \"max-diff\" : 0.0," + lineSeparator() +
                        "        \"cookies\" : [ {" + lineSeparator() +
                        "          \"name\" : \"exampleCookieName\"," + lineSeparator() +
                        "          \"value\" : \"exampleValue\"," + lineSeparator() +
                        "          \"domain\" : \"http://www.example.com\"," + lineSeparator() +
                        "          \"path\" : \"/\"," + lineSeparator() +
                        "          \"expiry\" : \"1970-01-01T00:00:01Z\"," + lineSeparator() +
                        "          \"secure\" : true" + lineSeparator() +
                        "        } ]," + lineSeparator() +
                        "        \"env-mapping\" : {" + lineSeparator() +
                        "          \"live\" : \"www\"" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"local-storage\" : {" + lineSeparator() +
                        "          \"exampleLocalStorageKey\" : \"value\"" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"session-storage\" : {" + lineSeparator() +
                        "          \"exampleSessionStorageKey\" : \"value\"" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"window-widths\" : [ 600, 800, 1000 ]," + lineSeparator() +
                        "        \"devices\" : null," + lineSeparator() +
                        "        \"max-scroll-height\" : 100000," + lineSeparator() +
                        "        \"wait-after-page-load\" : 0.0," + lineSeparator() +
                        "        \"wait-after-scroll\" : 0.0," + lineSeparator() +
                        "        \"wait-for-no-animation-after-scroll\" : 0.0," + lineSeparator() +
                        "        \"warmup-browser-cache-time\" : 0.0," + lineSeparator() +
                        "        \"wait-for-fonts-time\" : 0.0," + lineSeparator() +
                        "        \"hide-images\" : false," + lineSeparator() +
                        "        \"http-check\" : {" + lineSeparator() +
                        "          \"enabled\" : true," + lineSeparator() +
                        "          \"allowed-codes\" : [ 200, 202, 204, 205, 206, 301, 302, 303, 304, 307, 308 ]" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"max-color-diff-per-pixel\" : 1," + lineSeparator() +
                        "        \"javascript\" : \"console.log('This is JavaScript!')\"" + lineSeparator() +
                        "      }" + lineSeparator() +
                        "    }," + lineSeparator() +
                        "    \"browser\" : \"PhantomJS\"," + lineSeparator() +
                        "    \"name\" : null," + lineSeparator() +
                        "    \"page-load-timeout\" : 120," + lineSeparator() +
                        "    \"window-height\" : 800," + lineSeparator() +
                        "    \"report-format\" : 2," + lineSeparator() +
                        "    \"screenshot-retries\" : 0," + lineSeparator() +
                        "    \"threads\" : 0," + lineSeparator() +
                        "    \"debug\" : false," + lineSeparator() +
                        "    \"log-to-file\" : false," + lineSeparator() +
                        "    \"check-for-errors-in-log\" : true," + lineSeparator() +
                        "    \"http-check\" : {" + lineSeparator() +
                        "      \"enabled\" : false," + lineSeparator() +
                        "      \"allowed-codes\" : null" + lineSeparator() +
                        "    }," + lineSeparator() +
                        "    \"wait-after-page-load\" : 0.0," + lineSeparator() +
                        "    \"timeout\" : 600" + lineSeparator() +
                        "  }," + lineSeparator() +
                        "  \"flatResultList\" : [ {" + lineSeparator() +
                        "    \"contextHash\" : 1887," + lineSeparator() +
                        "    \"url\" : \"url\"," + lineSeparator() +
                        "    \"deviceConfig\" : {" + lineSeparator() +
                        "      \"width\" : 1337," + lineSeparator() +
                        "      \"height\" : 1887," + lineSeparator() +
                        "      \"pixel-ratio\" : 1.0," + lineSeparator() +
                        "      \"device-name\" : \"DESKTOP\"," + lineSeparator() +
                        "      \"user-agent\" : null," + lineSeparator() +
                        "      \"touch\" : false" + lineSeparator() +
                        "    }," + lineSeparator() +
                        "    \"verticalScrollPosition\" : 1979," + lineSeparator() +
                        "    \"difference\" : 0.0," + lineSeparator() +
                        "    \"screenshotBeforeFileName\" : \"before\"," + lineSeparator() +
                        "    \"screenshotAfterFileName\" : \"after\"," + lineSeparator() +
                        "    \"differenceImageFileName\" : \"differenceImageFileName\"," + lineSeparator() +
                        "    \"maxSingleColorDifference\" : 0" + lineSeparator() +
                        "  } ]" + lineSeparator() +
                        "}";

        testee.writeComparisonReportAsJson(report);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fileServiceMock).writeJsonReport(jsonCaptor.capture());

        JSONAssert.assertEquals(expectedJSON, jsonCaptor.getValue(),false);
    }
}