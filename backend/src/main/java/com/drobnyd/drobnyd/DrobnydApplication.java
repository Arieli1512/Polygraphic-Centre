package com.drobnyd.drobnyd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DrobnydApplication {

	public static void main(String[] args) {
		SpringApplication.run(DrobnydApplication.class, args);
	}

}
