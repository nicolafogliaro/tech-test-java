package com.nicolafogliaro.orderservice.api.config;


import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER)))
                .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER)));
    }

//    @Bean
//    public ObjectMapper objectMapper() {
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        // Register Java Time Module for LocalDate and LocalDateTime
//        JavaTimeModule javaTimeModule = new JavaTimeModule();
//        javaTimeModule.addSerializer(
//                java.time.LocalDateTime.class,
//                new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER))
//        );
//
//        objectMapper.registerModule(javaTimeModule);
//
//        // Serialize BigDecimal as strings with proper formatting
//        SimpleModule customModule = new SimpleModule();
//        customModule.addSerializer(BigDecimal.class, new ToStringSerializer()); // BigDecimal as String
//        objectMapper.registerModule(customModule);
//
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO-8601 for dates
//        return objectMapper;
//    }
}