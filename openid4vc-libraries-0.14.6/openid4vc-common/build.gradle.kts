/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
plugins { id("openid4vc-libraries.kotlin-library-conventions") }

dependencies {
    api(libs.nimbusds.jwt)
    api(libs.cose)

    implementation(libs.cbor)
    implementation(libs.bcprov)
    implementation(libs.bcpkix)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}
