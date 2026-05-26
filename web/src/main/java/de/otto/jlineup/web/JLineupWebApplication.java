package de.otto.jlineup.web;

import de.otto.jlineup.Utils;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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

	@Component
	static class StartupLogger {
		@EventListener
		public void onWebServerReady(WebServerInitializedEvent event) {
			int port = event.getWebServer().getPort();
			String contextPath = event.getApplicationContext().getEnvironment()
					.getProperty("server.servlet.context-path", "");
			if (contextPath.equals("/")) {
				contextPath = "";
			}
			String address = event.getApplicationContext().getEnvironment()
					.getProperty("server.address", "localhost");
			if (address.equals("0.0.0.0") || address.equals("::")) {
				address = "localhost";
			}
			LOG.info("\n\n --- http://{}:{}{}/internal/status ---\n", address, port, contextPath);
		}
	}
}
