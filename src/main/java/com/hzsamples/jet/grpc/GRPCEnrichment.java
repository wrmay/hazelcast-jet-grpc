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

package com.hzsamples.jet.grpc;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.grpc.GrpcService;
import com.hazelcast.jet.pipeline.*;
import com.hzsamples.jet.grpc.datamodel.Trade;
import io.grpc.ManagedChannelBuilder;

import static com.hazelcast.jet.datamodel.Tuple2.tuple2;
import static com.hazelcast.jet.datamodel.Tuple3.tuple3;
import static com.hazelcast.jet.grpc.GrpcServices.bidirectionalStreamingService;
import static com.hazelcast.jet.grpc.GrpcServices.unaryService;

/**
 * Demonstrates the usage of the Pipeline API to enrich a data stream. We
 * generate a stream of stock trade events and each event has an associated
 * product ID and broker ID. The reference lists of products and brokers
 * are stored in files. The goal is to enrich the trades with the actual
 * name of the products and the brokers.
 * <p>
 * We generate the stream of trade events by updating a single key in the
 * {@code trades} map which has the Event Journal enabled. The event
 * journal emits a stream of update events.
 */
public final class GRPCEnrichment {

    /**
     * Builds a pipeline which enriches the stream with the response from a
     * gRPC service.
     * <p>
     * It starts a gRPC server that will provide product and broker names based
     * on an ID. The job then enriches incoming trades using the service. This
     * sample demonstrates a way to call external service with an async API
     * using the {@link StreamStage#mapUsingServiceAsync}
     * method.
     */
    public static Pipeline enrichUsingGRPC(String enrichmentServiceHost, int enrichmentServicePort) {

        StreamSource<Trade> embeddedEventGenerator = SourceBuilder.stream("embedded event generator", (context) -> new EventGenerator())
                .<Trade>fillBufferFn((eventGen, buffer) -> {
                    for (Trade t : eventGen.getRandomTrades(100)) buffer.add(t);
                })
                .build();

        // The stream to be enriched: trades
        Pipeline p = Pipeline.create();
        StreamStage<Trade> trades = p.readFrom(embeddedEventGenerator).withoutTimestamps();

        ServiceFactory<?, ? extends GrpcService<ProductInfoRequest, ProductInfoReply>> productService = unaryService(
                () -> ManagedChannelBuilder.forAddress(enrichmentServiceHost, enrichmentServicePort).useTransportSecurity().usePlaintext(),
                channel -> ProductServiceGrpc.newStub(channel)::productInfo
        );

        ServiceFactory<?, ? extends GrpcService<BrokerInfoRequest, BrokerInfoReply>> brokerService =
                bidirectionalStreamingService(
                        () -> ManagedChannelBuilder.forAddress(enrichmentServiceHost, enrichmentServicePort).usePlaintext(),
                        channel -> BrokerServiceGrpc.newStub(channel)::brokerInfo
                );

        // Enrich the trade by querying the product and broker name from the gRPC services
        trades.mapUsingServiceAsync(productService,
                (service, trade) -> {
                    ProductInfoRequest request = ProductInfoRequest.newBuilder().setId(trade.productId()).build();
                    return service.call(request).thenApply(productReply -> tuple2(trade, productReply.getProductName()));
                })
              // input is (trade, product)
              .mapUsingServiceAsync(brokerService,
                      (service, t) -> {
                          BrokerInfoRequest request = BrokerInfoRequest.newBuilder().setId(t.f0().brokerId()).build();
                          return service.call(request)
                                        .thenApply(brokerReply -> tuple3(t.f0(), t.f1(), brokerReply.getBrokerName()));
                      })
              // output is (trade, productName, brokerName)
              .writeTo(Sinks.logger());
        return p;
    }

    /**
     *
     * @param args args[0] the enrichment service host args[1] the enrichment service port
     */
    public static void main(String[] args) {
        if (args.length != 2){
            System.err.println("Please pass in the enrichment service host and port " +
                    "as the first and second command line argument");
            System.exit(1);
        }

        String enrichmentServiceHost = args[0];
        int enrichmentServicePort = 0;
        try {
            enrichmentServicePort = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfx){
            System.err.println("The second command line argument could not be parsed as an integer port number");
            System.exit(0);
        }


        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        JetService jet = hz.getJet();
        jet.newJob(GRPCEnrichment.enrichUsingGRPC(enrichmentServiceHost, enrichmentServicePort));
        //new GRPCEnrichment(hz).go();
    }
}
