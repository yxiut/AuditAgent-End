package com.wb.audit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.wb.audit.**.mapper")
public class AuditAgentEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditAgentEndApplication.class, args);
    }

}
