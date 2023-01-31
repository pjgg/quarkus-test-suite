package io.quarkus.ts.reactive.transactions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Named("withdrawal")
public class TransferWithdrawalService extends TransferProcessor {

    private static final Logger LOG = Logger.getLogger(TransferWithdrawalService.class);
    private final static String ANNOTATION_WITHDRAWAL = "user withdrawal";

    private final MeterRegistry registry;
    private long transactionsAmount;

    public TransferWithdrawalService(MeterRegistry registry) {
        this.registry = registry;
        registry.gauge("transaction.withdrawal.amount", this, TransferWithdrawalService::getTransactionsAmount);
    }

    public Uni<JournalEntity> makeTransaction(String from, String to, int amount) {
        LOG.infof("Withdrawal account %s amount %s", from, amount);
        return verifyAccounts(from)
                .flatMap(l -> {
                    transactionsAmount++;
                    return accountService.decreaseBalance(from, amount);
                })
                .flatMap(updatedAmount -> journalService.addToJournal(from, to, ANNOTATION_WITHDRAWAL, updatedAmount))
                .onFailure().invoke(() -> transactionsAmount--);
    }

    public long getTransactionsAmount() {
        return transactionsAmount;
    }
}
