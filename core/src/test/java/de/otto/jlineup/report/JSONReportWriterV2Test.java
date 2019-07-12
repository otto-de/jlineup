package de.otto.jlineup.report;

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
                new ScreenshotComparisonResult("url", 1337, 1338, 0d, "before", "after", "differenceImageFileName", 0);
        final Summary globalSummary = new Summary(false, 0d, 0d);
        final Summary localSummary = new Summary(false, 0d, 0d);
        Report report = new Report(globalSummary, Collections.singletonMap("test", new UrlReport(singletonList(screenshotComparisonResult), localSummary)), exampleConfig());

        //language=JSON
        String expectedJSON =
                "{" + lineSeparator() +
                "  \"summary\": {" + lineSeparator() +
                "    \"error\": false," + lineSeparator() +
                "    \"differenceSum\": 0.0," + lineSeparator() +
                "    \"differenceMax\": 0.0" + lineSeparator() +
                "  }," + lineSeparator() +
                "  \"screenshotComparisonsForUrl\": {" + lineSeparator() +
                "    \"test\": {" + lineSeparator() +
                "      \"comparisonResults\": [" + lineSeparator() +
                "        {" + lineSeparator() +
                "          \"url\": \"url\"," + lineSeparator() +
                "          \"width\": 1337," + lineSeparator() +
                "          \"verticalScrollPosition\": 1338," + lineSeparator() +
                "          \"difference\": 0.0," + lineSeparator() +
                "          \"screenshotBeforeFileName\": \"before\"," + lineSeparator() +
                "          \"screenshotAfterFileName\": \"after\"," + lineSeparator() +
                "          \"differenceImageFileName\": \"differenceImageFileName\"," + lineSeparator() +
                "          \"maxSingleColorDifference\": 0" + lineSeparator() +
                "        }" + lineSeparator() +
                "      ]," + lineSeparator() +
                "      \"summary\": {" + lineSeparator() +
                "        \"error\": false," + lineSeparator() +
                "        \"differenceSum\": 0.0," + lineSeparator() +
                "        \"differenceMax\": 0.0" + lineSeparator() +
                "      }" + lineSeparator() +
                "    }" + lineSeparator() +
                "  }," + lineSeparator() +
                "  \"config\": {" + lineSeparator() +
                "    \"urls\": {" + lineSeparator() +
                "      \"http://www.example.com\": {" + lineSeparator() +
                "        \"paths\": [" + lineSeparator() +
                "          \"/\"," + lineSeparator() +
                "          \"someOtherPath\"" + lineSeparator() +
                "        ]," + lineSeparator() +
                "        \"max-diff\": 0.0," + lineSeparator() +
                "        \"cookies\": [" + lineSeparator() +
                "          {" + lineSeparator() +
                "            \"name\": \"exampleCookieName\"," + lineSeparator() +
                "            \"value\": \"exampleValue\"," + lineSeparator() +
                "            \"domain\": \"http://www.example.com\"," + lineSeparator() +
                "            \"path\": \"/\"," + lineSeparator() +
                "            \"expiry\": \"1970-01-01T00:00:01Z\"," + lineSeparator() +
                "            \"secure\": true" + lineSeparator() +
                "          }" + lineSeparator() +
                "        ]," + lineSeparator() +
                "        \"env-mapping\": {" + lineSeparator() +
                "          \"live\": \"www\"" + lineSeparator() +
                "        }," + lineSeparator() +
                "        \"local-storage\": {" + lineSeparator() +
                "          \"exampleLocalStorageKey\": \"value\"" + lineSeparator() +
                "        }," + lineSeparator() +
                "        \"session-storage\": {" + lineSeparator() +
                "          \"exampleSessionStorageKey\": \"value\"" + lineSeparator() +
                "        }," + lineSeparator() +
                "        \"window-widths\": [" + lineSeparator() +
                "          600," + lineSeparator() +
                "          800," + lineSeparator() +
                "          1000" + lineSeparator() +
                "        ]," + lineSeparator() +
                "        \"max-scroll-height\": 100000," + lineSeparator() +
                "        \"wait-after-page-load\": 0.0," + lineSeparator() +
                "        \"wait-after-scroll\": 0.0," + lineSeparator() +
                "        \"wait-for-no-animation-after-scroll\": 0.0," + lineSeparator() +
                "        \"warmup-browser-cache-time\": 0.0," + lineSeparator() +
                "        \"wait-for-fonts-time\": 0.0," + lineSeparator() +
                "        \"javascript\": \"console.log(\\u0027This is JavaScript!\\u0027)\"," + lineSeparator() +
                "        \"hide-images\": false," + lineSeparator() +
                "        \"http-check\": {" + lineSeparator() +
                "          \"enabled\": true," + lineSeparator() +
                "          \"allowed-codes\": [" + lineSeparator() +
                "            200," + lineSeparator() +
                "            202," + lineSeparator() +
                "            204," + lineSeparator() +
                "            205," + lineSeparator() +
                "            206," + lineSeparator() +
                "            301," + lineSeparator() +
                "            302," + lineSeparator() +
                "            303," + lineSeparator() +
                "            304," + lineSeparator() +
                "            307," + lineSeparator() +
                "            308" + lineSeparator() +
                "          ]" + lineSeparator() +
                "        }," + lineSeparator() +
                "        \"max-color-diff-per-pixel\": 1" + lineSeparator() +
                "      }" + lineSeparator() +
                "    }," + lineSeparator() +
                "    \"browser\": \"PhantomJS\"," + lineSeparator() +
                "    \"wait-after-page-load\": 0.0," + lineSeparator() +
                "    \"page-load-timeout\": 120," + lineSeparator() +
                "    \"window-height\": 800," + lineSeparator() +
                "    \"report-format\": 2," + lineSeparator() +
                "    \"screenshot-retries\": 0," + lineSeparator() +
                "    \"threads\": 0," + lineSeparator() +
                "    \"timeout\": 600," + lineSeparator() +
                "    \"debug\": false," + lineSeparator() +
                "    \"log-to-file\": false," + lineSeparator() +
                "    \"check-for-errors-in-log\": true," + lineSeparator() +
                "    \"http-check\": {" + lineSeparator() +
                "      \"enabled\": false" + lineSeparator() +
                "    }" + lineSeparator() +
                "  }" + lineSeparator() +
                "}";

        testee.writeComparisonReportAsJson(report);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fileServiceMock).writeJsonReport(jsonCaptor.capture());

        JSONAssert.assertEquals(expectedJSON, jsonCaptor.getValue(),false);
    }
}