package de.otto.jlineup.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JLineupController {

    @GetMapping("/")
    public String getHello() {
        return "hello...";
    }
}
