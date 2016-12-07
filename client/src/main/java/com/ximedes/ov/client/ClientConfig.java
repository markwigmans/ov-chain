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
package com.ximedes.ov.client;

import akka.actor.ActorSystem;
import akka.util.Timeout;
import com.chain.exception.ChainException;
import com.chain.http.Client;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.ximedes.ov.shared.ClusterConstants;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Configuration / Bean definitions
 */
@Configuration
public class ClientConfig {

    @Getter
    @Value("${key.alias:ovchain-client}")
    private String keyAlias;

    @Getter
    @Value("${actor.idActor.pool:8}")
    private int localIdActorPool;

    @Getter
    @Value("${id.pool:64}")
    private int idPoolSize;

    /**
     * Default timeout for processing calls.
     */
    @Value("${actor.ask.timeout.ms:5000}")
    private int timeout;
    @Getter
    @Value("${actor.creation.timeout.s:60}")
    private int creationTimeout;

    @Value("${seed.hostname:127.0.0.1}")
    private String seedHostName;
    @Value("${seed.port:2550}")
    private int seedPort;
    @Value("${clustering.hostname:}")
    private String hostName;
    @Value("${clustering.port:2551}")
    private int port;

    @Bean
    Client client() throws ChainException {
        return new Client();
    }

    @Bean
    ActorSystem actorSystem() throws UnknownHostException {
        final Map<String, Object> options = new HashMap<>();
        options.put("akka.cluster.roles", Arrays.asList(ClusterConstants.FRONTEND));
        options.put(String.format("akka.cluster.role.%s.min-nr-of-members", ClusterConstants.BACKEND), Integer.toString(1));
        options.put(String.format("akka.cluster.role.%s.min-nr-of-members", ClusterConstants.FRONTEND), Integer.toString(1));

        options.put("akka.cluster.seed-nodes", Arrays.asList(String.format("akka.tcp://%s@%s:%d", ClusterConstants.CLUSTER, seedHostName, seedPort)));
        options.put("akka.remote.netty.tcp.hostname", hostName);
        options.put("akka.remote.netty.tcp.port", Integer.toString(port));
        options.put("akka.remote.netty.tcp.bind-hostname", "0.0.0.0");
        options.put("akka.remote.netty.tcp.bind-port", 2551);

        final Config config = ConfigFactory.parseMap(options).withFallback(ConfigFactory.load());
        return ActorSystem.create(ClusterConstants.CLUSTER, config);
    }

    @Bean
    Timeout timeout() {
        return Timeout.apply(timeout, TimeUnit.MILLISECONDS);
    }
}
