package de.otto.jlineup.web;

import tools.jackson.databind.exc.InvalidDefinitionException;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.ConfigMerger;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.exceptions.ValidationError;
import de.otto.jlineup.service.BeforeRunImportService;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.service.InvalidBeforeRunArchiveException;
import de.otto.jlineup.service.InvalidRunStateException;
import de.otto.jlineup.service.JLineupService;
import de.otto.jlineup.service.RunNotFoundException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;

@RestController
public class JLineupController {

    private final JLineupService jLineupService;

    private final BeforeRunImportService beforeRunImportService;

    private final JLineupWebProperties properties;

    private final AtomicReference<String> currentExampleRun = new AtomicReference<>();

    @Autowired
    public JLineupController(JLineupService jLineupService, BeforeRunImportService beforeRunImportService, JLineupWebProperties properties) {
        this.jLineupService = jLineupService;
        this.beforeRunImportService = beforeRunImportService;
        this.properties = properties;
    }

    @GetMapping("/")
    public String getHello(HttpServletRequest request) {
        return String.format("<p>JLineup is great! Do you want to go to my <a href=\"%s/internal/status\">status page</a>?</p>", request.getContextPath());
    }

    @PostMapping(value = "/runs", consumes = {"application/json", "application/yaml"})
    public ResponseEntity<RunBeforeResponse> runBefore(@RequestBody JobConfig jobConfig, HttpServletRequest request) throws Exception {

        String id = jLineupService.startBeforeRun(prepareJobConfig(jobConfig)).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(request.getContextPath() + "/runs/" + id));

        return ResponseEntity.accepted()
                .headers(headers)
                .body(new RunBeforeResponse(id));
    }

    /**
     * Creates a run from an already completed 'before' run, i.e. the 'before' screenshots are uploaded
     * instead of being taken by this server. The resulting run is in state BEFORE_DONE and behaves exactly
     * like a regular run after its 'before' step.
     *
     * @param before     archive (tar.gz, tar or zip) of the directory containing files.json and the before screenshots
     * @param config     optional job config (JSON or YAML). If omitted, the job-config from the uploaded files.json is used.
     * @param startAfter if true, the 'after' step is started right away
     * @return 201 with the new run id, or 202 if the 'after' step was started
     */
    @PostMapping(value = "/runs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RunBeforeResponse> importBeforeRun(@RequestPart("before") MultipartFile before,
                                                             @RequestPart(value = "config", required = false) MultipartFile config,
                                                             @RequestParam(value = "startAfter", defaultValue = "false") boolean startAfter,
                                                             HttpServletRequest request) throws Exception {

        JobConfig jobConfig = config != null && !config.isEmpty() ? prepareJobConfig(parseConfigPart(config)) : null;

        String id;
        try (InputStream archive = before.getInputStream()) {
            id = beforeRunImportService.importBeforeRun(archive, jobConfig).getId();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(request.getContextPath() + "/runs/" + id));

        if (startAfter) {
            jLineupService.startAfterRun(id);
            return ResponseEntity.accepted().headers(headers).body(new RunBeforeResponse(id));
        }
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(new RunBeforeResponse(id));
    }

    private static JobConfig prepareJobConfig(JobConfig jobConfig) {
        if (jobConfig.mergeConfig != null) {
            JobConfig mainGlobalConfig = JobConfig.copyOfBuilder(jobConfig).withMergeConfig(null).build();
            JobConfig mergeGlobalConfig = jobConfig.mergeConfig;
            jobConfig = ConfigMerger.mergeJobConfigWithMergeConfig(mainGlobalConfig, mergeGlobalConfig);
        }
        return jobConfig.insertDefaults();
    }

    private static JobConfig parseConfigPart(MultipartFile config) throws InvalidBeforeRunArchiveException {
        String contentType = config.getContentType() != null ? config.getContentType().toLowerCase() : "";
        JacksonWrapper.ConfigFormat format = contentType.contains("yaml") || contentType.contains("yml")
                ? JacksonWrapper.ConfigFormat.YAML
                : JacksonWrapper.ConfigFormat.fromFilename(config.getOriginalFilename());
        try (Reader reader = new InputStreamReader(config.getInputStream(), StandardCharsets.UTF_8)) {
            return JacksonWrapper.deserializeConfig(reader, format);
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            throw new InvalidBeforeRunArchiveException("Could not parse config part: " + root.getMessage(), e);
        }
    }

    @GetMapping(value = "/exampleRun")
    public String exampleRun(@RequestParam(value = "url", required = false) String url,
                             @RequestParam(value = "browser", required = false) String browser,
                             HttpServletRequest request) throws Exception {

        String exampleRunId = currentExampleRun.get();
        if (exampleRunId != null) {
            Optional<JLineupRunStatus> run = jLineupService.getRun(exampleRunId);
            if (run.isPresent()) {
                State state = run.get().getState();
                if (!state.isDone()) {
                    if (state == State.BEFORE_DONE) {
                        jLineupService.startAfterRun(run.get().getId());
                        return "Example run entered 'after' step.";

                    } else {
                        return "Example run is currently running. The current state is " + state;
                    }
                }
            }
        }

        if (url == null) {
            url = "https://www.example.com";
        }

        JobConfig.Builder jobConfigBuilder = JobConfig.jobConfigBuilder().withBrowser(properties.getInstalledBrowsers().get(0)).withName("Example run").withUrls(ImmutableMap.of(url, urlConfigBuilder().build()));
        if (browser != null) {
            try {
                Browser.Type type = Browser.Type.forValue(browser);
                jobConfigBuilder.withBrowser(type);
            } catch (Exception e) {
                //Ouch
            }
        }

        currentExampleRun.set(jLineupService.startBeforeRun(jobConfigBuilder.build().insertDefaults()).getId());
        return "Example run started with 'before' step with Browser '" + jobConfigBuilder.build().insertDefaults().browser + "'.";
    }

    @PostMapping("/runs/{runId}")
    public ResponseEntity<Void> runAfter(@PathVariable final String runId, HttpServletRequest request) throws Exception {
        String sanitizedRunId = validateAndSanitizeRunId(runId);
        jLineupService.startAfterRun(sanitizedRunId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(request.getContextPath() + "/runs/" + sanitizedRunId));
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @PostMapping("/runs/{runId}/retry")
    public ResponseEntity<Void> retryAfterRun(@PathVariable final String runId, HttpServletRequest request) throws Exception {
        String sanitizedRunId = validateAndSanitizeRunId(runId);
        jLineupService.retryAfterRun(sanitizedRunId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(request.getContextPath() + "/runs/" + sanitizedRunId));
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @PostMapping("/runs/{runId}/rerun-after")
    public ResponseEntity<RunBeforeResponse> rerunAfterFromRun(@PathVariable final String runId, HttpServletRequest request) throws Exception {
        String sanitizedRunId = validateAndSanitizeRunId(runId);
        JLineupRunStatus newRun = jLineupService.rerunAfterFromRun(sanitizedRunId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(request.getContextPath() + "/runs/" + newRun.getId()));
        return ResponseEntity.accepted()
                .headers(headers)
                .body(new RunBeforeResponse(newRun.getId()));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<JLineupRunStatus>> getRuns() {
        return ResponseEntity.ok(jLineupService.getRunStatus());
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<JLineupRunStatus> getRun(@PathVariable final String runId) throws RunNotFoundException {
        String sanitizedRunId = validateAndSanitizeRunId(runId);
        Optional<JLineupRunStatus> run = jLineupService.getRun(sanitizedRunId);
        return run
                .map(jLineupRunStatus -> new ResponseEntity<>(jLineupRunStatus, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private String validateAndSanitizeRunId(String runId) throws RunNotFoundException {
        try {
            UUID uuid = UUID.fromString(runId);
            return uuid.toString();
        } catch (IllegalArgumentException e) {
            throw new RunNotFoundException(runId);
        }
    }

    @ExceptionHandler(InvalidBeforeRunArchiveException.class)
    public ResponseEntity<String> exceptionHandler(final InvalidBeforeRunArchiveException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> exceptionHandler(final MaxUploadSizeExceededException exception) {
        return new ResponseEntity<>("Upload too large: " + exception.getMessage(), HttpStatus.CONTENT_TOO_LARGE);
    }

    @ExceptionHandler(RunNotFoundException.class)
    public ResponseEntity<String> exceptionHandler(final RunNotFoundException exception) {
        return new ResponseEntity<>(String.format("Run with id '%s' was not found", exception.getId()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidRunStateException.class)
    public ResponseEntity<String> exceptionHandler(final InvalidRunStateException exception) {
        return new ResponseEntity<>(String.format("Run with id '%s' has wrong state. was %s but expected %s",
                exception.getId(), exception.getCurrentState(), exception.getExpectedState()), HttpStatus.PRECONDITION_FAILED);
    }

    @ExceptionHandler(BrowserNotInstalledException.class)
    public ResponseEntity<String> exceptionHandler(final BrowserNotInstalledException exception) {
        // https://httpstatuses.com/422
        return new ResponseEntity<>(String.format("Browser %s is not installed or not configured on server side.", exception.getDesiredBrowser().name()), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(InvalidDefinitionException.class)
    public ResponseEntity<String> exceptionHandler(final InvalidDefinitionException exception) {
        return new ResponseEntity<>(exception.getCause().getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ValidationError.class)
    public ResponseEntity<String> exceptionHandler(final ValidationError exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> exceptionHandler(final IllegalArgumentException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
