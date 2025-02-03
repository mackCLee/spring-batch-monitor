package com.lch275.springBatchMonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class SpringBatchMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBatchMonitorApplication.class, args);
	}

}
