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
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.google.protobuf.TextFormat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ximedes.ov.protocol.BackendProtocol.*;
import static com.ximedes.ov.protocol.SimulationProtocol.Reset;
import static com.ximedes.ov.protocol.SimulationProtocol.Reseted;

/**
 * handles the requests for ID ranges
 */
public class IdGenerator extends AbstractLoggingActor {

    private int watermark;

    /**
     * Create Props for an actor of this type.
     */
    public static Props props() {
        return Props.create(IdGenerator.class);
    }

    private IdGenerator() {
        init();

        receive(ReceiveBuilder
                .match(IdsRequest.class, this::idsRequest)
                .match(Reset.class, this::reset)
                .matchAny(this::unhandled)
                .build());
    }

    private void init() {
        watermark = 0;
    }

    private void idsRequest(final IdsRequest request) {
        log().debug("idRangeRequest: '{}'", TextFormat.shortDebugString(request));
            IdsResponse message = createResponse(watermark, request.getNumber());
            sender().tell(message, self());
            watermark += request.getNumber();
    }

    IdsResponse createResponse(final int start, final int count) {
        log().debug("createResponse({},{})'", start, count);
        final IdsResponse.Builder builder = IdsResponse.newBuilder().
                addAllIds(IntStream.range(start, start + count).boxed().collect(Collectors.toList()));
        return builder.build();
    }

    // reset the simulation
    void reset(final Reset message) {
        log().info("reset()");
        init();
        sender().tell(Reseted.getDefaultInstance(), self());
    }
}
