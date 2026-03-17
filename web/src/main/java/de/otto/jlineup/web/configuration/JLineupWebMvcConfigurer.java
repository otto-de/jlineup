package de.otto.jlineup.web.configuration;

import de.otto.jlineup.JacksonWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

@Configuration
public class JLineupWebMvcConfigurer implements WebMvcConfigurer {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private final JsonMapper jsonMapper;
    private final JLineupWebProperties properties;

    @Autowired
    public JLineupWebMvcConfigurer(JLineupWebProperties properties) {
        this.jsonMapper = JacksonWrapper.jsonMapper();
        this.properties = properties;
    }

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

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper));
    }
}
