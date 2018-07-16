package de.otto.jlineup.browser;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
@SpringBootApplication(scanBasePackages = {"de.otto.jlineup"})
public class FakeWebServerController {

    @GetMapping("/200")
    public ResponseEntity<String> get200() {
        return new ResponseEntity<>("This is 200!", HttpStatus.OK);
    }

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

    @GetMapping("/params")
    public ResponseEntity<String> getParamsPageWithSlash(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap.get("param2")[0].endsWith("/")) {
            return new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Params / page!", HttpStatus.OK);
    }

    @GetMapping("/somerootpath")
    public ResponseEntity<String> getNotFoundOnRootPath() {
        return new ResponseEntity<>("This is Not Found!", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/somerootpath/somevalidsubpath")
    public ResponseEntity<String> getPageOnDeeperPath() {
        return new ResponseEntity<>("Hallo! This is valid!", HttpStatus.OK);
    }

}
