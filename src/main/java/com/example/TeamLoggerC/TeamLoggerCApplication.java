package com.example.TeamLoggerC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TeamLoggerCApplication {

	public static void main(String[] args) {

		System.setProperty("java.awt.headless", "false");

		System.out.println(
				"Java headless mode at startup: "
						+ java.awt.GraphicsEnvironment.isHeadless()
		);

		SpringApplication.run(
				TeamLoggerCApplication.class,
				args
		);
	}
}