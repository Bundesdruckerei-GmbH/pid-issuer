// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.org.jacoco.core)
        classpath(libs.org.owasp.dependency.check.gradle)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform) apply false
}

subprojects {
    group = "de.bundesdruckerei.mdoc.kotlin"
    version = "0.19.0"
}
