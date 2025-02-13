# Status List Service

The status list service is a high throughput HTTP service that serves status lists and allows retrieval of free indices
and updates of taken indices in status lists.

[[_TOC_]]

## Build setup

The gradle script is designed to work without modifications using user defined maven repositories. These can for example
be repositories inside a company network.

To configure the required repositories, the following environment variables can be set:

| Variable                         | Purpose                                                                                                 |
|----------------------------------|---------------------------------------------------------------------------------------------------------|
| `MAVEN_REPO_{n}`                 | The URI of the maven repo to use.                                                                       |
| `MAVEN_REPO_{n}_USER`            | The user to use to access the repo with the given number. Optional. Ignored if the password is not set. |
| `MAVEN_REPO_{n}_PASSWORD`        | The password to use to access the repo with the given number. Optional. Ignored if the user is not set. |
| `MAVEN_PUBLISHING_REPO`          | The URI of the maven repo to publish to. Optional. Publishing is disabled if this is not set.           |
| `MAVEN_PUBLISHING_REPO_USER`     | The user to use for maven publishing. Required if the publishing repo is set.                           |
| `MAVEN_PUBLISHING_REPO_PASSWORD` | The password to use for maven publishing. Required if the publishing repo is set.                       |

`{n}` starts at 0 and all variables up to the first missing number are considered (so if there
is `MAVEN_REPO_0`, `MAVEN_REPO_1` and `MAVEN_REPO_3`, the last one will be ignored because `MAVEN_REPO_2` is missing).

## Getting started

This is an instruction of how to set up your project locally.

### Prerequisites

- The following repositories must be configured to be used by gradle. Use variables as described in [build setup](#build-setup) and add them to your `~/.gradle/gradle.properties`

  - *(recommended)*
    ```
    MAVEN_REPO_{n}=https://partner.bdr.de/artifactory/repo1
    MAVEN_REPO_{n}_USER=p...
    MAVEN_REPO_{n}_PASSWORD=...
    ```
    
  - *(required)*
    ```
    MAVEN_REPO_{n+1}=https://partner.bdr.de/artifactory/ssi-maven
    MAVEN_REPO_{n+1}_USER=p...
    MAVEN_REPO_{n+1}_PASSWORD=...
    ```
  > Instead of your real password, generate an identity token at https://partner.bdr.de/ui/user_profile

- Having Docker installed and running.

### Installation

Carry out the following steps to compile, run and test the application:

1. Clone the repo
   ```sh
   git clone https://gitlab.partner.bdr.de/ssi4de/services/status-list-service.git
   ```
2. Build & start the application (including docker services)
   ```sh
   ./gradlew bootRun --args='--spring.profiles.active=local,postgres'
   ```
   > or
   ```sh
   ./gradlew bootRun --args='--spring.profiles.active=local,redis'
   ```
   > Profile `local` has some configurations for local development and an example pool config to start with.
   > Use profile `postgres` to use a postgres database for persistence (also requires a datasource), `redis` to use a redis.

3. Run tests
   ```sh
   ./gradlew test --rerun
   ```
4. **[IntelliJ only]** Exclude the storage-directory to prevent indexing of the status-lists by opening the context menu of the storage-directory and select *Mark directory as* &rarr; *Excluded*.
   > The name of the storage directory can be found at the configuration property `$.app.storage-directory`, for *local* that is **status-lists**

## Functionality

### Status list pools

The core concept provided by the service is a status list pool. Pools are protected by an API-Key and are independent of
one another. A status list pool is a collection of status lists with same size and bits that are managed together.
Usually each credential type issued by an issuer will use a separate pool, but pools could also be shared across several
credentials if the same settings are used.
Through the API, free indices can be requested from a given pool. Lists are automatically created if all indices from
the lists are used.

### Status lists

Each status lists has a unique, random id that is a UUID.

The status list URI is build from the `public-url` using the pattern `<public-url>/<id>`.

### Status list storage

Status lists and the related data is stored in the Redis in memory database.

### Prefetching

To have references available quickly, without invoking redis everytime, the service will prefetch indices from redis.
Prefetching is configured using a threshold and a capacity value. As soon as the amount of available references drops
below threshold new indices are fetched to have at least capacity references available.

If the service is shutdown regularly, all unused indices are returned back to redis to be reused later. Should the
service crash, the fetched references are lost and will never be used.

### Persistence

To ensure that indices are never used twice the service needs persistence guarantees by redis before using indices.

Readers of this document may read [the redis persistence guide](https://redis.io/docs/management/persistence/) to get a
better understanding about how persistence works in redis.

#### appendfsync: always

When using this method the redis instance is configured to use AOF and appendfsync must be set to always. This will be
checked by the service on startup. The service will fetch indices and use them immediately because redis will persist on
each write. This will make individual writes slower (from microseconds to millseconds) but will be really reliable. When
tuning the prefetch to occur rarely (once several seconds or more rare) this will be fine.

#### Disabled persistence

This setting disables the redis configuration check performed for `appendfsync: always` and just uses the indices
directly. There is the risk of indices being used twice if redis crashes and is not configured correctly. This is a
setting to allow negative tests, and it is advised against using it, unless the risks are understood.

### List precreation

To always have lists available that contain free indices, a number of non-empty lists defined by the `precreated-lists`
is created in advance. This prevents delays when lists are created at the same time when indices are requested.

### Status list generation

After updates, the status list JWTs are generated and written to disk. This happens each time the status list has been
updated and the `update-interval` expired, but at least before the written status list JWT itself will expire (as
specified by `list-lifetime`).

The JWTs are signed using a key configured through `signer`.

The status list JWTs are written to the `storage-directory` in the following structure:

```
storage-directory
|- jwts
|  | # status lists JWTs by id
|  |- 5f1ae880-251c-4ec2-b3d0-b61ea62a306d
|  |- 7b57daba-f1a4-480d-9e25-6dd77fcf0fd0
|  \- ...
| # temporary JWT files
|- 5f1ae880-251c-4ec2-b3d0-b61ea62a306d.jwt.tmp
| # status lists metadata files
|- 5f1ae880-251c-4ec2-b3d0-b61ea62a306d.metadata
|- 7b57daba-f1a4-480d-9e25-6dd77fcf0fd0.metadata
\- 00a1386c-93b3-4a3e-81d1-f737df403712.lock # pool lockfile
```

The storage directory can be shared between multiple instances of the service. Locking will be used to prevent
concurrent writing. The data in the JWT directory is consistent at all times: When writing a JWT, it is first written to
a tmp file and then atomically moved to the JWT directory. The metadata files contain the last written version of the
JWT and store the expiration timestamp, to allow updates if the status list changed or if the JWT will expire.

### Status list serving

The written status lists are made available via HTTP by the status list service. For this to work the service must be
reachable via HTTP using the public url. Currently, no HTTP caching measures are used. An alternative is to serve the
lists directly from the provided storage directory via another HTTP server.

## Configuration

The following configuration properties exists:

```yaml
app:
  public-url: http://localhost:8090 # The public url of the status service, used as base URI for the served status lists
  storage-directory: status-lists # The storage directory for serialized status lists
  serve-status-lists: true # default true, if status lists should be made available via HTTP
  cache-duration: 24h # The in memory cache duration of serialized status lists
  storage-type: postgres # 'postgres' to use a postgres database for persistence (also requires a datasource), 'redis' to use a redis
  redis:
    host: localhost # default localhost
    port: 6379 # default 6379
    persistence-strategy: append-fsync-always # configures persistence settings, append-fsync-always or disabled
  status-list-pools:
    c4157bd6-9415-4cdb-af57-0fb91781993b: # one object per status list pool, choose the ID freely, must be a UUID
      # set either api-key or api-keys but not both
      api-key: af05bedc-ec26-472a-af56-f3b862e8e00d # the API-Key used to retrieve indices from the pool and to set the status, keep this secret and share only with the issuer using the pool
      api-keys: # the API-Keys used to retrieve indices from the pool and to set the status, keep this secret and share only with the issuer using the pool, api-keys exists to allow introducing a new api-key while the old is still valid
        - ghi...
        - abc...
        - def...
      issuer: http://localhost:8080 # the iss claim to use in the status lists
      size: 100000 # the size of the status lists (number of indices)
      bits: 1 # the bits of the list (1,2,4, or 8)
      precreation:
        lists: 1 # the number of precreated, full status lists
        check-delay: 10s # The delay used between checks to ensure that the desired amount of precreated lists is available.
      prefetch:
        threshold: 200 # if the number of prefetched entries drops below this value, prefetching is done
        capacity: 400 # the size of the prefetch buffer to reach when a prefetch is done
        on-underflow: delay # delay (default): requests will still be served but slower, fail: requests will return HTTP 429
      update-interval: 60s # the interval at which written status lists JWTs are updated
      list-lifetime: 90s # the duration after which the written status lists JWTs expire
      signer: # the signature settings
        keystore: classpath:/keys/keystore.p12  # a file or resource to use for signing, must contain a single private key with certificate chain
        password: test # the password of the keystore and entry
```

### Configuration value consideration

Different size and timing properties exist that must be set to specific values for a certain use case. This section
gives some hints on how to set those values usefully.

`status-list-pools.*.prefetch.threshold` and `status-list-pools.*.prefetch.capacity`:
This should be set so that `threshold` indices are enough to handle the maximum possible load for as long as it takes
for the new indices to become available. This is in the area of milliseconds for append-fsync-always and as described
above for bgsave. `capacity` should be set to a higher value because this means less fetches. The values set should be
smaller than the list size, in any case smaller than the amount of indices generated through precreation. Higher values
mean more loss of indices on service crashes. Because this is rare, it should not be a large problem though.

`status-list-pools.*.precreation.lists` and `status-list-pools.*.precreation.check-delay`:
The check-delay is the delay which is between checks that the desired amount of precreated lists is available and the
creation of those. The check-delay should be set so that the amount of indices generated this way is larger than the
maximum expected load. For large lists, not only the check delay should be considered but the time to create a list as
well.

`status-list-pools.*.api-key`:
This should be set to a cryptographically strong password or passphrase. Preferably a securely generated string or
random UUID.

`status-list-pools.*.update-interval` and `status-list-pools.*.list-lifetime`:
Those settings depend on the usecase. Writing lists is fast so even short values for `update-interval` like ten seconds
should work. When lots of lists are created and updated, larger values may be needed. `update-inveral` sets a lower
bound to the time it takes until an updated status is available. `list-lifetime` defines the lifetime of a JWT. A jwt is
rewritten before this duration expires to ensure, that a valid JWT is always available.

The implementation schedules a regular task at the rate defined by the update interval. A jwt is rewritten if it was
updated or if it will be expired after two update intervals. Thus, if list-lifetime is smaller than two time the update
interval, lists will be rewritten each time.

#### Example

For this example we assume a maximum load of 20 credential requests per second, each issued in bulk with 10 instances.
This means 200 indices per second are needed.

```yaml
app:
  #...
  redis:
    persistence-strategy: append-fsync-always
  status-list-pools:
    c4157bd6-9415-4cdb-af57-0fb91781993b:
      api-key: af05bedc-ec26-472a-af56-f3b862e8e00d # secure, randomly generated UUID
      issuer: ...
      size: 10000
      bits: ...
      precreation:
        lists: 1 # precreate one list
        check-delay: 40s # allows up to 250 indices per second
      prefetch:
        threshold: 400 # prefetch may take several ms, but we add a security margin of 400 indices as threshold
        capacity: 1400 # this means there will be a prefetch every 5 seconds on maximum load: (1400 - 400) / 200
        on-underflow: delay # service will work but be slower
      update-interval: 1m # updates to lists are available after a maximum of 1 minute
      list-lifetime: 1h # lists will be valid one hour and rewritten before that
      signer: # the signature settings
        keystore: classpath:/keys/keystore.p12  # a file or resource to use for signing, must contain a single private key with certificate chain
        password: test # the password of the keystore and entry
```

If the load exceeds 250 indices per second the lists will be precreated on demand as soon as new indices are needed.
This will lead to a longer prefetch duration, but still work. As soon as the load exceeds 400 indices per seconds the
prefetch buffer will underflow. This will cause delays until a response is generated.

## API

The file [status-list-service.openapi.yml](status-list-service.openapi.yml) contains the OpenAPI specification of the
status list service.

## Redis Storage Schema

| Key                           | Description                                                              |
|-------------------------------|--------------------------------------------------------------------------|
| `pool:config:<poolId>`        | A hash with the pool configuration. *1                                   |
| `pool:precreation:<poolId>`   | A value created with NX flag and removed after the precreation completed |
| `pool:lists:current:<poolId>` | A list containing URIs of lists that have free status available.         |
| `pool:lists:all:<poolId>`     | A set containing the URIs of all lists in the pool.                      |
| `list:indices:<listUri>`      | A list with a random sequence of free list indices.                      |
| `list:config:<listUri>`       | A hash with the list configuration. *2                                   |
| `list:data:<listUri>`         | A bit set with the lists data.                                           |

### *1 Pool configuration

On startup of the service the pool configuration is written to redis if not already done and the existing configuration
in redis is validated against the service configuration.

The pool configuration is a HASH with the following contents:

| Key      | Description                                                                    |
|----------|--------------------------------------------------------------------------------|
| bits     | The bit size of the lists in the pool.                                         |
| size     | The size of the lists in the pool.                                             |
| creation | A value used to synchronize creation of the pools. Either "pending" or "done". |

If service startup fails and a pool remains in state pending permanently, delete it and try starting the service again.

### *2 List configuration

The list configuration is a HASH with the following contents:

| Key     | Description                                            |
|---------|--------------------------------------------------------|
| listId  | The id of the list.                                    |
| bits    | The bit size of the list.                              |
| size    | The size of the list.                                  |
| poolId  | The id of the pool the list belongs to.                |
| version | The version of the list. Incremented with each update. |

## Performance tests

Some performance test results are shown in [performance-tests.md].

## TODOs

- Plan and implement a list expiration feature (so that outdated lists can be deleted and do not need to be
  stored anymore)
- Changeable list size
- API to manage pools
- Generate, store and provide pool statistics
    - Prometheus?
    - Could be relevant: Http stats, References returned, Lists requested, Status updates, Number of lists, Number of
      empty lists
    - Already prepared, relevant information is available at StatsService, number of lists and empty lists can be
      fetched on demand from redis
- Idea: Grow or shrink prefetch buffer size depending on load
- Elaborate: Alternative data storage based on RDBMS
    - Bit string manipulation: https://www.postgresql.org/docs/current/functions-bitstring.html
    - Transaction isolation: https://www.postgresql.org/docs/current/transaction-iso.html
    - Check how an ordered list of next indices can be managed efficiently
