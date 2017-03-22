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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.PatternsCS;
import akka.routing.RoundRobinPool;
import akka.util.Timeout;
import com.ximedes.ov.actor.ClusterActorProxy;
import com.ximedes.ov.client.ClientConfig;
import com.ximedes.ov.shared.ClusterActorType;
import com.ximedes.ov.shared.ClusterConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import com.ximedes.ov.actor.*;

/**
 *
 */
@Component("frontendActorManager")
@Slf4j
public class ActorManager {

    @Getter
    private final ActorRef idGenerator;
    @Getter
    private final ActorRef resetActor;
    @Getter
    private final ActorRef ledgerActor;

    /**
     * Auto wired constructor
     */
    ActorManager(final ActorSystem system, final ClientConfig config) throws Exception {
        final Timeout timeout = Timeout.apply(config.getCreationTimeout(), TimeUnit.SECONDS);

        final ActorRef supervisor = system.actorOf(Supervisor.props(), "supervisor");

        final ActorRef idActor = (ActorRef) PatternsCS.ask(supervisor, new Supervisor.NamedProps(ClusterActorProxy.props(ClusterActorType.ID_GENERATOR),
                "proxy-idActor"), timeout).toCompletableFuture().get();
        this.idGenerator = (ActorRef) PatternsCS.ask(supervisor, new Supervisor.NamedProps(new RoundRobinPool(config.getLocalIdActorPool())
                .props(IdGenerator.props(idActor, config.getIdPoolSize())),
                "idGeneratorRouter"), timeout).toCompletableFuture().get();

        this.resetActor = (ActorRef) PatternsCS.ask(supervisor, new Supervisor.NamedProps(ResetActor.props(),
                "resetActor"), timeout).toCompletableFuture().get();

        final ActorRef proxyLedgerActor = (ActorRef) PatternsCS.ask(supervisor,
                new Supervisor.NamedProps(ClusterActorProxy.props(ClusterActorType.BLOCKCHAIN_PROXY),
                "proxy-ledgerActor"), timeout).toCompletableFuture().get();
        this.ledgerActor = (ActorRef) PatternsCS.ask(supervisor, new Supervisor.NamedProps(LedgerActor.props(proxyLedgerActor),
                "ledgerActor"), timeout).toCompletableFuture().get();

        // register frontend to the cluster
        system.actorOf(ClusterManager.props(idActor, proxyLedgerActor), ClusterConstants.FRONTEND);
    }
}
