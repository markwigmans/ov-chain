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
package com.ximedes.ov.serializer;

import akka.serialization.JSerializer;
import com.chain.google.gson.Gson;
import com.chain.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by mawi on 01/12/2016.
 */
@Slf4j
public class GsonSerializer extends JSerializer {

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Override
    public int identifier() {
        return 1034;
    }

    @Override
    public byte[] toBinary(Object o) {
        return gson.toJson(o).getBytes();
    }

    @Override
    public boolean includeManifest() {
        return true;
    }

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        final String content = new String(bytes);
        try {
            return gson.fromJson(content, manifest);
        } catch (Exception e) {
            log.error("fromBinaryJava: '{}' ; error:[{}]", content, e.getMessage());
            throw e;
        }
    }
}
