package com.dinhuan.shortify;

import com.dinhuan.shortify.configuration.url.ShortenerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties(ShortenerProperties.class)
public class ShortifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShortifyApplication.class, args);
	}

}
