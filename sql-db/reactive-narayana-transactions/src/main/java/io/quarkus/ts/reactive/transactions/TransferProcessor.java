package io.quarkus.ts.reactive.transactions;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public abstract class TransferProcessor {

    @Inject
    AccountService accountService;

    @Inject
    JournalService journalService;

    protected Uni<List<Boolean>> verifyAccounts(String... accounts) {
        return Multi.createFrom().items(accounts)
                .onItem().transform(account -> accountService.isPresent(account))
                .collect().in(ArrayList::new, List::contains);
    }

    public abstract Uni<JournalEntity> makeTransaction(String from, String to, int amount);
}
