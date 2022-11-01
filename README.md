# Overview
This is a Docker based example of accessing a grpc service from a Hazelcast Pipeline. It is modified from
https://github.com/hazelcast/hazelcast-code-samples/tree/master/jet/grpc by pulling each component into its 
own container.

Each component is a service in the `compose.yaml` file.  They are:

| Component          | Description                                                                                                                                                 |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| enrichment-service | The GRPC service that is called from the Jet Pipeline                                                                                                       |
| event-generator    | Generates simulated "trade events by putting entries into the "trades" table                                                                                |
| hazlecast          | A single Hazelcast 5.2 instance running the "vanilla" docker image. Note that the enrichment service stub and the Pipeline are not on the startup classpath |
| mancenter          | The Hazelcast 5.2  management center, exposed on localhost:8080                                                                                             |
| hazelcast-shell    | This container is used to deploy the Jet job or run other interactive commands.                                                                             |

 The overall topology looks like this:
 
```
 event-generator -----> 
 mancenter  ----------> hazelcast ----> enrichment-service
 hazelcast-shell ----->
```

# Pre-requisites

Docker Desktop 
maven

This was tested on an M1 Macbook Pro running MacOS Ventura 

# Instructions 

1. Build: `mvn clean package` 
2. Run: `docker compose up -d`
3. Verify that all processes are running using `docker compose ps`
4. Management center can be accesses at `localhost:8080`.  Verify that there are puts on the `trades` map
5. Submit the job: `submitjob.sh`
6. You should now see the job running in mancenter
7. Additionally, there should be log output from the pipeline running in the cluster: `docker compose logs --follow hazelcast`

If you wish to run any command using the Hazelcast shell, you can do so with the following command:
`docker compose run hazelcast-shell /opt/hazelcast/bin/hz-cli ...`






