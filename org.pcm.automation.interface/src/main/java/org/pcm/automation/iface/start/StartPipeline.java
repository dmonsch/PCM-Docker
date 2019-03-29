package org.pcm.automation.iface.start;

import org.pcm.automation.iface.RestApplication;
import org.springframework.boot.SpringApplication;

public class StartPipeline {

	public static void main(String[] args) {
		SpringApplication.run(RestApplication.class, args);
	}

}