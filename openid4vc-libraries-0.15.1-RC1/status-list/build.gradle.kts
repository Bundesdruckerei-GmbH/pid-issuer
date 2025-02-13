/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
plugins { id("openid4vc-libraries.kotlin-library-conventions") }

dependencies {
    api(libs.nimbusds.jwt)
    api(libs.cose)
    api(libs.cbor)
    api(project(":openid4vc-common"))
    implementation(libs.caffeine)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.test)
}
