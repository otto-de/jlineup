package de.otto.jlineup.web;

import de.otto.jlineup.Utils;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

import static java.lang.invoke.MethodHandles.lookup;

@PropertySource(value = "version.properties", ignoreResourceNotFound = true)
@SpringBootApplication(scanBasePackages = {"de.otto.jlineup", "de.otto.edison"})
@EnableConfigurationProperties(JLineupWebProperties.class)
public class JLineupWebApplication {

	private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

	public static void main(String[] args) {
		LOG.info("\nStarting JLineup {}\n", Utils.getVersion());
		SpringApplicationBuilder builder = new SpringApplicationBuilder(JLineupWebApplication.class);
		builder.headless(false).run(args);

	}
}
