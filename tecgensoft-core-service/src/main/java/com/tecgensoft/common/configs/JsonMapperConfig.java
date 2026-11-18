package com.tecgensoft.common.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonMapperConfig {
    @Bean
    public ObjectMapper applicationObjectMapper() {
        return new ObjectMapper();
    }
}
