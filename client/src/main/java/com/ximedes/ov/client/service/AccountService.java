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
import com.chain.api.Asset;
import com.chain.api.Balance;
import com.chain.api.MockHsm;
import com.chain.api.Transaction;
import com.chain.exception.ChainException;
import com.chain.http.Client;
import com.chain.signing.HsmSigner;
import com.ximedes.ov.client.ClientConfig;
import com.ximedes.ov.client.actor.ActorManager;
import com.ximedes.ov.client.model.Account;
import com.ximedes.ov.shared.Assets;
import com.ximedes.ov.shared.ClusterConstants;
import com.ximedes.ov.shared.Keys;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static com.ximedes.ov.protocol.BackendProtocol.IdRequest;
import static com.ximedes.ov.protocol.BackendProtocol.IdResponse;

@Service
@Slf4j
public class AccountService {

    private final Client client;
    private final MockHsm.Key key;
    private final Asset eur;
    private final ActorRef idGenerator;
    private final ActorRef ledger;
    private final Timeout timeout;

    /**
     * Auto wired constructor
     */
    public AccountService(Client client, ClientConfig config, Keys keys, Assets assets, ActorManager actorManager, Timeout timeout) throws ChainException {
        this.client = client;
        this.key = keys.getKey(config.getKeyAlias());
        this.eur = assets.getAsset("eur", key);
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
        com.chain.api.Account account = findByAlias(alias);
        if (account == null) {
            account = new com.chain.api.Account.Builder().setAlias(alias).addRootXpub(key.xpub).setQuorum(1).create(client);
        }

        if (balance != null && balance > 0) {
            // create transaction and send (asynchronously) to ledger
            Transaction.Builder issuance = new Transaction.Builder()
                    .addAction(new Transaction.Action.Issue().setAssetId(eur.id).setAmount(balance))
                    .addAction(new Transaction.Action.ControlWithAccount().setAccountId(account.id).setAssetId(eur.id).setAmount(balance));
            ledger.tell(issuance, ActorRef.noSender());
        }
    }

    public void reset() throws ChainException {
        resetAccounts();
    }

    /**
     * Reset all available accounts to its initial state by retiring all available balances
     */
    void resetAccounts() throws ChainException {
        final com.chain.api.Account.Items items = new com.chain.api.Account.QueryBuilder().execute(client);
        while (items.hasNext()) {
            final com.chain.api.Account account = items.next();
            if (account.alias.startsWith(ClusterConstants.ACCOUNT_PREFIX)) {
                log.info("reset account: {}", account.alias);
                final long balance = getBalance(account.alias).longValue();

                // retire the balance of the given account, to create a '0' balance starting point
                if (balance > 0) {
                    ledger.tell(new Transaction.Builder()
                            .addAction(new Transaction.Action.SpendFromAccount()
                                    .setAccountId(account.id)
                                    .setAssetId(eur.id)
                                    .setAmount(balance))
                            .addAction(new Transaction.Action.Retire()
                                    .setAssetId(eur.id)
                                    .setAmount(balance)), ActorRef.noSender());
                }
            } else {
                log.debug("other account: '{}'", account.alias);
            }
        }
    }

    public Long getBalance(final String accountId) {
        try {
            Balance.Items balances = new Balance.QueryBuilder().setFilter("account_alias=$1").addFilterParameter(accountId).execute(client);
            return balances.list.stream().map(b -> b.amount).reduce(0L, Long::sum);
        } catch (ChainException e) {
            log.warn("getBalance() : Exception: {}", e.toString());
        }
        return 0L;
    }

    public Optional<com.ximedes.ov.client.model.Account> queryAccount(final String accountId) throws ChainException {
        com.chain.api.Account account = findByAlias(accountId);
        if (account != null) {
            return Optional.of(Account.builder().accountId(accountId).balance(getBalance(accountId).intValue()).build());
        } else {
            return Optional.empty();
        }
    }

    com.chain.api.Account findByAlias(final String alias) {
        try {
            final com.chain.api.Account.Items items = new com.chain.api.Account.QueryBuilder().setFilter("alias=$1").addFilterParameter(alias).execute(client);
            return items.hasNext() ? items.next() : null;
        } catch (ChainException e) {
            log.warn("findByAlias() : Exception: {}", e.toString());
        }
        return null;
    }

    String createAlias(final String alias) {
        return "ov-chain-" + alias;
    }

}
