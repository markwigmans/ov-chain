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
import akka.cluster.Cluster;
import akka.japi.pf.ReceiveBuilder;

import static com.ximedes.ov.protocol.ClusterProtocol.BackendRegistration;

/**
 * Created by mawi on 16/08/2016.
 */
public class ClusterManager extends AbstractLoggingActor {

    private final ActorRef idActor;
    private final Cluster cluster;

    /**
     * Create Props for an actor of this type.
     */
    public static Props props(final ActorRef idActor) {
        return Props.create(ClusterManager.class, idActor);
    }

    private ClusterManager(ActorRef idActor) {
        this.idActor = idActor;
        this.cluster = Cluster.get(getContext().system());

        receive(ReceiveBuilder
                .match(BackendRegistration.class, this::BackendRegistration)
                .matchAny(this::unhandled)
                .build());
    }

    private void BackendRegistration(final BackendRegistration message) {
        log().info("BackendRegistration");
        // initialise the rest of the system
        idActor.tell(message, self());
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }
}
