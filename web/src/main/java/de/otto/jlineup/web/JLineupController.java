package de.otto.jlineup.web;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.exceptions.ValidationError;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.service.InvalidRunStateException;
import de.otto.jlineup.service.JLineupService;
import de.otto.jlineup.service.RunNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;

@RestController
public class JLineupController {

    private final JLineupService jLineupService;

    private AtomicReference<String> currentExampleRun = new AtomicReference<>();

    @Autowired
    public JLineupController(JLineupService jLineupService) {
        this.jLineupService = jLineupService;
    }

    @GetMapping("/")
    public String getHello(HttpServletRequest request) {
        return String.format("<p>JLineup is great! Do you want to go to my <a href=\"%s/internal/status\">status page</a>?</p>", request.getContextPath());
    }

    @PostMapping(value = "/runs")
    public ResponseEntity<RunBeforeResponse> runBefore(@RequestBody JobConfig jobConfig, HttpServletRequest request) throws Exception {
        String id = jLineupService.startBeforeRun(jobConfig).getId();

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

        JobConfig.Builder jobConfigBuilder = JobConfig.jobConfigBuilder().withName("Example run").withUrls(ImmutableMap.of(url, urlConfigBuilder().build()));
        if (browser != null) {
            try {
                Browser.Type type = Browser.Type.forValue(browser);
                jobConfigBuilder.withBrowser(type);
            } catch (Exception e) {
                //Ouch
            }
        }

        currentExampleRun.set(jLineupService.startBeforeRun(jobConfigBuilder.build()).getId());
        return "Example run startet with 'before' step with Browser '" + jobConfigBuilder.build().browser + "'.";
    }

    @PostMapping("/runs/{runId}")
    public ResponseEntity<Void> runAfter(@PathVariable String runId, HttpServletRequest request) throws Exception {
        jLineupService.startAfterRun(runId);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(request.getContextPath() + "/runs/" + runId));
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<JLineupRunStatus> getRun(@PathVariable String runId) {
        Optional<JLineupRunStatus> run = jLineupService.getRun(runId);
        return run
                .map(jLineupRunStatus -> new ResponseEntity<>(jLineupRunStatus, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
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
}
