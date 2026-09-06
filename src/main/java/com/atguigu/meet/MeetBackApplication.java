package com.atguigu.meet;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan(basePackages = "com.atguigu.meet.mapper")
@EnableTransactionManagement
@EnableFileStorage
@EnableScheduling
public class MeetBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetBackApplication.class, args);
    }

}