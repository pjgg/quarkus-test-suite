package io.quarkus.ts.reactive.transactions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Named("regular-transaction")
public class TransferTransactionService extends TransferProcessor {

    private static final Logger LOG = Logger.getLogger(TransferTransactionService.class);
    private final static String ANNOTATION_TRANSACTION = "user transaction to other user";
    private final MeterRegistry registry;
    private long transactionsAmount;

    public TransferTransactionService(MeterRegistry registry) {
        this.registry = registry;
        registry.gauge("transaction.regular.amount", this, TransferTransactionService::getTransactionsAmount);
    }

    public Uni<JournalEntity> makeTransaction(String from, String to, int amount) {
        LOG.infof("Regular transaction, from %s to %s amount %s", from, to, amount);
        return verifyAccounts(from, to)
                .onItem().transformToUni(l -> accountService.decreaseBalance(from, amount))
                .onItem().transformToUni(fromUpdatedAmount -> accountService.increaseBalance(to, amount))
                .onItem()
                .transformToUni(i -> {
                    Uni<JournalEntity> journalEntity = journalService.addToJournal(from, to, ANNOTATION_TRANSACTION, amount);
                    transactionsAmount++;
                    return journalEntity;
                })
                .onFailure().invoke(this::exceptionHandler);
    }

    private void exceptionHandler(Throwable e) {
        LOG.errorf("Error on regular transaction %s ", e.getMessage());
        transactionsAmount--;
    }

    public long getTransactionsAmount() {
        return transactionsAmount;
    }
}
