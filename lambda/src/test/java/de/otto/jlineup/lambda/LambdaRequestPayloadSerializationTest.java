package de.otto.jlineup.lambda;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Tests serialization and deserialization of {@link LambdaRequestPayload} with
 * {@link JacksonWrapper#jsonMapperForLambdaHandler()} – the same mapper used inside
 * {@link LambdaBrowser} and {@link JLineupHandler}.
 *
 * <p>Key invariants verified:
 * <ul>
 *   <li>{@code step} and {@code urlKey} survive the round-trip as top-level fields on
 *       the payload record.</li>
 *   <li>The serialized JSON does <em>not</em> contain a {@code step}, {@code urlKey} or
 *       {@code urlConfig} field <em>inside</em> the {@code screenshotContext} object –
 *       those fields carry {@code @JsonIgnore} on {@link ScreenshotContext}.</li>
 *   <li>After deserialization the handler can reconstruct the full context via
 *       {@code payload.jobConfig().urls.get(payload.urlKey())} (top-level urlKey, not
 *       {@code screenshotContext.urlKey} which is {@code null} after deserialization).</li>
 *   <li>A full {@link JobConfig} with URLs and device config round-trips without data loss.</li>
 * </ul>
 */
class LambdaRequestPayloadSerializationTest {
    private static final JsonMapper HANDLER_JSON_MAPPER = JacksonWrapper.jsonMapperForLambdaHandler();
    private static final String URL_KEY = "https://www.example.com";
    private static final String RUN_ID  = "test-run-id-12345";

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private JobConfig buildJobConfig() {
        return JobConfig.jobConfigBuilder()
                .withUrls(Map.of(URL_KEY, UrlConfig.urlConfigBuilder()
                        .withStringPaths(List.of("/", "/about"))
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder()
                                .withWidth(1280).withHeight(800).build()))
                        .build()))
                .withBrowser(Browser.Type.CHROME_HEADLESS)
                .withGlobalTimeout(120)
                .build();
    }

    private ScreenshotContext buildScreenshotContext(BrowserStep step) {
        return ScreenshotContext.of(
                URL_KEY,
                "/",
                DeviceConfig.deviceConfigBuilder().withWidth(1280).withHeight(800).build(),
                step,
                UrlConfig.urlConfigBuilder()
                        .withStringPaths(List.of("/"))
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder()
                                .withWidth(1280).withHeight(800).build()))
                        .build(),
                Collections.emptyList(),
                URL_KEY);
    }

    // -------------------------------------------------------------------------
    // Serialization structure
    // -------------------------------------------------------------------------
    @Test
    void stepAndUrlKeyAreTopLevelFieldsInJson_andNotInsideScreenshotContext() throws Exception {
        ScreenshotContext ctx = buildScreenshotContext(BrowserStep.before);
        LambdaRequestPayload payload =
                new LambdaRequestPayload(RUN_ID, buildJobConfig(), ctx, RunStep.before, URL_KEY);
        String json = HANDLER_JSON_MAPPER.writeValueAsString(payload);
        ObjectNode root = (ObjectNode) HANDLER_JSON_MAPPER.readTree(json);
        // step, urlKey and runId must be top-level fields on the payload
        assertTrue(root.has("step"),   "top-level 'step' field must be present");
        assertTrue(root.has("urlKey"), "top-level 'urlKey' field must be present");
        assertTrue(root.has("runId"),  "top-level 'runId' field must be present");
        // step, urlKey and urlConfig must NOT appear inside screenshotContext –
        // they carry @JsonIgnore, so the handler reconstructs them from the
        // top-level payload fields instead (see JLineupHandler.handleRequest).
        ObjectNode screenshotContextNode = (ObjectNode) root.get("screenshotContext");
        assertNotNull(screenshotContextNode, "screenshotContext node must be present");
        assertFalse(screenshotContextNode.has("step"),
                "'step' must NOT be inside screenshotContext (it is @JsonIgnore)");
        assertFalse(screenshotContextNode.has("urlKey"),
                "'urlKey' must NOT be inside screenshotContext (it is @JsonIgnore)");
        assertFalse(screenshotContextNode.has("urlConfig"),
                "'urlConfig' must NOT be inside screenshotContext (it is @JsonIgnore)");
        // The non-ignored fields must be present
        assertTrue(screenshotContextNode.has("url"),          "'url' must be in screenshotContext");
        assertTrue(screenshotContextNode.has("urlSubPath"),   "'urlSubPath' must be in screenshotContext");
        assertTrue(screenshotContextNode.has("deviceConfig"), "'deviceConfig' must be in screenshotContext");
    }

    // -------------------------------------------------------------------------
    // Round-trip: before step
    // -------------------------------------------------------------------------
    @Test
    void beforePayloadRoundTrip() throws Exception {
        ScreenshotContext ctx = buildScreenshotContext(BrowserStep.before);
        LambdaRequestPayload original =
                new LambdaRequestPayload(RUN_ID, buildJobConfig(), ctx, RunStep.before, URL_KEY);
        String json = HANDLER_JSON_MAPPER.writeValueAsString(original);
        LambdaRequestPayload deserialized = HANDLER_JSON_MAPPER.readValue(json, LambdaRequestPayload.class);
        assertAll("before payload round-trip",
                () -> assertEquals(RUN_ID,        deserialized.runId()),
                () -> assertEquals(RunStep.before, deserialized.step()),
                // urlKey survives as top-level field
                () -> assertEquals(URL_KEY,        deserialized.urlKey()),
                // screenshotContext fields that are NOT @JsonIgnore round-trip correctly
                () -> assertEquals(URL_KEY,        deserialized.screenshotContext().url),
                () -> assertEquals("/",            deserialized.screenshotContext().urlSubPath),
                // @JsonIgnore fields are null after deserialization – JLineupHandler
                // reconstructs them via event.urlKey() / event.step()
                () -> assertNull(deserialized.screenshotContext().urlKey,
                        "screenshotContext.urlKey is @JsonIgnore and must be null after deserialization"),
                () -> assertNotNull(deserialized.jobConfig()),
                () -> assertTrue(deserialized.jobConfig().urls.containsKey(URL_KEY))
        );
    }

    // -------------------------------------------------------------------------
    // Round-trip: after step
    // -------------------------------------------------------------------------
    @Test
    void afterPayloadRoundTrip() throws Exception {
        ScreenshotContext ctx = buildScreenshotContext(BrowserStep.after);
        LambdaRequestPayload original =
                new LambdaRequestPayload(RUN_ID, buildJobConfig(), ctx, RunStep.after_only, URL_KEY);
        String json = HANDLER_JSON_MAPPER.writeValueAsString(original);
        LambdaRequestPayload deserialized = HANDLER_JSON_MAPPER.readValue(json, LambdaRequestPayload.class);
        assertEquals(RunStep.after_only, deserialized.step());
        assertEquals(URL_KEY,            deserialized.urlKey());
        assertEquals(URL_KEY,            deserialized.screenshotContext().url);
    }

    // -------------------------------------------------------------------------
    // Handler urlKey lookup simulation
    // -------------------------------------------------------------------------
    @Test
    void urlKeyCanBeUsedToLookUpUrlConfigAfterDeserialization() throws Exception {
        LambdaRequestPayload payload =
                new LambdaRequestPayload(RUN_ID, buildJobConfig(),
                        buildScreenshotContext(BrowserStep.before), RunStep.before, URL_KEY);
        String json = HANDLER_JSON_MAPPER.writeValueAsString(payload);
        LambdaRequestPayload deserialized = HANDLER_JSON_MAPPER.readValue(json, LambdaRequestPayload.class);
        // Simulates what the fixed JLineupHandler does:
        // use event.urlKey() (top-level) – NOT event.screenshotContext().urlKey (that is null)
        UrlConfig lookedUpConfig =
                deserialized.jobConfig().urls.get(deserialized.urlKey());
        assertNotNull(lookedUpConfig,
                "urlConfig must be retrievable from jobConfig.urls using the top-level urlKey " +
                "– this is exactly what JLineupHandler.handleRequest performs");
    }

    // -------------------------------------------------------------------------
    // Round-trip with contexts built by BrowserUtils
    // -------------------------------------------------------------------------
    @Test
    void contextsBuiltByBrowserUtilsRoundTripCorrectly() throws Exception {
        // exampleConfigBuilder() includes proper device configs and avoids NPE
        // in BrowserUtils.buildScreenshotContextListFromConfigAndState
        JobConfig jobConfig = JobConfig.exampleConfigBuilder()
                .withBrowser(Browser.Type.CHROME_HEADLESS)
                .build();
        RunStepConfig runStepConfig = RunStepConfig.runStepConfigBuilder()
                .withStep(RunStep.before)
                .build();
        List<ScreenshotContext> contexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig);
        assertFalse(contexts.isEmpty(), "BrowserUtils must produce at least one context");
        for (ScreenshotContext ctx : contexts) {
            LambdaRequestPayload payload =
                    new LambdaRequestPayload(RUN_ID, jobConfig, ctx, RunStep.before, ctx.urlKey);
            String json = HANDLER_JSON_MAPPER.writeValueAsString(payload);
            LambdaRequestPayload deserialized = HANDLER_JSON_MAPPER.readValue(json, LambdaRequestPayload.class);
            assertAll("round-trip for context " + ctx.url + ctx.urlSubPath,
                    () -> assertEquals(ctx.url,        deserialized.screenshotContext().url),
                    () -> assertEquals(ctx.urlSubPath,  deserialized.screenshotContext().urlSubPath),
                    // top-level urlKey round-trips correctly
                    () -> assertEquals(ctx.urlKey,     deserialized.urlKey()),
                    () -> assertEquals(RunStep.before,  deserialized.step()),
                    // handler lookup works with top-level urlKey
                    () -> assertNotNull(
                            deserialized.jobConfig().urls.get(deserialized.urlKey()),
                            "urlKey must resolve to a UrlConfig in the deserialized jobConfig")
            );
        }
    }

    @Test
    void contextHashMatchesAfterRoundTrip() throws Exception {
        JobConfig jobConfig = JobConfig.jobConfigBuilder()
                .withUrls(Map.of(URL_KEY, UrlConfig.urlConfigBuilder()
                        .withStringPaths(List.of("/"))
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder()
                                .withWidth(800).withHeight(800).build()))
                        .build()))
                .withBrowser(Browser.Type.CHROME_HEADLESS)
                .build();
        RunStepConfig rsc = RunStepConfig.runStepConfigBuilder()
                .withStep(RunStep.before)
                .build();
        List<ScreenshotContext> contexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(rsc, jobConfig);
        for (ScreenshotContext ctx : contexts) {
            LambdaRequestPayload payload =
                    new LambdaRequestPayload(RUN_ID, jobConfig, ctx, RunStep.before, ctx.urlKey);
            String json = HANDLER_JSON_MAPPER.writeValueAsString(payload);
            System.out.println("Serialized JSON: " + json);
            LambdaRequestPayload deserialized = HANDLER_JSON_MAPPER.readValue(json, LambdaRequestPayload.class);
            // Reconstruct exactly like JLineupHandler does
            ScreenshotContext reconstructed = ScreenshotContext.copyOfBuilder(deserialized.screenshotContext())
                    .withStep(deserialized.step().toBrowserStep())
                    .withUrlKey(deserialized.urlKey())
                    .withUrlConfig(deserialized.jobConfig().urls.get(deserialized.urlKey()))
                    .build();
            System.out.println("Original hash: " + ctx.contextHash() + " deviceConfig.hash=" + ctx.deviceConfig.hashCode() + " urlKey=" + ctx.urlKey + " urlSubPath=" + ctx.urlSubPath + " browserType=" + ctx.browserType);
            System.out.println("Reconstructed hash: " + reconstructed.contextHash() + " deviceConfig.hash=" + reconstructed.deviceConfig.hashCode() + " urlKey=" + reconstructed.urlKey + " urlSubPath=" + reconstructed.urlSubPath + " browserType=" + reconstructed.browserType);
            assertEquals(ctx.contextHash(), reconstructed.contextHash(),
                    "contextHash must match after serialization round-trip");
        }
    }

    @Test
    void deviceConfigRoundTrips() throws Exception {
        DeviceConfig device = DeviceConfig.deviceConfigBuilder()
                .withWidth(375).withHeight(812).withDeviceName("iPhone 14").build();
        ScreenshotContext ctx = ScreenshotContext.of(
                URL_KEY, "/", device, BrowserStep.before,
                UrlConfig.urlConfigBuilder()
                        .withDevices(List.of(device))
                        .build(),
                Collections.emptyList(), URL_KEY);
        LambdaRequestPayload payload =
                new LambdaRequestPayload(RUN_ID, buildJobConfig(), ctx, RunStep.before, URL_KEY);
        String json = HANDLER_JSON_MAPPER.writeValueAsString(payload);
        LambdaRequestPayload deserialized = HANDLER_JSON_MAPPER.readValue(json, LambdaRequestPayload.class);
        DeviceConfig deserializedDevice = deserialized.screenshotContext().deviceConfig;
        assertAll("device config round-trip",
                () -> assertEquals(375,         deserializedDevice.width),
                () -> assertEquals(812,         deserializedDevice.height),
                () -> assertEquals("iPhone 14", deserializedDevice.deviceName)
        );
    }

    @Test
    void jobConfigUrlsRoundTrip() throws Exception {
        JobConfig jobConfig = buildJobConfig();
        LambdaRequestPayload payload =
                new LambdaRequestPayload(RUN_ID, jobConfig,
                        buildScreenshotContext(BrowserStep.before), RunStep.before, URL_KEY);
        String json = HANDLER_JSON_MAPPER.writeValueAsString(payload);
        LambdaRequestPayload deserialized = HANDLER_JSON_MAPPER.readValue(json, LambdaRequestPayload.class);
        JobConfig deserializedConfig = deserialized.jobConfig();
        assertAll("jobConfig round-trip",
                () -> assertNotNull(deserializedConfig),
                () -> assertTrue(deserializedConfig.urls.containsKey(URL_KEY)),
                () -> assertEquals(
                        jobConfig.urls.get(URL_KEY).paths,
                        deserializedConfig.urls.get(URL_KEY).paths)
        );
    }
}
