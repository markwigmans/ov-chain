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
package com.ximedes.ov.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.ximedes.ov.protocol.ClusterProtocol;
import com.ximedes.ov.shared.ClusterActorType;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 */
public class ClusterActorProxy extends AbstractActorWithUnboundedStash {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    private ActorRef remoteActor;
    private final ClusterActorType actorType;

    private final PartialFunction<Object, BoxedUnit> unconnected;
    private final PartialFunction<Object, BoxedUnit> initialized;

    private String identifyId;

    /**
     * Create Props for an actor of this type.
     */
    public static Props props(final ClusterActorType actorType) {
        return Props.create(ClusterActorProxy.class, actorType);
    }

    private ClusterActorProxy(final ClusterActorType actorType) {
        this.actorType = actorType;

        unconnected = ReceiveBuilder
                .match(ClusterProtocol.Registration.class, this::Registration)
                .match(ActorIdentity.class, this::actorIdentity)
                .matchAny(msg -> stash())
                .build();

        initialized = ReceiveBuilder
                .match(ClusterProtocol.Registration.class, m -> {
                    log.info("already registered");
                })
                .matchAny(this::forward)
                .build();

        // set start state
        context().become(unconnected);
    }

    private void Registration(ClusterProtocol.Registration message) {
        final List<ClusterProtocol.Actor> refs = message.getActorsList()
                .stream()
                .filter(r -> r.getType().equals(actorType.toString()))
                .collect(Collectors.toList());
        if (!refs.isEmpty()) {
            // actorPath found, so let's ask it's identity for it's actorRef
            final String actorPath = refs.get(0).getActorPath();
            log.info("found, path '{}'", actorPath);
            identifyId = UUID.randomUUID().toString();
            getContext().actorSelection(actorPath).tell(new Identify(identifyId), self());
        } else {
            log.debug("Type ({}) not found", actorType);
        }
    }

    private void actorIdentity(final ActorIdentity message) {
        if (message.correlationId().equals(identifyId)) {
            // identity found
            remoteActor = message.getRef();
            unstashAll();
            context().become(initialized);
        } else {
            log.warning("actorIdentity: received:'{}', expected:'{}'", message.correlationId(), identifyId);
        }
    }

    private void forward(final Object message) {
        log.debug("forward ({}) : '{}'", actorType, message);
        remoteActor.forward(message, getContext());
    }
}
