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

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.japi.pf.ReceiveBuilder;
import com.ximedes.ov.shared.ClusterConstants;

import static com.ximedes.ov.protocol.ClusterProtocol.Actor;
import static com.ximedes.ov.protocol.ClusterProtocol.Registration;
import static com.ximedes.ov.shared.ClusterActorType.ID_GENERATOR;

/**
 * Created by mawi on 16/08/2016.
 */
public class ClusterManager extends AbstractLoggingActor {

    private final Cluster cluster;
    private final ActorRef idGenerator;


    /**
     * Create Props for an actor of this type.
     */
    public static Props props(final ActorRef idGenerator) {
        return Props.create(ClusterManager.class, idGenerator);
    }

    private ClusterManager(final ActorRef idGenerator) {
        this.idGenerator = idGenerator;
        this.cluster = Cluster.get(getContext().system());

        receive(ReceiveBuilder
                .match(ClusterEvent.MemberUp.class, this::memberUp)
                .matchAny(this::unhandled)
                .build());
    }

    private void memberUp(ClusterEvent.MemberUp message) {
        log().info("memberUp : member up: {}", message);
        register(message.member());
    }

    void register(final Member member) {
        log().info("register: roles [{}]", String.join(",", member.getRoles()));

        if (member.hasRole(ClusterConstants.FRONTEND)) {
            final String idActorPath = idGenerator.path().toStringWithAddress(getContext().provider().getDefaultAddress());

            final Registration message = Registration.newBuilder()
                    .addActors(Actor.newBuilder().setType(ID_GENERATOR.toString()).setActorPath(idActorPath).build())
                    .build();

            final String actorPath = String.format("%s/user/%s", member.address(), ClusterConstants.FRONTEND);
            log().info("connect: '{}'", actorPath);
            getContext().actorSelection(actorPath).tell(message, self());
        }
    }

    @Override
    public void preStart() {
        //subscribe to cluster changes, MemberUp
        cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberUp.class);
    }

    @Override
    public void postStop() {
        log().info("postStop()");
        cluster.unsubscribe(self());
    }
}
