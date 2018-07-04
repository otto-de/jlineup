package de.otto.jlineup.web.configuration;

import de.otto.jlineup.service.Housekeeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
public class JLineupConfiguration {

    @Bean
    public Housekeeper houseKeeper(JLineupWebProperties properties) {
        return new Housekeeper(Paths.get(properties.getWorkingDirectory()));
    }

}
