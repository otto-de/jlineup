package de.otto.jlineup.report;

import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Map;
import java.util.Set;

import static de.otto.jlineup.config.JobConfig.exampleConfig;
import static java.util.Collections.singletonList;

@ExtendWith(MockitoExtension.class)
class JSONReportWriterTest {

    private JSONReportWriter testee;

    @Mock
    private FileService fileServiceMock;

    @BeforeEach
    void setup() {
        testee = new JSONReportWriter(fileServiceMock);
    }

    @Test
    void shouldWriteComparisonReportAsJson() throws Exception {

        ScreenshotComparisonResult screenshotComparisonResult =
                new ScreenshotComparisonResult("1887", "url", DeviceConfig.deviceConfig(1337, 1887), 1979, 0d, 0d, "before", "after", "differenceImageFileName", 0);
        final Summary globalSummary = new Summary(false, 0d, 0d, 0);
        final Summary localSummary = new Summary(false, 0d, 0d, 0);
        ContextReport contextReport = new ContextReport("1887", ScreenshotContext.of("url", null, DeviceConfig.deviceConfigBuilder().build(), BrowserStep.before, UrlConfig.urlConfigBuilder().build()), localSummary, singletonList(screenshotComparisonResult));
        UrlReport urlReport = new UrlReport("test", "url", localSummary, singletonList(contextReport));
        Report report = new Report(globalSummary, exampleConfig(), singletonList(urlReport), Map.of(BrowserStep.before, Set.of("SomeBrowser")));

        //language=JSON
        String expectedJSON = """
                {
                  "summary" : {
                    "error" : false,
                    "difference-sum" : 0.0,
                    "difference-max" : 0.0,
                    "accepted-different-pixels" : 0
                  },
                  "config" : {
                    "urls" : {
                      "https://www.example.com" : {
                        "url" : "https://www.example.com",
                        "paths" : [ "/" ],
                        "max-diff" : 0.0,
                        "cookies" : [ {
                          "name" : "exampleCookieName",
                          "value" : "*****",
                          "domain" : "www.example.com",
                          "path" : "/",
                          "expiry" : "1970-01-01T00:00:01Z",
                          "secure" : false,
                          "show-in-report" : false
                        } ],
                        "env-mapping" : {
                          "live" : "www"
                        },
                        "local-storage" : {
                          "exampleLocalStorageKey" : "*****"
                        },
                        "session-storage" : {
                          "exampleSessionStorageKey" : "*****"
                        },
                        "devices" : [ {
                          "width" : 850,
                          "height" : 600,
                          "pixel-ratio" : 1.0,
                          "device-name" : "DESKTOP",
                          "touch" : false
                        }, {
                          "width" : 1000,
                          "height" : 850,
                          "pixel-ratio" : 1.0,
                          "device-name" : "DESKTOP",
                          "touch" : false
                        }, {
                          "width" : 1200,
                          "height" : 1000,
                          "pixel-ratio" : 1.0,
                          "device-name" : "DESKTOP",
                          "touch" : false
                        } ],
                        "max-scroll-height" : 100000,
                        "wait-after-page-load" : 0.0,
                        "wait-after-scroll" : 0.0,
                        "scroll-distance-factor" : 1.0,
                        "http-check" : {
                          "enabled" : true,
                          "allowed-codes" : [ 200, 202, 204, 205, 206, 301, 302, 303, 304, 307, 308 ]
                        },
                        "remove-selectors" : [ "#removeNodeWithThisId", ".removeNodesWithThisClass" ],
                        "wait-for-selectors" : [ "h1" ],
                        "wait-for-selectors-timeout" : 10.0,
                        "fail-if-selectors-not-found" : false,
                        "ignore-anti-aliasing" : false,
                        "max-anti-alias-color-distance" : 2.3,
                        "strict-color-comparison" : false,
                        "max-color-distance" : 2.3,
                        "javascript" : "console.log('This is JavaScript!')"
                      }
                    },
                    "browser" : "Chrome-Headless",
                    "name" : "Example",
                    "message" : "This is an example message, which will be shown in the report.",
                    "page-load-timeout" : 120,
                    "debug" : false,
                    "check-for-errors-in-log" : true,
                    "wait-after-page-load" : 0.0,
                    "timeout" : 1800
                  },
                  "url-reports" : [ {
                    "url-key" : "test",
                    "url" : "url",
                    "summary" : {
                      "error" : false,
                      "difference-sum" : 0.0,
                      "difference-max" : 0.0,
                      "accepted-different-pixels" : 0
                    },
                    "context-reports" : [ {
                      "context-hash" : "1887",
                      "screenshot-context" : {
                        "url" : "url",
                        "url-sub-path" : null,
                        "device-config" : {
                          "width" : 800,
                          "height" : 800,
                          "pixel-ratio" : 1.0,
                          "device-name" : "DESKTOP",
                          "touch" : false
                        },
                        "cookies" : null
                      },
                      "summary" : {
                        "error" : false,
                        "difference-sum" : 0.0,
                        "difference-max" : 0.0,
                        "accepted-different-pixels" : 0
                      },
                      "results" : [ {
                        "context-hash" : "1887",
                        "url" : "url",
                        "device-config" : {
                          "width" : 1337,
                          "height" : 1887,
                          "pixel-ratio" : 1.0,
                          "device-name" : "DESKTOP",
                          "touch" : false
                        },
                        "vertical-scroll-position" : 1979,
                        "difference" : 0.0,
                        "max-detected-color-difference" : 0.0,
                        "screenshot-before-file-name" : "before",
                        "screenshot-after-file-name" : "after",
                        "difference-image-file-name" : "differenceImageFileName",
                        "accepted-different-pixels" : 0
                      } ],
                      "device-info" : "800x800",
                      "success" : true,
                      "flaky-accepted" : false,
                      "shown-cookies" : null,
                      "shortened-url" : "url",
                      "shown-cookies-string" : null,
                      "width" : 800,
                      "url" : "url"
                    } ]
                  } ],
                  "browsers" : {
                    "before" : [ "SomeBrowser" ]
                  }
                }
                """;


        testee.writeComparisonReportAsJson(report);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fileServiceMock).writeJsonReport(jsonCaptor.capture());

        JSONAssert.assertEquals(expectedJSON, jsonCaptor.getValue(), false);
    }
}
