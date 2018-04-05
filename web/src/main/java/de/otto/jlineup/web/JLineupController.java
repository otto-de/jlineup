package de.otto.jlineup.web;

import com.google.gson.JsonParseException;
import de.otto.jlineup.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

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
    public ResponseEntity<Void> runBefore(@RequestBody Config config) {
        String id = jLineupService.startBeforeRun(config).getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/runs/" + id));
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @PostMapping("/runs/{runId}")
    public ResponseEntity<Void> runAfter(@PathVariable String runId) {
        jLineupService.startAfterRun(runId);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/runs/" + runId));
        return new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<String> getRun(@PathVariable String runId) {
        Optional<JLineupRunStatus> run = jLineupService.getRun(runId);
        if (run.isPresent()) {
            return new ResponseEntity<>(run.get().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Run with id '" + runId + "' not found", HttpStatus.NOT_FOUND);
        }
    }

    @ExceptionHandler(JLineupWebException.class)
    public ResponseEntity<String> exceptionHandler(final JLineupWebException exception) {
        return new ResponseEntity<>(exception.getMessage(), exception.getStatus());
    }

}
