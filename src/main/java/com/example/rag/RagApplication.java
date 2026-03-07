package com.example.rag;

import com.example.rag.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class RagApplication {
	public static void main(String[] args) {
		SpringApplication.run(RagApplication.class, args);
	}
}