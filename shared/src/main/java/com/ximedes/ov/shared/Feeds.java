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

import com.chain.api.Transaction;
import com.chain.exception.ChainException;
import com.chain.http.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
@Lazy
@Slf4j
public class Feeds {

    private final Client client;

    /**
     * Auto wired constructor
     */
    @Autowired
    public Feeds(Client client) {
        this.client = client;
    }

    public Transaction.Feed getFeed(final String alias, final String filter) throws ChainException {
        Transaction.Feed feed = findByAlias(alias);
        if (feed == null) {
            feed = Transaction.Feed.create(client, alias, filter);
        }
        return feed;
    }

    public Transaction.Feed getFeed(final String alias) throws ChainException {
        return getFeed(alias, null);
    }

    Transaction.Feed findByAlias(final String alias) throws ChainException {
        try {
            return Transaction.Feed.getByAlias(client, alias);
        } catch (Exception  e) {
            log.debug("Exception", e);
        }
        return null;
    }
}
