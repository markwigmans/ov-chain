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
package com.ximedes.ov.backend;

import akka.actor.ActorSystem;
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

/**
 * Configuration / Bean definitions
 */
@Configuration
public class BackendConfig {

    @Value("${clustering.hostname:127.0.0.1}")
    private String hostName;
    @Value("${clustering.port:2550}")
    private int port;

    @Getter
    @Value("${actor.creation.timeout.s:60}")
    private int creationTimeout;

    @Bean
    ActorSystem actorSystem() throws UnknownHostException {
        final Map<String, Object> options = new HashMap<>();
        options.put("akka.cluster.roles", Arrays.asList(ClusterConstants.BACKEND));
        options.put(String.format("akka.cluster.role.%s.min-nr-of-members", ClusterConstants.BACKEND), Integer.toString(1));
        options.put(String.format("akka.cluster.role.%s.min-nr-of-members", ClusterConstants.FRONTEND), Integer.toString(1));

        options.put("akka.cluster.seed-nodes", Arrays.asList(String.format("akka.tcp://%s@%s:%d", ClusterConstants.CLUSTER, hostName, port)));

        options.put("akka.remote.netty.tcp.hostname", hostName);
        options.put("akka.remote.netty.tcp.port", Integer.toString(port));
        options.put("akka.remote.netty.tcp.bind-hostname", "0.0.0.0");
        options.put("akka.remote.netty.tcp.bind-port", 2550);

        final Config config = ConfigFactory.parseMap(options).withFallback(ConfigFactory.load());
        return ActorSystem.create(ClusterConstants.CLUSTER, config);
    }
}
