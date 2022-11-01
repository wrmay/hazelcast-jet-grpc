#!/bin/zsh

docker compose run hazelcast-shell /opt/hazelcast/bin/hz-cli submit -t hazelcast:5701 \
  -c com.hazelcast.samples.jet.grpc.GRPCEnrichment \
  /opt/project/target/jet-grpc-standalone-1.0-SNAPSHOT.jar enrichment-service 50051
