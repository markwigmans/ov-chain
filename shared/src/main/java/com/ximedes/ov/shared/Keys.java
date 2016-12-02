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

import com.chain.api.MockHsm;
import com.chain.exception.ChainException;
import com.chain.http.Client;
import com.chain.signing.HsmSigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class Keys {

    @Autowired
    private Client client;

    public MockHsm.Key getKey(final String alias) throws ChainException {
        MockHsm.Key key = findByAlias(alias);
        if (key == null) {
            key = MockHsm.Key.create(client, alias);
            HsmSigner.addKey(key, MockHsm.getSignerClient(client));
        }
        return key;
    }

    MockHsm.Key findByAlias(final String alias) throws ChainException {
        final MockHsm.Key.Items items = new MockHsm.Key.QueryBuilder().addAlias(alias).execute(client);
        return items.hasNext() ? items.next() : null;
    }
}
