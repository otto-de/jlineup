package de.otto.jlineup.web;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.service.InvalidRunStateException;
import de.otto.jlineup.service.JLineupService;
import de.otto.jlineup.service.RunNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return "JLineup is great!";
    }

    @PostMapping(value = "/runs")
    public ResponseEntity<Void> runBefore(@RequestBody JobConfig jobConfig) {
        String id = jLineupService.startBeforeRun(jobConfig).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/runs/" + id));
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @PostMapping("/runs/{runId}")
    public ResponseEntity<Void> runAfter(@PathVariable String runId) throws InvalidRunStateException, RunNotFoundException {
        jLineupService.startAfterRun(runId);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/runs/" + runId));
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<JLineupRunStatus> getRun(@PathVariable String runId) {
        Optional<JLineupRunStatus> run = jLineupService.getRun(runId);
        if (run.isPresent()) {
            return new ResponseEntity<>(run.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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

}
