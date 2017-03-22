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

import akka.actor.AbstractActorWithUnboundedStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.google.protobuf.TextFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import scala.Option;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.util.List;

import static com.ximedes.ov.protocol.IdProtocol.*;
import static com.ximedes.ov.protocol.SimulationProtocol.Reset;
import static com.ximedes.ov.protocol.SimulationProtocol.Reseted;

/**
 * Cache ID set locally.
 */
class IdGenerator extends AbstractActorWithUnboundedStash {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    private final IdQueue ids;

    private final PartialFunction<Object, BoxedUnit> empty;
    private final PartialFunction<Object, BoxedUnit> initialized;

    /**
     * Create Props for an actor of this type.
     */
    public static Props props(final ActorRef backendIdGenerator, final int idPoolSize) {
        return Props.create(IdGenerator.class, backendIdGenerator, idPoolSize);
    }

    private IdGenerator(final ActorRef backendIdGenerator, final int idPoolSize) {
        this.ids = new IdQueue(backendIdGenerator, self(), idPoolSize);

        empty = ReceiveBuilder
                .match(IdsResponse.class, this::IdsResponse)
                .matchAny(msg -> stash())
                .build();

        initialized = ReceiveBuilder
                .match(IdRequest.class, this::idRequest)
                .match(IdsResponse.class, this::IdsResponse)
                .match(Reset.class, this::reset)
                .matchAny(this::unhandled)
                .build();

        // set start state
        context().become(empty);
    }

    @Override
    public void preStart() throws Exception {
        log.debug("preStart()");
        initQueues();
    }

    private void idRequest(final IdRequest request) {
        boolean successful = ids.requestId(sender());

        // resend while the ID queue is being filed
        if (!successful) {
            stash();
            context().become(empty);
        }
    }

    @Override
    public void preRestart(Throwable reason, Option<Object> message) {
        log.debug("preRestart()", reason);
        // TODO return existing ID's
    }

    private void initQueues() {
        ids.init();
    }

    private void IdsResponse(final IdsResponse response) {
        log.debug("IdRangeResponse: '{}'", TextFormat.shortDebugString(response));
        ids.addIds(response.getIdsList());
        unstashAll();
        context().become(initialized);
    }

    // reset the simulation
    void reset(final Reset message) {
        log.info("reset()");
        initQueues();
        sender().tell(Reseted.getDefaultInstance(), self());
    }

    @Slf4j
    static class IdQueue {
        private final ActorRef actor;
        private final ActorRef self;
        private CircularFifoBuffer ids;
        private final int requestFactor;
        private int batchSize;

        public IdQueue(final ActorRef actor, final ActorRef self, final int batchSize) {
            this.actor = actor;
            this.self = self;
            this.ids = new CircularFifoBuffer(batchSize);
            this.batchSize = batchSize;
            this.requestFactor = 2;
        }

        public void init() {
            log.info("init()");
            ids.clear();
            requestIds(batchSize);

            // TODO race condition if there is still an IdRangeRequest pending (due to a test reset)
        }

        /**
         * @param sender
         * @return {code true} if successful
         */
        public boolean requestId(final ActorRef sender) {
            if (ids.isEmpty()) {
                log.debug("queue still empty");
                return false;
            } else {
                final String id = (String) ids.remove();
                sender.tell(IdResponse.newBuilder().setId(id).build(), self);

                // check if we need more ID's
                if (ids.size() * requestFactor <= batchSize) {
                    // we need more ID's
                    requestIds(batchSize - ids.size());
                }
                return true;
            }
        }

        void requestIds(final int count) {
            log.debug("requestIds({})", count);
            actor.tell(IdsRequest.newBuilder().setNumber(count).build(), self);
        }

        public void addIds(final List<String> list) {
            log.debug("addIds({}), old size ids:{}", list.size(), ids.size());
            list.forEach(ids::add);
        }
    }
}
