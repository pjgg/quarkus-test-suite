package io.quarkus.ts.reactive.transactions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.logging.Logger;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.RunOptions;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Named("topup")
public class TransferTopUpService extends TransferProcessor {
    private static final Logger LOG = Logger.getLogger(TransferTopUpService.class);
    private final static String ANNOTATION_TOP_UP = "user top up";
    private final static int TRANSACTION_TIMEOUT_SEC = 10;

    public Uni<JournalEntity> makeTransaction(String from, String to, int amount) {
        LOG.infof("TopUp account %s amount %s", from, amount);
        return verifyAccounts(to)
                .flatMap(l -> QuarkusTransaction.call(QuarkusTransaction.runOptions()
                        .timeout(TRANSACTION_TIMEOUT_SEC)
                        .exceptionHandler(t -> RunOptions.ExceptionResult.ROLLBACK)
                        .semantic(RunOptions.Semantic.REQUIRE_NEW),
                        () -> accountService.increaseBalance(from, amount).onItem().transformToUni(
                                amountUpdated -> journalService.addToJournal(from, to, ANNOTATION_TOP_UP,
                                        amount))));
    }

}
