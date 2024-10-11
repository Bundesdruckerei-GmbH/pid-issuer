# PID Issuer

This document contains the instructions to build the PID Issuer from the open source delivery.

> **_NOTE:_**  Commands in this document where tested on ubuntu linux.

## Prerequisites

1. The hostname `pidi.localhost.bdr.de` needs to point to `localhost`. In most cases this domain should be resolved by
   your dns provider. Some provider don't resolve domains which point to `localhost`. A different solution is needed
   then, e.g. add it to the local `hosts`file.
2. Java 21 needs to be installed. This instruction is tested with eclipse temurin 21.0.3 installed
   by [sdk man](https://sdkman.io/) on ubuntu linux.
3. Docker and docker compose need to be installed and running. The current user needs access to it, e.g. needs to be in
   the correct group.

## Building the dependency mdoc-sdk-0.19.0

Enter the directory `mdoc-sdk-0.19.0` and run the following command: `./gradlew build publishToMavenLocal`.

## Building the dependency openid4vc-libraries-0.14.6

Enter the directory `openid4vc-libraries-0.14.6` and run the following command: `./gradlew build publishToMavenLocal`.

### Configuring the eID integration

The PID Issuer is a [Spring Boot](https://docs.spring.io/spring-boot/index.html) application. There
are [several ways to configure it](https://docs.spring.io/spring-boot/reference/features/external-config.html).
In this documentation we use environment variables.
Key material should be placed in the folder `pidi-release-1.5.0/app/eid`. In this example the keys have the names from
the governikus pandstar sdk samples.

Open `pidi-release-1.5.0/app/compose.yaml` and edit the following properties:

```yaml
services:
  pidi.localhost.bdr.de:
    # skipped properties
    environment:
      # skipped keys
      # Configuration of eid identification, settings fit to the governikus sdk samples
      # x.509 signature certificate of the eID provider (used for checking the signature of the eID providers SAML Authentication Responses)
      - PIDI_IDENTIFICATION_SERVER_CERTIFICATESIGPATHS=/opt/keys/test/panstar-signature.cer
      # x.509 encryption certificate of the eID provider (used for encrypting your SAML Authentication Requests to the eID provider)
      - PIDI_IDENTIFICATION_SERVER_CERTIFICATEENCPATH=/opt/keys/test/panstar-encryption.cer
      # URL of the eID providers endpoint receiving SAMLAuthenticationRequests
      - PIDI_IDENTIFICATION_SERVER_URL=https://dev.id.governikus-eid.de/gov_autent/async
      # Path of the PKCS#12 keystore containing your key and certificate for signing the SAML Authentication Requests.
      - PIDI_IDENTIFICATION_XMLSIGKEYSTORE_PATH=/opt/keys/test/Governikus_GmbH_&_Co._KG_Localhost_SAML_Signature_620935.p12
      # Alias of your signing key and certificate in the keystore
      - PIDI_IDENTIFICATION_XMLSIGKEYSTORE_ALIAS=saml-signature
      # Password for the keystore and signing key (must be equal)
      - PIDI_IDENTIFICATION_XMLSIGKEYSTORE_PASSWORD=620935
      # Path of the PKCS#12 keystore containing your key and certificate for decrypting the SAML Authentication Responses
      - PIDI_IDENTIFICATION_XMLENCKEYSTORE_PATH=/opt/keys/test/Governikus_GmbH_&_Co._KG_Localhost_SAML_Encryption_466035.p12
      # Alias of your encryption key and certificate in the keystore
      - PIDI_IDENTIFICATION_XMLENCKEYSTORE_ALIAS=saml-encryption
      # Password for the keystore and encryption key (must be equal)
      - PIDI_IDENTIFICATION_XMLENCKEYSTORE_PASSWORD=466035
      # The identifier to be used for the ProviderName field of the SAML Authentication Request.
      - PIDI_IDENTIFICATION_SERVICEPROVIDERNAME=https://localhost:8443
```

### Building

Once you have configured the eID integration, the PID Issuer can be built with the
actual eID integration. To build the PID issuer with activated eID integration,
change directory to `pidi-release-1.5.0\app` and
run the following command: `./mvnw -P'!full' verify`.

### Running

The PID Issuer requires a Postgres database for its execution. A suitable database
container image and migration scripts for creating the required tables are provided
as a docker compose setup.

The docker compose setup also contains the [AusweisApp container](https://www.ausweisapp.bund.de/sdk/container.html)
so the eID integration can be tested.

To start the whole stack, enter directory `pidi-release-1.5.0\app`
and run the following commands:

```
docker compose --profile fullStack up
```

### Running the integration tests

The PID Issuer source code is shipped with several integration tests which run against a deployed version of the issuer.
To run these the issuer should be started by docker compose, like described before.

To start thetests, enter directory `pidi-release-1.5.0\app`
and run the following commands:

```
./mvnw -P'!full' verify -D'test.groups=remote'
```