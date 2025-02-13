/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
plugins { id("openid4vc-libraries.kotlin-library-conventions") }

dependencies {
    api(project(":openid4vc-common"))
    implementation(project(":status-list"))

    implementation(libs.spring.boot.starter.web)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.kotlin)

    implementation(libs.nimbusds.jwt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.jsonpath)

    implementation(libs.bcpkix)

    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.test.spring)
}
