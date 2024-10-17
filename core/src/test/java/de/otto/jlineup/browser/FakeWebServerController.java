package de.otto.jlineup.browser;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

@Controller
@SpringBootApplication
public class FakeWebServerController {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @GetMapping({"/200", "/200/ "})
    public ResponseEntity<String> get200() {
        return new ResponseEntity<>("This is 200!", HttpStatus.OK);
    }

    @GetMapping({"/403", "/403/"})
    public ResponseEntity<String> get403() {
        return new ResponseEntity<>("This is 403!", HttpStatus.FORBIDDEN);
    }

    @GetMapping({"/404", "/404/"})
    public ResponseEntity<String> get404() {
        return new ResponseEntity<>("This is 404!", HttpStatus.NOT_FOUND);
    }

    @GetMapping({"/500", "/500/"})
    public ResponseEntity<String> get500() {
        return new ResponseEntity<>("This is 500!", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping({"/params", "/params/"})
    public ResponseEntity<String> getParamsPageWithSlash(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap.get("param2")[0].endsWith("/")) {
            return new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Params / page!", HttpStatus.OK);
    }

    @GetMapping({"/somerootpath", "/somerootpath/"})
    public ResponseEntity<String> getNotFoundOnRootPath() {
        return new ResponseEntity<>("This is Not Found!", HttpStatus.NOT_FOUND);
    }

    @GetMapping({"/somerootpath/somevalidsubpath", "/somerootpath/somevalidsubpath/"})
    public ResponseEntity<String> getPageOnDeeperPath() {
        return new ResponseEntity<>("Hallo! This is valid!", HttpStatus.OK);
    }

    @GetMapping({"/cookies","/cookies/"})
    public ResponseEntity<String> testAlteringCookies(@CookieValue(value = "alternating", required = false) String alternatingCookieValue, HttpServletRequest request) {
        LOG.info("Request URI is {}", request.getRequestURI());
        LOG.info("Cookie value is {}", alternatingCookieValue);
        if (alternatingCookieValue == null) {
            return new ResponseEntity<>("No cookie found", HttpStatus.OK);
        }
        return new ResponseEntity<>("Alternating cookie value is " + alternatingCookieValue, HttpStatus.valueOf(alternatingCookieValue));
    }

    /**
     * Returns an empty favicon to avoid 404 errors
     */
    @GetMapping("favicon.ico")
    public ResponseEntity<String> favicon() {
        return new ResponseEntity<>("", HttpStatus.OK);
    }

}
