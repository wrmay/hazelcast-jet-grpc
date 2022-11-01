package com.hazelcast.samples.jet.grpc;

import com.hazelcast.samples.jet.grpc.datamodel.Broker;
import com.hazelcast.samples.jet.grpc.datamodel.Product;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Stream;

import static com.hazelcast.jet.Util.entry;
import static java.util.stream.Collectors.toMap;

public class EnrichmentServer {

    /**
     *
     * @param args a single argument designating the port to bind
     */
    public static void main(String []args){
        if (args.length != 1){
            System.err.println("Please provide the port number to bind as the only command line argument");
            System.exit(0);
        }
        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        } catch(NumberFormatException x){
            System.err.println("The port argument could not be parsed as an integer");
            System.exit(0);
        }

        Map<Integer, Product> productMap = readLines("products.txt")
                .collect(toMap(Map.Entry::getKey, e -> new Product(e.getKey(), e.getValue())));
        Map<Integer, Broker> brokerMap = readLines("brokers.txt")
                .collect(toMap(Map.Entry::getKey, e -> new Broker(e.getKey(), e.getValue())));

        Server server = ServerBuilder.forPort(port)
                .addService(new ProductServiceImpl(productMap))
                .addService(new BrokerServiceImpl(brokerMap))
                .build();
        try {
            server.start();
        } catch(IOException iox){
            System.err.println("Server failed to start");
            iox.printStackTrace(System.err);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        System.out.println("*** Server started, listening on " + port);

        while(!Thread.currentThread().isInterrupted()){
            try {
                Thread.sleep(2000);
            } catch(InterruptedException ix){
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("*** Server is shutting down");
    }
    private static Stream<Map.Entry<Integer, String>> readLines(String file) {
        try {
            InputStream stream = GRPCEnrichment.class.getResourceAsStream("/" + file);
            assert stream != null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            return reader.lines().map(EnrichmentServer::splitLine);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map.Entry<Integer, String> splitLine(String e) {
        int commaPos = e.indexOf(',');
        return entry(Integer.valueOf(e.substring(0, commaPos)), e.substring(commaPos + 1));
    }
}
