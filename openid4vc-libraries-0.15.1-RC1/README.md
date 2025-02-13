# OpenID4VC Library

This library implements standards around digital credentials.

## Modules

The following is lists and describes the contained modules.

| Module                                    | Description                                                                                                                                                                                                         |
|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| openid4vc-common                          | Datastructures to build and parse issuance and verification structures, which are useful for issuers, verifiers and wallets. An abstraction layer for credentials to be used during verification and in the wallet. |
| openid4vc-common-msomdoc                  | mdoc credential implementation based on mdoc core.                                                                                                                                                                  |
| openid4vc-common-sdjwtvc                  | SD-JWT VC credential implementation based on the EUDI SD-JWT VC library.                                                                                                                                            |
| openid4vci                                | OpenID4VCI Issuer implementation including a spring module.                                                                                                                                                         |
| openid4vci-mdoc                           | OpenID4VCI Issuer implementation for mdoc credentials.                                                                                                                                                              |
| openid4vci-sdjwtvc                        | OpenID4VCI Issuer implementation for sdjwtvc credentials.                                                                                                                                                           |
| openid4vci-http-status-list-client        | Status list client (to fetch a status list) to be used by the openid4vci module                                                                                                                                     |
| openid4vci-http-status-list-client-spring | Status list client spring binding                                                                                                                                                                                   |
| openid4vp                                 | OpenID4VP Verifier implementation with mdoc and SD-JWT VC support including a spring module.                                                                                                                        |
| status-list                               | Basic structures to handle token status lists                                                                                                                                                                       |

## Standards and features

The following table lists the implemented standards and features thereof. Parts that were left out explicitly are
mentioned as well.

| Standard          | Version | Implemented features | Omitted features | URI  |
|-------------------|---------|----------------------|------------------|------|
| OpenID4VCI        | TODO    | TODO                 | TODO             | TODO |
| OpenID4VP         | TODO    | TODO                 | TODO             | TODO |
| HAIP              | TODO    | TODO                 | TODO             | TODO |
| ISO-18013-7       | TODO    | TODO                 | TODO             | TODO |
| Token Status List | TODO    | TODO                 | TODO             | TODO |

## Known TODOs

This library evolved in parallel to the standards, which are still not finalized. Development was performend to check
the standards, provide POCs and develop demo systems. The contained code is not necessarily meant to be used as is in a
productive environment. The developers know of the following things that need to be considered when developing this
library further. Nevertheless, there may and will be more things and a thorough review is recommended.

- Add unit tests to increase test coverage, only some parts are tested
- Add end-to-end tests for issuance and verification*
- Document the code thoroughly
- Currently spring is not mandatory but this choice lead to introduction of an HTTP and storage abstraction layer
    - Maybe split the spring logic into separate components or remove the HTTP abstraction layer and make spring
      mandatory
    - Maybe remove the storage abstraction layer and make a specific technology like spring-data mandatory or move
      instance creation of stored data classes to the storage classes and only use interfaces in the code
- Maybe split authorization server and issuer
- Maybe allow to reconfigure the running issuer

*During development, changes were tested manually by using the command line wallet 