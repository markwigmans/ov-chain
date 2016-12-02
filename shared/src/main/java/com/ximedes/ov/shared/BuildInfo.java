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
package com.ximedes.ov.shared;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * Show the GIT build information
 */
@Slf4j
@PropertySource(value = "classpath:build.properties", ignoreResourceNotFound = true)
@Configuration
public class BuildInfo {
    @Getter
    @Value("${version:}")
    private String version;

    @Getter
    @Value("${revision:}")
    private String revision;

    @Getter
    @Value("${name:}")
    private String name;

    @Value("${timestamp:}")
    private String timestamp;

    @PostConstruct
    void init() {
        log.info("App: '{}', version: '{}', git: '{}', timestamp: '{}'", name, version, revision, getTimestamp());
    }

    String getTimestamp() {
        if (NumberUtils.isNumber(timestamp)) {
            return new Date(NumberUtils.createLong(timestamp)).toString();
        } else {
            return timestamp;
        }
    }
}
