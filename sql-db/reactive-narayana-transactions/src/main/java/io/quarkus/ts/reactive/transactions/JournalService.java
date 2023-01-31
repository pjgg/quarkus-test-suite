package io.quarkus.ts.reactive.transactions;

import static io.quarkus.ts.reactive.transactions.JournalEntity.getAllJournalRecords;
import static io.quarkus.ts.reactive.transactions.JournalEntity.getLatestJournalRecord;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class JournalService {

    public Uni<JournalEntity> addToJournal(String accountFrom, String accountTo, String annotation, int amount) {
        JournalEntity journal = new JournalEntity(accountFrom, accountTo, annotation, amount);
        return journal.addLog();
    }

    public Uni<List<JournalEntity>> getJournalRecords() {
        return getAllJournalRecords();
    }

    public Uni<JournalEntity> getLatestJournalRecordByAccountNumber(String accountNumber) {
        return getLatestJournalRecord(accountNumber);
    }
}
