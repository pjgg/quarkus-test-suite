package io.quarkus.ts.reactive.transactions;

import static javax.ws.rs.core.Response.Status.CREATED;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/transfer")
public class TransferResource {

    @Inject
    @Named("regular-transaction")
    TransferProcessor regularTransaction;

    @Inject
    @Named("topup")
    TransferProcessor topUp;

    @Inject
    @Named("withdrawal")
    TransferProcessor withdrawal;

    @Inject
    JournalService journalService;

    @Inject
    AccountService accountService;

    @Path("/transaction")
    @POST
    @Transactional
    public Uni<Response> makeRegularTransaction(TransferDTO transferDTO) {
        return regularTransaction
                .makeTransaction(transferDTO.getAccountFrom(), transferDTO.getAccountTo(), transferDTO.getAmount())
                .map(journalEntity -> Response.ok(journalEntity.getId()).status(CREATED).build());
    }

    @Path("/top-up")
    @POST
    public Uni<Response> topup(TransferDTO transferDTO) {
        return topUp.makeTransaction(transferDTO.getAccountFrom(), transferDTO.getAccountTo(), transferDTO.getAmount())
                .map(journalEntity -> Response.ok(journalEntity.getId()).status(CREATED).build());
    }

    @Path("/withdrawal")
    @POST
    @Transactional
    public Uni<Response> makeMoneyTransaction(TransferDTO transferDTO) {
        return withdrawal.makeTransaction(transferDTO.getAccountFrom(), transferDTO.getAccountTo(), transferDTO.getAmount())
                .map(journalEntity -> Response.ok(journalEntity.getId()).status(CREATED).build());
    }

    @Path("/accounts/{account_id}")
    @GET
    @Transactional
    public Uni<AccountEntity> getAccountById(@PathParam("account_id") String accountNumber) {
        return accountService.getAccount(accountNumber);
    }

    @Path("/journal/latest/{account_id}")
    @GET
    public Uni<JournalEntity> getLatestJournalRecord(@PathParam("account_id") String accountNumber) {
        return journalService.getLatestJournalRecordByAccountNumber(accountNumber);
    }
}
