package io.artur.bankaccount;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "io.artur.bankaccount",
    "io.artur.eventsourcing"  // Temporary: Keep scanning old packages during migration
})
public class BankAccountApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BankAccountApplication.class, args);
    }
}