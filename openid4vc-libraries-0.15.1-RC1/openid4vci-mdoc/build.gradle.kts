/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
plugins { id("openid4vc-libraries.kotlin-library-conventions") }

dependencies {
    api(project(":openid4vci"))
    api(libs.cbor)

    implementation(libs.mdoc.core) {
        // excluded due to CVE-2023-33202, replaced by bcprov-jdk18on
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }

    implementation(libs.nimbusds.jwt)
    implementation(libs.kotlinx.serialization.json)

    // bcprov-jdk15on has been renamed to bcprov-jdk18on, it is excluded as transitive dependency
    // from other dependencies due to CVE-2023-33202, this is the up to date replacement
    runtimeOnly(libs.bcprov)

    testImplementation(libs.bundles.test)
}
