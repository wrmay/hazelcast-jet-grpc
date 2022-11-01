/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.samples.jet.grpc;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.samples.jet.grpc.datamodel.Trade;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class EventGenerator extends Thread {

    private static final int PRODUCT_ID_BASE = 31;
    private static final int BROKER_ID_BASE = 21;
    private static final int PRODUCT_BROKER_COUNT = 4;

    private volatile boolean enabled;
    private volatile boolean keepRunning = true;

    private final IMap<Object, Trade> trades;

    EventGenerator(IMap<Object, Trade> trades) {
        this.trades = trades;
    }

    @Override
    public void run() {
        Random rnd = ThreadLocalRandom.current();
        int tradeId = 1;
        while (keepRunning) {
            LockSupport.parkNanos(MILLISECONDS.toNanos(50));
            if (!enabled) {
                continue;
            }
            Trade trad = new Trade(tradeId,
                    PRODUCT_ID_BASE + rnd.nextInt(PRODUCT_BROKER_COUNT),
                    BROKER_ID_BASE + rnd.nextInt(PRODUCT_BROKER_COUNT));
            trades.put(42, trad);
            tradeId++;
        }
    }

    void generateEventsForFiveSeconds() throws InterruptedException {
        enabled = true;
        System.out.println("\nGenerating trade events\n");
        Thread.sleep(5000);
        System.out.println("\nStopped trade events\n");
        enabled = false;
    }

    void shutdown() {
        keepRunning = false;
    }

    public static void main(String []args){
        // The first argument is a cluster member to connect to (e.g. myhost:5701)
        if (args.length == 0){
            System.err.println("As the first argument, please provide a member of the hazelcast cluster to connect to.");
            System.exit(1);
        }

        ClientConfig config = new ClientConfig();
        config.setClusterName("dev");
        config.setInstanceName("event generator");
        config.getNetworkConfig().addAddress(args[0]);
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(config);
        Runtime.getRuntime().addShutdownHook(new Thread(hz::shutdown));

        IMap<Object, Trade> tradeMap = hz.getMap(GRPCEnrichment.TRADES);
        EventGenerator eventGenerator = new EventGenerator(tradeMap);
        eventGenerator.setDaemon(true);
        eventGenerator.enabled = true;  // setting this here only to avoid changing the code that runs in local dev mode
        eventGenerator.start();
    }
}
