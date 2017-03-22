/*
 * Copyright (C) 2016 Mark Wigmans (mark.wigmans@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ximedes.ov.client.service;

import akka.actor.ActorRef;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import com.chain.api.Transaction;
import com.chain.exception.ChainException;
import com.ximedes.ov.client.actor.ActorManager;
import com.ximedes.ov.client.model.Account;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static com.ximedes.ov.protocol.IdProtocol.IdRequest;
import static com.ximedes.ov.protocol.IdProtocol.IdResponse;

@Service
@Slf4j
public class AccountService {
    private static final String AssetAlias = "eur";

    private final ActorRef idGenerator;
    private final ActorRef ledger;
    private final Timeout timeout;

    /**
     * Auto wired constructor
     */
    public AccountService(ActorManager actorManager, Timeout timeout) throws ChainException {
        this.idGenerator = actorManager.getIdGenerator();
        this.ledger = actorManager.getLedgerActor();
        this.timeout = timeout;
    }

    public CompletableFuture<Account> createAccount(final Account request) throws ChainException {
        final CompletableFuture<Object> ask = PatternsCS.ask(idGenerator, IdRequest.getDefaultInstance(), timeout).toCompletableFuture();

        return ask.thenApply(r -> {
            final IdResponse response = (IdResponse) r;
            final String alias = response.getId();
            log.info("createAccount({})", alias);
            addToBlockChain(alias, request.getBalance());

            return Account.builder().accountId(alias).build();
        }).exceptionally(ex -> {
            log.error("Unrecoverable error", ex);
            return null;
        });
    }

    @SneakyThrows
    void addToBlockChain(final String alias, Integer balance) {
        if (balance != null && balance > 0) {
            // create transaction and send (asynchronously) to ledger
            Transaction.Builder issuance = new Transaction.Builder()
                    .addAction(new Transaction.Action.Issue().setAssetAlias(AssetAlias).setAmount(balance))
                    .addAction(new Transaction.Action.ControlWithAccount().setAccountAlias(alias).setAssetAlias(AssetAlias).setAmount(balance));
            ledger.tell(issuance, ActorRef.noSender());
        }
    }
}
