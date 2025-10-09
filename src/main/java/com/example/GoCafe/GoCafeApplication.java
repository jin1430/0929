package com.example.GoCafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.example.GoCafe")
public class GoCafeApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoCafeApplication.class, args);
	}

}
