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

/**
 *
 */
class LedgerActor extends AbstractLoggingActor {

    private final ActorRef backendLedger;

    /**
     * Create Props for an actor of this type.
     */
    public static Props props(final ActorRef backendLedger) {
        return Props.create(LedgerActor.class, backendLedger);
    }

    private LedgerActor(final ActorRef backendLedger) {
        this.backendLedger = backendLedger;

        receive(ReceiveBuilder
                .match(Transaction.Builder.class, this::submit)
                .matchAny(this::unhandled)
                .build());
    }

    private void submit(Transaction.Builder msg) throws ChainException {
       backendLedger.tell(msg, self());
    }
}
