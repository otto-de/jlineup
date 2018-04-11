package de.otto.jlineup.web;

import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"de.otto.jlineup", "de.otto.edison"})
@EnableConfigurationProperties(JLineupWebProperties.class)
public class JLineupWebApplication {

	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(JLineupWebApplication.class);
		builder.headless(false).run(args);
	}
}
