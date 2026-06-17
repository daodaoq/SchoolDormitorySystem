package org.java.backed;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("org.java.backed.mapper")
@EnableScheduling
@EnableAsync
public class BackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackedApplication.class, args);
    }

}
 