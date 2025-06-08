package io.artur.bankaccount.application.services;

import io.artur.bankaccount.application.commands.models.*;
import io.artur.bankaccount.application.ports.incoming.AccountManagementUseCase;
import io.artur.bankaccount.application.ports.incoming.AccountQueryUseCase;
import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.application.ports.outgoing.CachePort;
import io.artur.bankaccount.application.ports.outgoing.MetricsPort;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.valueobjects.Money;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountApplicationService implements AccountManagementUseCase, AccountQueryUseCase {
    
    private final AccountRepository accountRepository;
    private final CachePort cachePort;
    private final MetricsPort metricsPort;
    
    public AccountApplicationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.cachePort = null; // Optional dependency
        this.metricsPort = null; // Optional dependency
    }
    
    public AccountApplicationService(AccountRepository accountRepository, CachePort cachePort, MetricsPort metricsPort) {
        this.accountRepository = accountRepository;
        this.cachePort = cachePort;
        this.metricsPort = metricsPort;
    }
    
    @Override
    public UUID openAccount(OpenAccountCommand command) {
        return recordMetrics(() -> {
            command.validate();
            
            BankAccount account = BankAccount.openNewAccount(
                command.getAccountId(),
                command.getAccountHolder(),
                command.getOverdraftLimit(),
                command.getMetadata()
            );
            
            accountRepository.save(account);
            
            // Invalidate cache for new account
            if (cachePort != null) {
                cachePort.invalidateAccount(account.getAccountId());
            }
            
            // Record business metric
            if (metricsPort != null) {
                metricsPort.recordAccountCreation();
            }
            
            return account.getAccountId();
        });
    }
    
    @Override
    public void deposit(DepositMoneyCommand command) {
        recordMetrics(() -> {
            command.validate();
            
            BankAccount account = loadAccount(command.getAccountId());
            account.deposit(command.getAmount(), command.getMetadata());
            accountRepository.save(account);
            
            // Update cache with new balance
            if (cachePort != null) {
                cachePort.updateBalance(account.getAccountId(), account.getBalance());
                cachePort.invalidateAccount(account.getAccountId()); // Invalidate summary
            }
            
            // Record business metric
            if (metricsPort != null) {
                metricsPort.recordDeposit();
            }
        });
    }
    
    @Override
    public void withdraw(WithdrawMoneyCommand command) {
        recordMetrics(() -> {
            command.validate();
            
            try {
                BankAccount account = loadAccount(command.getAccountId());
                account.withdraw(command.getAmount(), command.getMetadata());
                accountRepository.save(account);
                
                // Update cache with new balance
                if (cachePort != null) {
                    cachePort.updateBalance(account.getAccountId(), account.getBalance());
                    cachePort.invalidateAccount(account.getAccountId()); // Invalidate summary
                }
                
                // Record business metric
                if (metricsPort != null) {
                    metricsPort.recordWithdrawal();
                }
            } catch (Exception e) {
                // Record overdraft attempt if withdrawal fails
                if (metricsPort != null && e.getMessage().contains("overdraft")) {
                    metricsPort.recordOverdraftAttempt();
                }
                throw e;
            }
        });
    }
    
    @Override
    public void transfer(TransferMoneyCommand command) {
        recordMetrics(() -> {
            command.validate();
            
            BankAccount fromAccount = loadAccount(command.getFromAccountId());
            BankAccount toAccount = loadAccount(command.getToAccountId());
            
            fromAccount.transferOut(command.getToAccountId(), command.getAmount(), command.getDescription(), command.getMetadata());
            toAccount.receiveTransfer(command.getFromAccountId(), command.getAmount(), command.getDescription(), command.getMetadata());
            
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            
            // Update cache for both accounts
            if (cachePort != null) {
                cachePort.updateBalance(fromAccount.getAccountId(), fromAccount.getBalance());
                cachePort.updateBalance(toAccount.getAccountId(), toAccount.getBalance());
                cachePort.invalidateAccount(fromAccount.getAccountId());
                cachePort.invalidateAccount(toAccount.getAccountId());
            }
            
            // Record business metric
            if (metricsPort != null) {
                metricsPort.recordTransfer();
            }
        });
    }
    
    @Override
    public Optional<BankAccount> findAccountById(UUID accountId) {
        return recordMetrics(() -> {
            // Try cache first
            if (cachePort != null) {
                Optional<CachePort.AccountSummary> cachedSummary = cachePort.getCachedAccountSummary(accountId);
                if (cachedSummary.isPresent()) {
                    metricsPort.recordCacheHit("account-summary");
                    // For cached case, still need to load full aggregate from repository
                    // This is a simplified approach - in a real system you might cache the full aggregate
                } else if (metricsPort != null) {
                    metricsPort.recordCacheMiss("account-summary");
                }
            }
            
            return accountRepository.findById(accountId);
        });
    }
    
    @Override
    public List<BankAccount> findAllAccounts() {
        return recordMetrics(() -> accountRepository.findAll());
    }
    
    @Override
    public Money getAccountBalance(UUID accountId) {
        return recordMetrics(() -> {
            // Try cache first
            if (cachePort != null) {
                Optional<Money> cachedBalance = cachePort.getCachedBalance(accountId);
                if (cachedBalance.isPresent()) {
                    if (metricsPort != null) {
                        metricsPort.recordCacheHit("balance");
                    }
                    return cachedBalance.get();
                } else if (metricsPort != null) {
                    metricsPort.recordCacheMiss("balance");
                }
            }
            
            // Load from repository and cache result
            Money balance = accountRepository.findById(accountId)
                    .map(BankAccount::getBalance)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
            
            if (cachePort != null) {
                cachePort.updateBalance(accountId, balance);
            }
            
            return balance;
        });
    }
    
    // Helper methods
    
    private BankAccount loadAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }
    
    private <T> T recordMetrics(java.util.function.Supplier<T> operation) {
        if (metricsPort != null) {
            return metricsPort.recordCommandProcessing(operation);
        } else {
            return operation.get();
        }
    }
    
    private void recordMetrics(Runnable operation) {
        if (metricsPort != null) {
            metricsPort.recordCommandProcessing(operation);
        } else {
            operation.run();
        }
    }
    
    // Account Lifecycle Management Methods
    
    public void freezeAccount(FreezeAccountCommand command) {
        recordMetrics(() -> {
            command.validate();
            
            BankAccount account = loadAccount(command.getAccountId());
            account.freeze(command.getReason(), command.getFrozenBy(), command.getMetadata());
            accountRepository.save(account);
            
            // Invalidate cache entries for frozen account
            if (cachePort != null) {
                cachePort.invalidateAccount(account.getAccountId());
            }
            
            // Record business metric
            if (metricsPort != null) {
                metricsPort.recordAccountStatusChange("FROZEN");
            }
        });
    }
    
    public void closeAccount(CloseAccountCommand command) {
        recordMetrics(() -> {
            command.validate();
            
            BankAccount account = loadAccount(command.getAccountId());
            account.close(command.getReason(), command.getClosedBy(), command.getMetadata());
            accountRepository.save(account);
            
            // Invalidate cache entries for closed account
            if (cachePort != null) {
                cachePort.invalidateAccount(account.getAccountId());
            }
            
            // Record business metric
            if (metricsPort != null) {
                metricsPort.recordAccountStatusChange("CLOSED");
            }
        });
    }
    
    public void reactivateAccount(ReactivateAccountCommand command) {
        recordMetrics(() -> {
            command.validate();
            
            BankAccount account = loadAccount(command.getAccountId());
            account.reactivate(command.getReason(), command.getReactivatedBy(), command.getMetadata());
            accountRepository.save(account);
            
            // Invalidate cache entries for reactivated account
            if (cachePort != null) {
                cachePort.invalidateAccount(account.getAccountId());
            }
            
            // Record business metric
            if (metricsPort != null) {
                metricsPort.recordAccountStatusChange("ACTIVE");
            }
        });
    }
    
    public void markAccountDormant(MarkAccountDormantCommand command) {
        recordMetrics(() -> {
            command.validate();
            
            BankAccount account = loadAccount(command.getAccountId());
            account.markDormant(command.getReason(), command.getMarkedBy(), command.getMetadata());
            accountRepository.save(account);
            
            // Invalidate cache entries for dormant account
            if (cachePort != null) {
                cachePort.invalidateAccount(account.getAccountId());
            }
            
            // Record business metric
            if (metricsPort != null) {
                metricsPort.recordAccountStatusChange("DORMANT");
            }
        });
    }
}