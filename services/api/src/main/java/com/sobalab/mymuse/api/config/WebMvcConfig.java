package com.sobalab.mymuse.api.config;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Use Spring Boot's built-in converters to preserve lowercase enum query values.
        ApplicationConversionService.addApplicationConverters(registry);
    }
}
