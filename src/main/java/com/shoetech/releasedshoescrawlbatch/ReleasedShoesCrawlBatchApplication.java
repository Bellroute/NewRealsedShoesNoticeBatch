package com.shoetech.releasedshoescrawlbatch;

import lombok.val;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableBatchProcessing
public class ReleasedShoesCrawlBatchApplication {

    public static void main(String[] args) {
        val context = SpringApplication.run(ReleasedShoesCrawlBatchApplication.class, args);
        SpringApplication.exit(context);
    }

}
