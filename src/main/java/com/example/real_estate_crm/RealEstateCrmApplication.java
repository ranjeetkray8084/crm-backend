package com.example.real_estate_crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.real_estate_crm.model")
@EnableJpaRepositories("com.example.real_estate_crm.repository")
public class RealEstateCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealEstateCrmApplication.class, args);
	}

}
