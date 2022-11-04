#!/bin/zsh
cd hazelcast-cloud-sql-sample-client-*
~/opt/hazelcast/hazelcast-enterprise-5.1.3/bin/hz-cli -f hazelcast-client-with-ssl.yml  submit \
  -c com.hzsamples.jet.grpc.GRPCEnrichment \
  ../enrichment-service-build-context/enrichment-service.jar \
  virid-LoadB-TYHDGKUUJ2DI-f131f1a59fb010d8.elb.us-east-2.amazonaws.com 50051
