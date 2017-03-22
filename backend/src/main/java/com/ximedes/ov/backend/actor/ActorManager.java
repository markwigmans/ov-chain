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
package com.ximedes.ov.backend.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import com.ximedes.ov.backend.BackendConfig;
import com.ximedes.ov.shared.ClusterConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by mawi on 13/11/2015.
 */
@Component("backendActorManager")
class ActorManager {

    /**
     * Auto wired constructor
     */
    @Autowired
    ActorManager(final ActorSystem system, final BackendConfig config) throws Exception {
        final Timeout timeout = Timeout.apply(config.getCreationTimeout(), TimeUnit.SECONDS);

        final ActorRef supervisor = system.actorOf(Supervisor.props(), "supervisor");

        final ActorRef idGenerator = (ActorRef) PatternsCS.ask(supervisor, new Supervisor.NamedProps(IdGenerator.props(),
                "idGenerator"), timeout).toCompletableFuture().get();

        // create
        system.actorOf(ClusterManager.props(idGenerator), ClusterConstants.BACKEND);
    }
}
