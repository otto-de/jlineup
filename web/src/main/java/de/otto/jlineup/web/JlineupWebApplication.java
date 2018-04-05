package de.otto.jlineup.web;

import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "de.otto.jlineup")
@EnableConfigurationProperties(JLineupWebProperties.class)
public class JlineupWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(JlineupWebApplication.class, args);
	}
}
