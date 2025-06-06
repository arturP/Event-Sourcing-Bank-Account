package io.artur.eventsourcing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EventSourcingBankAccountApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventSourcingBankAccountApplication.class, args);
    }
}