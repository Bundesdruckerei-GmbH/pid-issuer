/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
plugins { id("openid4vc-libraries.kotlin-library-conventions") }

dependencies {
    api(project(":openid4vc-common"))
    api(project(":status-list"))
    api(libs.sdjwt)

    implementation(libs.nimbusds.jwt)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}
