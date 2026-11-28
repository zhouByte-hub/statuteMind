package com.zhoubyte.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.zhoubyte")
@ConfigurationPropertiesScan(basePackages = "com.zhoubyte")
public class MindCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindCoreApplication.class, args);
    }

}
