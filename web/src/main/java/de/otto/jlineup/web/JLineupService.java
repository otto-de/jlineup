package de.otto.jlineup.web;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JLineupService {

    public String startRun(String config) {
        return String.valueOf(UUID.randomUUID());
    }

}
