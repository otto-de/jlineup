package de.otto.jlineup.web.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;

@Configuration
public class MyWebMvcConfigurationSupport extends WebMvcConfigurationSupport {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private final ObjectMapper objectMapper;

    @Autowired
    private JLineupWebProperties properties;

    @Autowired
    public MyWebMvcConfigurationSupport(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
        addDefaultHttpMessageConverters(converters);
        super.configureMessageConverters(converters);
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

}
