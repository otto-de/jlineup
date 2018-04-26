package de.otto.jlineup.browser;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@SpringBootApplication(scanBasePackages = {"de.otto.jlineup"})
public class FakeWebServer {

    @GetMapping("/403")
    public ResponseEntity<String> get403() {
        return new ResponseEntity<>("This is 403!", HttpStatus.FORBIDDEN);
    }

    @GetMapping("/404")
    public ResponseEntity<String> get404() {
        return new ResponseEntity<>("This is 404!", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/500")
    public ResponseEntity<String> get500() {
        return new ResponseEntity<>("This is 500!", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
