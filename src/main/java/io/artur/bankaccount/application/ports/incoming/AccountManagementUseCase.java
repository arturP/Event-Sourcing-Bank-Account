package io.artur.bankaccount.application.ports.incoming;

import io.artur.bankaccount.application.commands.models.*;
import java.util.UUID;

public interface AccountManagementUseCase {
    
    UUID openAccount(OpenAccountCommand command);
    
    void deposit(DepositMoneyCommand command);
    
    void withdraw(WithdrawMoneyCommand command);
    
    void transfer(TransferMoneyCommand command);
}