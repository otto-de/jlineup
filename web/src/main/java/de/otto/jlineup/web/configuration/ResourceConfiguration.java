package de.otto.jlineup.web.configuration;

import de.otto.jlineup.file.FileService;
import de.otto.jlineup.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class ResourceConfiguration implements WebMvcConfigurer {

    public final static Logger LOG = LoggerFactory.getLogger(ResourceConfiguration.class);

    @Autowired
    private JLineupWebProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        try {
            Files.createDirectories(Paths.get(properties.getWorkingDirectory()));
        } catch (IOException e) {
            LOG.error("Cannot create JLineup working directory.", e);
        }

        registry.addResourceHandler("/reports/**")
                .addResourceLocations("file:" + properties.getWorkingDirectory())
                .setCachePeriod(0);
    }
}

