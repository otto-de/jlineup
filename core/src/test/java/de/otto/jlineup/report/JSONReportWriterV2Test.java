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
        final Summary globalSummary = new Summary(false, 0d, 0d, 0);
        final Summary localSummary = new Summary(false, 0d, 0d, 0);
        Report report = new Report(globalSummary, Collections.singletonMap("test", new UrlReport(singletonList(screenshotComparisonResult), localSummary)), exampleConfig());

        //language=JSON
        String expectedJSON =
                "{" + lineSeparator() +
                        "  \"summary\" : {" + lineSeparator() +
                        "    \"error\" : false," + lineSeparator() +
                        "    \"difference-sum\" : 0.0," + lineSeparator() +
                        "    \"difference-max\" : 0.0" + lineSeparator() +
                        "  }," + lineSeparator() +
                        "  \"screenshot-comparisons-for-url\" : {" + lineSeparator() +
                        "    \"test\" : {" + lineSeparator() +
                        "      \"comparison-results\" : [ {" + lineSeparator() +
                        "        \"context-hash\" : 1887," + lineSeparator() +
                        "        \"url\" : \"url\"," + lineSeparator() +
                        "        \"device-config\" : {" + lineSeparator() +
                        "          \"width\" : 1337," + lineSeparator() +
                        "          \"height\" : 1887," + lineSeparator() +
                        "          \"pixel-ratio\" : 1.0," + lineSeparator() +
                        "          \"device-name\" : \"DESKTOP\"," + lineSeparator() +
                        "          \"touch\" : false" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"vertical-scroll-position\" : 1979," + lineSeparator() +
                        "        \"difference\" : 0.0," + lineSeparator() +
                        "        \"screenshot-before-file-name\" : \"before\"," + lineSeparator() +
                        "        \"screenshot-after-file-name\" : \"after\"," + lineSeparator() +
                        "        \"difference-image-file-name\" : \"differenceImageFileName\"" + lineSeparator() +
                        "      } ]," + lineSeparator() +
                        "      \"summary\" : {" + lineSeparator() +
                        "        \"error\" : false," + lineSeparator() +
                        "        \"difference-sum\" : 0.0," + lineSeparator() +
                        "        \"difference-max\" : 0.0" + lineSeparator() +
                        "      }" + lineSeparator() +
                        "    }" + lineSeparator() +
                        "  }," + lineSeparator() +
                        "  \"config\" : {" + lineSeparator() +
                        "    \"urls\" : {" + lineSeparator() +
                        "      \"https://www.example.com\" : {" + lineSeparator() +
                        "        \"paths\" : [ \"/\" ]," + lineSeparator() +
                        "        \"max-diff\" : 0.0," + lineSeparator() +
                        "        \"cookies\" : [ {" + lineSeparator() +
                        "          \"name\" : \"exampleCookieName\"," + lineSeparator() +
                        "          \"value\" : \"*****\"," + lineSeparator() +
                        "          \"domain\" : \"www.example.com\"," + lineSeparator() +
                        "          \"path\" : \"/\"," + lineSeparator() +
                        "          \"expiry\" : \"1970-01-01T00:00:01Z\"," + lineSeparator() +
                        "          \"secure\" : false" + lineSeparator() +
                        "        } ]," + lineSeparator() +
                        "        \"env-mapping\" : {" + lineSeparator() +
                        "          \"live\" : \"www\"" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"local-storage\" : {" + lineSeparator() +
                        "          \"exampleLocalStorageKey\" : \"*****\"" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"session-storage\" : {" + lineSeparator() +
                        "          \"exampleSessionStorageKey\" : \"*****\"" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"max-scroll-height\" : 100000," + lineSeparator() +
                        "        \"wait-after-page-load\" : 0.0," + lineSeparator() +
                        "        \"wait-after-scroll\" : 0.0," + lineSeparator() +
                        "        \"http-check\" : {" + lineSeparator() +
                        "          \"enabled\" : true," + lineSeparator() +
                        "          \"allowed-codes\" : [ 200, 202, 204, 205, 206, 301, 302, 303, 304, 307, 308 ]" + lineSeparator() +
                        "        }," + lineSeparator() +
                        "        \"javascript\" : \"console.log('This is JavaScript!')\"" + lineSeparator() +
                        "      }" + lineSeparator() +
                        "    }," + lineSeparator() +
                        "    \"browser\" : \"Chrome-Headless\"," + lineSeparator() +
                        "    \"page-load-timeout\" : 120," + lineSeparator() +
                        "    \"debug\" : false," + lineSeparator() +
                        "    \"check-for-errors-in-log\" : true," + lineSeparator() +
                        "    \"wait-after-page-load\" : 0.0," + lineSeparator() +
                        "    \"timeout\" : 1800" + lineSeparator() +
                        "  }," + lineSeparator() +
                        "  \"flat-result-list\" : [ {" + lineSeparator() +
                        "    \"context-hash\" : 1887," + lineSeparator() +
                        "    \"url\" : \"url\"," + lineSeparator() +
                        "    \"device-config\" : {" + lineSeparator() +
                        "      \"width\" : 1337," + lineSeparator() +
                        "      \"height\" : 1887," + lineSeparator() +
                        "      \"pixel-ratio\" : 1.0," + lineSeparator() +
                        "      \"device-name\" : \"DESKTOP\"," + lineSeparator() +
                        "      \"touch\" : false" + lineSeparator() +
                        "    }," + lineSeparator() +
                        "    \"vertical-scroll-position\" : 1979," + lineSeparator() +
                        "    \"difference\" : 0.0," + lineSeparator() +
                        "    \"screenshot-before-file-name\" : \"before\"," + lineSeparator() +
                        "    \"screenshot-after-file-name\" : \"after\"," + lineSeparator() +
                        "    \"difference-image-file-name\" : \"differenceImageFileName\"," + lineSeparator() +
                        "    \"accepted-different-pixels\" : 0" + lineSeparator() +
                        "  } ]" + lineSeparator() +
                        "}";

        testee.writeComparisonReportAsJson(report);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fileServiceMock).writeJsonReport(jsonCaptor.capture());

        JSONAssert.assertEquals(expectedJSON, jsonCaptor.getValue(),false);
    }
}