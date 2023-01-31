package io.quarkus.ts.reactive.transactions;

import static io.quarkus.ts.reactive.transactions.AccountEntity.exist;
import static io.quarkus.ts.reactive.transactions.AccountEntity.findAccount;
import static io.quarkus.ts.reactive.transactions.AccountEntity.getAllAccountsRecords;
import static io.quarkus.ts.reactive.transactions.AccountEntity.updateAmount;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class AccountService {

    private static final Logger LOG = Logger.getLogger(AccountService.class);

    public Uni<Boolean> isPresent(String accountNumber) {
        return exist(accountNumber).onItem().transform(exist -> {
            if (!exist) {
                String msg = String.format("Account %s doesn't exist", accountNumber);
                LOG.warn(msg);
                throw new NotFoundException(msg);
            }
            return true;
        });
    }

    public Uni<Integer> increaseBalance(String account, int amount) {
        return findAccount(account)
                .map(accountEntity -> accountEntity.getAmount() + amount)
                .flatMap(updatedAmount -> updateAmount(account, updatedAmount));
    }

    public Uni<Integer> decreaseBalance(String account, int amount) {
        return findAccount(account)
                .map(accountEntity -> accountEntity.getAmount() - amount)
                .flatMap(updatedAmount -> {
                    if (updatedAmount < 0) {
                        String msg = String.format("Account %s Not enough balance.", account);
                        LOG.warn(msg);
                        throw new BadRequestException(msg);
                    }
                    return updateAmount(account, updatedAmount);
                });
    }

    public Uni<List<AccountEntity>> getAllAccounts() {
        return getAllAccountsRecords();
    }

    public Uni<AccountEntity> getAccount(String accountNumber) {
        return AccountEntity.findAccount(accountNumber);
    }
}
