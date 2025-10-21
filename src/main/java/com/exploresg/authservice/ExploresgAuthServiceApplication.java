package com.exploresg.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ExploresgAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExploresgAuthServiceApplication.class, args);
	}

}
