plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.kotlin.serialization)
    `maven-publish`
}

kotlin {
    applyDefaultHierarchyTemplate()
    withSourcesJar(false) // Note: No Sources means no Documentation

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvmTarget.get()
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.org.bouncycastle.bcpkix.jdk18on)
                api(libs.co.touchlab.kermit)
                api(libs.com.upokecenter.cbor)
                api(libs.com.augustcellars.cose)
                api(libs.org.jetbrains.kotlinx.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":mdoc-testdata"))
                implementation(libs.junit)
                implementation(libs.com.google.truth)
                implementation(libs.io.mockk)
            }
        }
    }
}

