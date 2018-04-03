package de.otto.jlineup.web;

import com.google.gson.JsonParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    @PostMapping("/runs")
    public void runBefore(final HttpServletRequest httpServletRequest,
                            final HttpServletResponse httpServletResponse,
                            final @RequestBody String config) throws IOException {

        String id = jLineupService.startBeforeRun(config).getId();
        httpServletResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
        httpServletResponse.setHeader("Location", String.format("/runs/%s", id));
    }

    @PostMapping("/runs/{id}")
    public void runAfter(final HttpServletRequest httpServletRequest,
                         final HttpServletResponse httpServletResponse,
                         @PathVariable final String id) throws IOException {
        jLineupService.startAfterRun(id);
        httpServletResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
        httpServletResponse.setHeader("Location", String.format("/runs/%s", id));
    }

    @GetMapping("/runs/{id}")
    public String getRun(final HttpServletRequest httpServletRequest,
                         final HttpServletResponse httpServletResponse,
                         @PathVariable final String id) throws IOException {
        Optional<JLineupRunStatus> run = jLineupService.getRun(id);
        if (run.isPresent()) {
            return run.get().toString();
        } else {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Run with id " + id + " not found");
            return null;
        }
    }

    @ExceptionHandler(JLineupWebException.class)
    public void exceptionHandler(final JLineupWebException exception,
                                 final HttpServletResponse response) throws IOException {
        response.sendError(exception.getStatus(),
                exception.getMessage());
    }

    @ExceptionHandler(JsonParseException.class)
    public void jsonParseExceptionHandler(final JsonParseException exception,
                                          final HttpServletResponse response) throws IOException {
        response.sendError(UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }
}
