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
import akka.cluster.ClusterEvent;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ximedes.ov.protocol.ClusterProtocol.Registration;

/**
 * Created by mawi on 16/08/2016.
 */
public class ClusterManager extends AbstractLoggingActor {

    private final List<ActorRef> actors;
    private final Cluster cluster;

    /**
     * Create Props for an actor of this type.
     */
    public static Props props(final ActorRef... actors) {
        return Props.create(ClusterManager.class, Arrays.asList(actors));
    }

    private ClusterManager(List<ActorRef> actors) {
        this.actors = new ArrayList<>(actors);
        this.cluster = Cluster.get(getContext().system());

        receive(ReceiveBuilder
                .match(Registration.class, this::Registration)
                .matchAny(this::unhandled)
                .build());
    }

    private void Registration(final Registration message) {
        log().debug("Registration");
        // initialise the rest of the system
        actors.stream().forEach(a -> a.tell(message, self()));
    }

    @Override
    public void preStart() {
        //subscribe to cluster changes, MemberUp
        cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(),
                ClusterEvent.MemberRemoved.class, ClusterEvent.UnreachableMember.class);
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }
}
