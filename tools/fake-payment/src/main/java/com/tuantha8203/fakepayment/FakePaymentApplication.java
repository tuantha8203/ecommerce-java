package com.tuantha8203.fakepayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FakePaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(FakePaymentApplication.class, args);
	}

}
