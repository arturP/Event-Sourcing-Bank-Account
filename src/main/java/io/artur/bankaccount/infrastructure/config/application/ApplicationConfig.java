package io.artur.bankaccount.infrastructure.config.application;

import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.application.services.AccountApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    
    @Bean
    public AccountApplicationService accountApplicationService(AccountRepository accountRepository) {
        return new AccountApplicationService(accountRepository);
    }
}