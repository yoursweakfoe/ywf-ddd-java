package com.yoursweakfoe.sampleapplication.sampleservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
