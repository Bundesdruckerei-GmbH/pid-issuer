# PID Issuer

This document contains the instructions to build the PID Issuer from the source delivery.

## Prerequisites

1. The hostname `pidi.localhost.bdr.de` needs to point to `localhost`.
2. Java 21 needs to be installed.
3. Docker and docker compose need to be installed.
4. For revocation of PIDs npm need to be installed. The [AusweisApp-2](https://www.ausweisapp.bund.de/en/home) should run on the same device.

This instruction is testet with eclipse temurin 21.0.3 installed by [sdk man](https://sdkman.io/) on ubuntu linux, docker 27.4.0 and the latest lts version 10.9.2 of npm.

## Configuring the eID integration
Open `pidi-release-1.6.1/app/src/main/resources/application.properties` and edit the following properties:

Configuration provided by the eID provider:
* **pidi.identification.server.url**\
  URL of the eID providers endpoint receiving SAMLAuthenticationRequests
* **pidi.identification.service-provider-name**\
  The identifier to be used for the ProviderName field of the SAML Authentication Request.
* **pidi.identification.server.certificate-sig-paths**\
  x.509 signature certificate of the eID provider (used for checking the signature of the eID providers SAML Authentication Responses)
* **pidi.identification.server.certificate-enc-path**\
  x.509 encryption certificate of the eID provider (used for encrypting your SAML Authentication Requests to the eID provider)

Configuration of your keystore to be used for signing SAML Authentication Requests:
* **pidi.identification.xmlsig-keystore.path**\
  Path of the PKCS#12 keystore containing your key and certificate for signing the SAML Authentication Requests.
* **pidi.identification.xmlsig-keystore.alias**\
  Alias of your signing key and certificate in the keystore.
* **pidi.identification.xmlsig-keystore.password**\
  Password for the keystore and signing key (must be equal).

Configuration of your keystore to be used for decrypting SAML Authentication Responses:
* **pidi.identification.xmlenc-keystore.path**\
  Path of the PKCS#12 keystore containing your key and certificate for decrypting the SAML Authentication Responses.
* **pidi.identification.xmlenc-keystore.alias**\
  Alias of your encryption key and certificate in the keystore.
* **pidi.identification.xmlenc-keystore.password**\
  Password for the keystore and encryption key (must be equal).

eID is also integrated in revocation-backend. Open `revocation-backend-1.6.1/src/main/resources/application.yaml`
and change the same properties (not with prefix `pidi` but with prefix `revocation`).

## Building the dependency mdoc-sdk-0.20.0-RC4

Enter the directory `mdoc-sdk-0.20.0-RC4` and run the following command: `./gradlew build publishToMavenLocal`.

## Building the dependency openid4vc-libraries-0.15.1-RC1

Enter the directory `openid4vc-libraries-0.15.1-RC1` and run the following command: `./gradlew build publishToMavenLocal`.

## Building the application status-list-service 0.1.11

Enter the directory `status-list-service-0.1.11` and run the following command: `./gradlew build --exclude-task test`.
For tests with Redis in Docker Container run `./gradlew testRedis test` and for tests with Postgres in Docker Container `./gradlew testPostgres test`.

## Building the application revocation-backend 1.6.1

Enter the directory `revocation-backend-1.6.1` and run the following command: `./mvnw verify -DskipTests`.
For running integration tests the eID integration needs to be configured (see next chapter) in `src/test/resources/application.yaml`
and there must be a running instance of status-list-service.

## Building the application revocation-frontend 1.6.1

Enter the directory `revocation-frontend-1.6.1` and run the following commands: `npm install` and `npm run build`.
For tests run `npm run ng -- test --no-watch --karma-config karma.conf.js --browsers=ChromeHeadlessNoSandbox`.

## Building PID Issuer
Once you have configured the eID integration, the PID Issuer can be built with the
actual eID integration. To build the PID issuer with activated eID integration,
change directory to `pidi-release-1.6.1\app` (sub directory __app__!) and
run the following command: `./mvnw verify -DskipTests`.
For running integration tests the eID integration needs to be configured (see above) in `src/main/resources/application.yaml`
and there must be a running instance of status-list-service.

## Running
The PID Issuer requires a Postgres database and a Rabbit MQ for its execution. The revocation-backend requires a Postgres database and the status-list-service requires a Redis storage.
Suitable container images for Postgres and Rabbit and migration scripts for creating the required tables are provided
as a docker compose setup. To start the container, enter directory `docker-compose`
and run the following commands:

```
docker compose up
```
At first change directory to `status-list-service-0.1.11` and run the command: `./gradlew bootRunRedis`,   
Then change directory to `revocation-backend-1.6.1` and run the command: `./mvnw spring-boot:run`.   
And finally change directory to `pidi-release-1.6.1\app` and run the following command: `./mvnw spring-boot:run`

For revocation of PIDs change directory to `revocation-frontend-1.6.1` and run the command: `npm run start`