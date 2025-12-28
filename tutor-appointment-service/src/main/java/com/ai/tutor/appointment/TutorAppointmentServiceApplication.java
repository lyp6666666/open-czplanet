package com.ai.tutor.appointment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ai.tutor.**.mapper")
public class TutorAppointmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutorAppointmentServiceApplication.class, args);
	}

}
