package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class JLineupService {

    public String startRun(String config) {
        return String.valueOf(UUID.randomUUID());
    }

}
