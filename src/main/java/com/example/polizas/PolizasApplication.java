package com.example.polizas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PolizasApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolizasApplication.class, args);
	}

}
