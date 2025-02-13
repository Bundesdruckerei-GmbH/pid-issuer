# Revocation Service (REVOC) Backend

## Description
The revocation service backend provides an API to revoke issued PIDs. It holds status information about issued PIDs and updates statuses on the Status List Service in case of revocation. The revocation process is secured by and the user identified by eID.

## Getting Started
### Prerequisites
In order to use the Revocation Service, it requires the following dependent services to be running and be configured.
- Postgres database (initialized with the Flyway scripts located in [migrations](db/migrations))
- RabbitMQ
- Status List Service
- eID-Service

For local development a [Docker compose](compose.yaml) file is provided which starts the required services on application startup. The eID-Service is provided in the form of a mock.

The default configuration for these services can be changed by setting the following application properties:
```yaml
spring:
  datasource:
    # datasource configuration
  rabbitmq:
    # RabbitMQ configuration
statuslistservice:
  restclient:
    # Status List Service configuration
revocation:
  identification:
    # eID identification configuration
```

### Installation
The installation is performed via the Maven Wrapper:
```shell
./mvnw install
```

Without running the tests:
```shell
./mvnw install -DskipTests
```

### Start
The Revocation Service backend can be started via Maven and Spring:
```shell
./mvnw spring-boot:run
```

### Test
The service provides JUnit unit tests and integration tests using SpringBoot and RestAssured to ensure that the code works as expected. The tests are executed in the installation process and can also be executed directly via Maven:
```shell
./mvnw test
```

Beyond that there are more integration tests using Karate, that need to be executed separately. More information can be found in the corresponding [README](integration-tests/README.adoc).

## Usage
The revocation service backend offers the following APIs:
- [Revocation-API](docs/api/revocation-service.openapi.yaml)
- [Identification-API](docs/api/revocation-service.identification.openapi.yaml)
- [Issuing-AsyncAPI](docs/api/issuer-info-asyncapi.yml)

The Revocation-API offers secured endpoints to view the number issued and valid PIDs and to revoke all PIDs.

The Identification-API offers endpoints to go through the eID identification in interaction with the AusweisApp2 and provide a session with a session token to use the Revocation-API.

The Issuing-AsyncAPI is an internal API to provide information of issued PIDs to the Revocation-Service which will be required in case of a revocation. It should not be made accessible to the public.
