package com.roadinspection;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.roadinspection.mapper")
@EnableAsync
@EnableScheduling
public class RoadInspectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoadInspectionApplication.class, args);
    }
}
