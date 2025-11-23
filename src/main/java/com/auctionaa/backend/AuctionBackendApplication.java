package com.auctionaa.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoAuditing
@EnableConfigurationProperties
@ComponentScan(basePackages = {"com.auctionaa.backend", "AdminBackend"})
@EnableMongoRepositories(basePackages = {"com.auctionaa.backend.Repository", "AdminBackend.Repository"})
public class AuctionBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionBackendApplication.class, args);
		System.out.println("Hello");
	}
}
