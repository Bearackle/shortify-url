package com.dinhuan.shortify.configuration.url;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqids.Sqids;

@Configuration
public class SqidsConfig {
    @Bean
    public Sqids sqids(
            ShortenerProperties properties
    ) {
        return Sqids.builder()
                .alphabet(properties.getAlphabet())
                .minLength(properties.getMinLength())
                .build();
    }
}
