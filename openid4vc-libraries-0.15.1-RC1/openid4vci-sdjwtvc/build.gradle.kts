/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
plugins { id("openid4vc-libraries.kotlin-library-conventions") }

dependencies {
    api(project(":openid4vci"))
    api(libs.sdjwt)

    implementation(libs.bcpkix)
    implementation(libs.nimbusds.jwt)

    testImplementation(libs.bundles.test)
}
