# mdoc Core

A library that provides basic functionality to parse and validate mdocs.

## Importing

The library may be found on the Gitlab Package Registry (partner network).
Add it to your project by adding the following dependency:

```groovy
implementation 'de.bundesdruckerei.mdoc.kotlin:mdoc-core:x.y.z'
```

## Examples

```kotlin
val mdoc = Document.fromBytes(testMdoc)

// verify issuer and device signature
assertTrue(
    mdoc.validate(
        rootCertificates = listOf(cert),
        certificateValidator = {
            verifyCountryNamesInCertificates(it[0], it[1]) && verifyStateNameInCertificates(it[0], it[1])
        },
        readerMacKey = ephemeralReaderKeyPair,
        sessionTranscript = SessionTranscript(deviceEngagementBytes, eReaderKeyBytes, handoverBytes),
    ) is ValidationResult.Success
)

// or

// verify issuer signature
assertTrue(
    mdoc.issuerSigned.validate(
        docType = mdoc.docType,
        rootCertificates = listOf(cert),
        certificateValidator = {
            verifyCountryNamesInCertificates(it[0], it[1]) && verifyStateNameInCertificates(it[0], it[1])
        },
    ) is ValidationResult.Success
)
// verify device signature
assertTrue(
    mdoc.deviceSigned.validate(
        docType = mdoc.docType,
        devicePublicKey = OneKey(mdoc.issuerSigned.issuerAuth.mso.deviceKeyInfo.deviceKey.AsPublicKey(), null),
        readerMacKey = ephemeralReaderKeyPair,
        sessionTranscript = SessionTranscript(deviceEngagementBytes, eReaderKeyBytes, handoverBytes)
    ) is ValidationResult.Success
)

mdoc.issuerSigned.nameSpaces.entries.forEach() {
    println("${it.key} (${it.value.size} entries)")
    it.value.forEach() {
        println("\t ${it.elementIdentifier}: ${it.elementValue}")
    }
}
```
