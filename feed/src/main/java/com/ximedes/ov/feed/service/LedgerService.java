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
package com.ximedes.ov.feed.service;

import com.chain.api.Transaction;
import com.chain.exception.ChainException;
import com.chain.http.Client;
import com.ximedes.ov.feed.FeedConfig;
import com.ximedes.ov.shared.Feeds;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
@Service
@Slf4j
public class LedgerService {

    private final Transaction.Feed feed;
    private final ExecutorService pool;

    /**
     * autowired constructor
     */
    public LedgerService(Client client, FeedConfig config, Feeds feeds) throws ChainException {
        this.feed = feeds.getFeed(config.getFeedAlias());
        pool = Executors.newSingleThreadExecutor();
        pool.submit(() -> processingLoop(client));
    }

    @PreDestroy
    public void stop() {
        pool.shutdown();
    }

    void processingLoop(final Client client) {
        while (true) {
            try {
                final Transaction tx = feed.next(client);
                processTransaction(tx);
                feed.ack(client);
            } catch (com.chain.exception.HTTPException e) {
                log.trace("timeout");
            } catch (Exception e) {
                log.error("processingLoop exception", e);
            }
        }
    }

    void processTransaction(Transaction tx) {
        log.info("New transaction at " + tx.timestamp);
        log.info("\tID: " + tx.id);

        for (int i = 0; i < tx.inputs.size(); i++) {
            Transaction.Input input = tx.inputs.get(i);
            log.info("\tInput {}", i);
            log.info("\t\tType: {}", input.type);
            log.info("\t\tAsset: {}", input.assetAlias);
            log.info("\t\tAmount: {}", input.amount);
            log.info("\t\tAccount: {}", StringUtils.defaultString(input.accountAlias));
        }

        for (int i = 0; i < tx.outputs.size(); i++) {
            Transaction.Output output = tx.outputs.get(i);
            log.info("\tOutput {}", i);
            log.info("\t\tType: {}", output.type);
            log.info("\t\tPurpose: {}", StringUtils.defaultString(output.purpose));
            log.info("\t\tAsset: {}", output.assetAlias);
            log.info("\t\tAmount: {}", output.amount);
            log.info("\t\tAccount: {}", StringUtils.defaultString(output.accountAlias));
        }
    }
}
