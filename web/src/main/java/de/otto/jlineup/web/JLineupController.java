package de.otto.jlineup.web;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.ConfigMerger;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.exceptions.ValidationError;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.service.InvalidRunStateException;
import de.otto.jlineup.service.JLineupService;
import de.otto.jlineup.service.RunNotFoundException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;

@RestController
public class JLineupController {

    private final JLineupService jLineupService;

    private final JLineupWebProperties properties;

    private final AtomicReference<String> currentExampleRun = new AtomicReference<>();

    @Autowired
    public JLineupController(JLineupService jLineupService, JLineupWebProperties properties) {
        this.jLineupService = jLineupService;
        this.properties = properties;
    }

    @GetMapping("/")
    public String getHello(HttpServletRequest request) {
        return String.format("<p>JLineup is great! Do you want to go to my <a href=\"%s/internal/status\">status page</a>?</p>", request.getContextPath());
    }

    @PostMapping(value = "/runs")
    public ResponseEntity<RunBeforeResponse> runBefore(@RequestBody JobConfig jobConfig, HttpServletRequest request) throws Exception {

        if (jobConfig.mergeConfig != null) {
            JobConfig mainGlobalConfig = JobConfig.copyOfBuilder(jobConfig).withMergeConfig(null).build();
            JobConfig mergeGlobalConfig = jobConfig.mergeConfig;
            jobConfig = ConfigMerger.mergeJobConfigWithMergeConfig(mainGlobalConfig, mergeGlobalConfig);
        }

        String id = jLineupService.startBeforeRun(jobConfig.insertDefaults()).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(request.getContextPath() + "/runs/" + id));

        return ResponseEntity.accepted()
                .headers(headers)
                .body(new RunBeforeResponse(id));
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
