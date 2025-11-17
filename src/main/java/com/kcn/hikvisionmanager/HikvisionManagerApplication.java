package com.kcn.hikvisionmanager;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@ConfigurationPropertiesScan
@Slf4j
public class HikvisionManagerApplication {

    @PostConstruct
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("TIMEZONE", "UTC")));
        log.info("⏰ Default timezone set to {}", TimeZone.getDefault().getID());
    }

	public static void main(String[] args) {
		SpringApplication.run(HikvisionManagerApplication.class, args);
	}

}
