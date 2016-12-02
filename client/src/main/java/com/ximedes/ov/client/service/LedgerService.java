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
import com.chain.api.Transaction;
import com.chain.exception.APIException;
import com.chain.http.BatchResponse;
import com.chain.http.Client;
import com.chain.signing.HsmSigner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by mawi on 01/12/2016.
 */
@Service
@Slf4j
public class LedgerService implements Serializable {

    private transient final Client client;

    /**
     * autowired constructor
     */
    public LedgerService(Client client) {
        this.client = client;
    }


    // TODO real messages
    public void submit(final ActorRef actor, final List<Transaction.Builder> msg, final String messageId) {
        log.debug("submit: {}", msg);
        submit(msg).thenAccept(r -> {
            actor.tell("message", ActorRef.noSender());
        }).exceptionally(ex -> {
            log.error("Exception", ex);
            actor.tell("error", ActorRef.noSender());
            return null;
        });
    }

    CompletableFuture<BatchResponse<Transaction.SubmitResponse>> submit(final List<Transaction.Builder> msg) {
        return CompletableFuture.supplyAsync(() -> {
            return sendBatch(msg);
        });
    }

    @SneakyThrows
    BatchResponse<Transaction.SubmitResponse> sendBatch(final List<Transaction.Builder> txBuilders) {
        log.debug("sendBatch: {}", txBuilders);

        final BatchResponse<Transaction.Template> buildTxBatch = Transaction.buildBatch(client, txBuilders);
        Assert.isTrue(buildTxBatch.errors().isEmpty(), "Errors in build template");
        final BatchResponse<Transaction.Template> signTxBatch = HsmSigner.signBatch(buildTxBatch.successes());
        Assert.isTrue(signTxBatch.errors().isEmpty(), "Errors in sign template");

        final BatchResponse<Transaction.SubmitResponse> submitTxBatch = Transaction.submitBatch(client, signTxBatch.successes());
        if (!submitTxBatch.errors().isEmpty()) {
            for (Map.Entry<Integer, APIException> err : submitTxBatch.errorsByIndex().entrySet()) {
                System.out.println("Error submitting transaction " + err.getKey() + ": " + err.getValue());
            }
        }
        Assert.isTrue(submitTxBatch.errors().isEmpty(), "Errors in submit template");

        return submitTxBatch;
    }
}
