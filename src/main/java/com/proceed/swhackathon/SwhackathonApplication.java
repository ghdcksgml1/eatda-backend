package com.proceed.swhackathon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class SwhackathonApplication {

	static {
		System.setProperty("com.amazonaws.sdk.disableEc2Metadata", "true");
	}

//	@PostConstruct
//	public void started(){
//		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
//		System.out.println("íėŽėę° : " + new Date());
//	}

	public static void main(String[] args) {
		SpringApplication.run(SwhackathonApplication.class, args);
	}

}
