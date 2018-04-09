package de.otto.jlineup.web.configuration;

import de.otto.jlineup.service.ReportHousekeeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
public class JLineupConfiguration {

    @Bean
    public ReportHousekeeper reportHousekeeper(JLineupWebProperties properties) {
        return new ReportHousekeeper(Paths.get(properties.getWorkingDirectory()));
    }

}
