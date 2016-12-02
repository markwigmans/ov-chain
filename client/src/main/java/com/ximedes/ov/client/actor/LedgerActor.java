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
package com.ximedes.ov.client.actor;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.chain.api.Transaction;
import com.chain.exception.ChainException;
import com.ximedes.ov.client.service.LedgerService;
import lombok.Value;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 *
 */
class LedgerActor extends AbstractLoggingActor {

    private final LedgerService ledgerService;

    private final Queue<Transaction.Builder> queue = new LinkedBlockingQueue();

    /**
     * Create Props for an actor of this type.
     */
    public static Props props(final LedgerService ledgerService) {
        return Props.create(LedgerActor.class, ledgerService);
    }

    private LedgerActor(final LedgerService ledgerService) {
        this.ledgerService = ledgerService;

        receive(ReceiveBuilder
                .match(Tick.class, this::scheduler)
                .match(Transaction.Builder.class, this::submit)
                .matchAny(this::unhandled)
                .build());
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        scheduleCall();
    }

    /**
     * scheduler called, send all transactions to the ledger
     */
    private void scheduler(Tick tick) {
        if (!queue.isEmpty()) {

            final List<Transaction.Builder> data = new ArrayList<>(queue.size());
            data.addAll(queue);

            // process the queue per batchsize elements
            final int BatchSize = 10;
            IntStream.range(0, (queue.size() + BatchSize - 1) / BatchSize)
                    .mapToObj(i -> data.subList(i * BatchSize, Math.min(data.size(), (i + 1) * BatchSize)))
                    .forEach(batch -> ledgerService.submit(self(), batch, "1"));

            queue.clear();
        }

        // schedule next call
        scheduleCall();
    }

    private void submit(Transaction.Builder msg) throws ChainException {
        queue.add(msg);
    }

    private void scheduleCall() {
        getContext().system().scheduler().scheduleOnce(
                Duration.create(1000, TimeUnit.MILLISECONDS),
                self(), new Tick(), getContext().dispatcher(), ActorRef.noSender());
    }

    @Value
    public static final class Tick implements Serializable {
    }
}
