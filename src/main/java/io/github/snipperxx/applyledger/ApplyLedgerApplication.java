package io.github.snipperxx.applyledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApplyLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplyLedgerApplication.class, args);
    }
}
