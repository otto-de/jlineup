package de.otto.jlineup.web;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
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

@RestController
public class JLineupController {

    private final JLineupService jLineupService;

    @Autowired
    public JLineupController(JLineupService jLineupService) {
        this.jLineupService = jLineupService;
    }

    @GetMapping("/")
    public String getHello() {
        return "<p>JLineup is great! Do you want to go to my <a href=\"/internal/status\">status page</a>?</p>";
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
