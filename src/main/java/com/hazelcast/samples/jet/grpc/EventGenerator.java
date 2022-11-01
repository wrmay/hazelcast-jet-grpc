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

import com.hazelcast.samples.jet.grpc.datamodel.Trade;

import java.util.Random;

/**
 * This will be used as a context for a stream source.  It is not safe for concurrent use.
 */
public class EventGenerator  {

    private static final int PRODUCT_ID_BASE = 31;
    private static final int BROKER_ID_BASE = 21;
    private static final int PRODUCT_BROKER_COUNT = 4;
    private final Random random;
    private int tradeId;

    EventGenerator() {
        this.random = new Random();
        this.tradeId = 1;
    }

    public Trade[] getRandomTrades(int count){
        Trade []result = new Trade[count];
        for(int i=0;i< count; ++i){
            result[i] = new Trade(tradeId++,
                    PRODUCT_ID_BASE + random.nextInt(PRODUCT_BROKER_COUNT),
                    BROKER_ID_BASE + random.nextInt(PRODUCT_BROKER_COUNT));
        }
        return result;
    }
}
