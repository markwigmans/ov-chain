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

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.Broadcast;
import lombok.EqualsAndHashCode;
import lombok.Value;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.*;

/**
 *
 */
public class Supervisor extends AbstractActor {

    /**
     * Create Props for an actor of this type.
     */
    public static Props props() {
        return Props.create(Supervisor.class, Supervisor::new);
    }

    private static final SupervisorStrategy strategy =
            new OneForOneStrategy(10, Duration.create(1, TimeUnit.MINUTES), DeciderBuilder.
                    match(ArithmeticException.class, e -> resume()).
                    match(NullPointerException.class, e -> restart()).
                    match(IllegalArgumentException.class, e -> stop()).
                    matchAny(o -> escalate()).build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private Supervisor() {
        receive(ReceiveBuilder
                .match(Props.class, props -> sender().tell(context().actorOf(props), self()))
                .match(NamedProps.class, namedProps -> sender().tell(context().actorOf(namedProps.props, namedProps.name), self()))
                .match(Broadcast.class, m -> {
                })    // ignore
                .matchAny(this::unhandled)
                .build()
        );
    }

    @Value
    @EqualsAndHashCode
    public static final class NamedProps implements Serializable {
        Props props;
        String name;
    }
}
