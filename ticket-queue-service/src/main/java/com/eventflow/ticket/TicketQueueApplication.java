package com.eventflow.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TicketQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketQueueApplication.class, args);
    }
}
