## Deploy the GRPC Enrichment Service Using ECS

Your AWS CLI must be installed and configured. See https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html 
for details.  The configured user must have sufficient privileges.  The PowerUser policy is not sufficient because 
the command needs to create IAMRoles.  The root user, or a user with AdminAccess will work.  A detailed list of 
required permissions is here: https://docs.docker.com/cloud/ecs-integration/#run-an-application-on-ecs .

If you don't already have a docker context for Amazon ECS, create an ECS context: `docker context create ecs myecscontext`.
You can of course name the context something other than "myecscontext".

Make sure `enrichment-service.jar` exists in `enrichment-service-build-context`.  If it does not, 
run `mvn clean package` in the project root directory.

Build the Docker image for the enrichment service. Note that the command below uses Docker's buildx command 
to create an image for a specified platform, regardless of the build platform.  This is necessary if, for 
example you are building on an M1 Mac. 

```bash
# if you have switched contexts, make sure you are using your local context
docker context use default
docker buildx build --platform linux/amd64 -t enrichment_service_amd64 enrichment-service-build-context
```

In the AWS Console, go to the ECR service and create a new private registry called "enrichment_service". Select 
that repository and click the "View push commands" button. Follow those instructions to upload the image to the 
private repository in your AWS account.  Note that, in your local repository, the image is called 
"enrichment_service_amd64", not "enrichment_service".  You will need to adjust the "docker tag" command 
accordingly.

Edit `compose.yaml` to refer to the name of the image you have just pushed.

Change to the ecs context back to ecs and start the project
```bash
docker contest use myecscontext
docker compose up
```

After this works, run `docker compose ps` to see the host and port that the service is running on.  For example:
`virid-LoadB-TYHDGKUUJ2DI-f131f1a59fb010d8.elb.us-east-2.amazonaws.com:50051`.

Later, when you are done with the service, use `docker compose down` to undeploy it.






