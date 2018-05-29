package de.otto.jlineup.web.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
public class MyWebMvcConfigurationSupport extends WebMvcConfigurationSupport {

    private final ObjectMapper objectMapper;

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

}
