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
                "{\n" +
                "  \"summary\": {\n" +
                "    \"error\": false,\n" +
                "    \"differenceSum\": 0.0,\n" +
                "    \"differenceMax\": 0.0\n" +
                "  },\n" +
                "  \"screenshotComparisonsForUrl\": {\n" +
                "    \"test\": {\n" +
                "      \"comparisonResults\": [\n" +
                "        {\n" +
                "          \"url\": \"url\",\n" +
                "          \"width\": 1337,\n" +
                "          \"verticalScrollPosition\": 1338,\n" +
                "          \"difference\": 0.0,\n" +
                "          \"screenshotBeforeFileName\": \"before\",\n" +
                "          \"screenshotAfterFileName\": \"after\",\n" +
                "          \"differenceImageFileName\": \"differenceImageFileName\",\n" +
                "          \"maxSingleColorDifference\": 0\n" +
                "        }\n" +
                "      ],\n" +
                "      \"summary\": {\n" +
                "        \"error\": false,\n" +
                "        \"differenceSum\": 0.0,\n" +
                "        \"differenceMax\": 0.0\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"config\": {\n" +
                "    \"urls\": {\n" +
                "      \"http://www.example.com\": {\n" +
                "        \"paths\": [\n" +
                "          \"/\",\n" +
                "          \"someOtherPath\"\n" +
                "        ],\n" +
                "        \"max-diff\": 0.0,\n" +
                "        \"cookies\": [\n" +
                "          {\n" +
                "            \"name\": \"exampleCookieName\",\n" +
                "            \"value\": \"exampleValue\",\n" +
                "            \"domain\": \"http://www.example.com\",\n" +
                "            \"path\": \"/\",\n" +
                "            \"expiry\": \"1970-01-01T00:00:01Z\",\n" +
                "            \"secure\": true\n" +
                "          }\n" +
                "        ],\n" +
                "        \"env-mapping\": {\n" +
                "          \"live\": \"www\"\n" +
                "        },\n" +
                "        \"local-storage\": {\n" +
                "          \"exampleLocalStorageKey\": \"value\"\n" +
                "        },\n" +
                "        \"session-storage\": {\n" +
                "          \"exampleSessionStorageKey\": \"value\"\n" +
                "        },\n" +
                "        \"window-widths\": [\n" +
                "          600,\n" +
                "          800,\n" +
                "          1000\n" +
                "        ],\n" +
                "        \"max-scroll-height\": 100000,\n" +
                "        \"wait-after-page-load\": 0.0,\n" +
                "        \"wait-after-scroll\": 0.0,\n" +
                "        \"wait-for-no-animation-after-scroll\": 0.0,\n" +
                "        \"warmup-browser-cache-time\": 0.0,\n" +
                "        \"wait-for-fonts-time\": 0.0,\n" +
                "        \"javascript\": \"console.log(\\u0027This is JavaScript!\\u0027)\",\n" +
                "        \"hide-images\": false,\n" +
                "        \"http-check\": {\n" +
                "          \"enabled\": true,\n" +
                "          \"allowed-codes\": [\n" +
                "            200,\n" +
                "            202,\n" +
                "            204,\n" +
                "            205,\n" +
                "            206,\n" +
                "            301,\n" +
                "            302,\n" +
                "            303,\n" +
                "            304,\n" +
                "            307,\n" +
                "            308\n" +
                "          ]\n" +
                "        },\n" +
                "        \"max-color-diff-per-pixel\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    \"browser\": \"PhantomJS\",\n" +
                "    \"wait-after-page-load\": 0.0,\n" +
                "    \"page-load-timeout\": 120,\n" +
                "    \"window-height\": 800,\n" +
                "    \"report-format\": 2,\n" +
                "    \"screenshot-retries\": 0,\n" +
                "    \"threads\": 0,\n" +
                "    \"timeout\": 600,\n" +
                "    \"debug\": false,\n" +
                "    \"log-to-file\": false,\n" +
                "    \"check-for-errors-in-log\": true,\n" +
                "    \"http-check\": {\n" +
                "      \"enabled\": false\n" +
                "    }\n" +
                "  }\n" +
                "}";

        testee.writeComparisonReportAsJson(report);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fileServiceMock).writeJsonReport(jsonCaptor.capture());

        JSONAssert.assertEquals(expectedJSON, jsonCaptor.getValue(),false);
    }
}