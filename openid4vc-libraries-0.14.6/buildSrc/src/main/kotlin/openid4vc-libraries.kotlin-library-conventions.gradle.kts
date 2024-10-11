/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
import com.github.jk1.license.render.JsonReportRenderer
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    `maven-publish`

    id("org.sonarqube")
    id("org.owasp.dependencycheck")
    id("com.github.jk1.dependency-license-report")
    jacoco

    id("com.diffplug.spotless")
}

group = "de.bdr.openid4vc"

version = project.findProperty("PROJECT_VERSION") ?: "0.14.6"

java.sourceCompatibility = JavaVersion.VERSION_21

fun optionalPropertyOrEnv(name: String, default: String? = null): String? =
    project.properties[name]?.toString() ?: System.getenv(name) ?: default

fun propertyOrEnv(name: String): String =
    project.properties[name]?.toString() ?: System.getenv(name)

data class MavenCredentials(val user: String, val password: String)

data class MavenRepo(val url: String, val credentials: MavenCredentials? = null)

val mavenRepos =
    mutableListOf<MavenRepo>().apply {
        var index = 0
        while (optionalPropertyOrEnv("MAVEN_REPO_$index") != null) {
            val url = propertyOrEnv("MAVEN_REPO_$index")
            val user = optionalPropertyOrEnv("MAVEN_REPO_${index}_USER")
            val password = optionalPropertyOrEnv("MAVEN_REPO_${index}_PASSWORD")
            if (user != null && password != null) {
                add(MavenRepo(url, MavenCredentials(user, password)))
            } else {
                add(MavenRepo(url))
            }
            index++
        }
    }

repositories {
    mavenRepos.forEach {
        maven {
            setUrl(it.url)
            if (it.credentials != null) {
                credentials {
                    username = it.credentials.user
                    password = it.credentials.password
                }
            }
        }
    }
    mavenLocal()
    mavenCentral()
}

tasks.register<Jar>("sourcesJar") {
    group = "build"
    description = "Builds a sources jar to be published by maven"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "de.bdr.ssi.${project.name}")
        property("sonar.projectName", project.name)
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.coveragePlugin", "jacoco")
        property("sonar.java.binaries", "${layout.buildDirectory.get()}/classes/java/main")
        println(layout.buildDirectory.get())
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml"
        )
        // DependencyCheck integration
        property(
            "sonar.dependencyCheck.htmlReportPath",
            "${layout.buildDirectory.get()}/reports/dependency-check/dependency-check-report.html"
        )
        property(
            "sonar.dependencyCheck.jsonReportPath",
            "${layout.buildDirectory.get()}/reports/dependency-check/dependency-check-report.json"
        )
        property(
            "sonar.dependencyCheck.xmlReportPath",
            "${layout.buildDirectory.get()}/reports/dependency-check/dependency-check-report.xml"
        )
        property("sonar.dependencyCheck.summarize", "true")
    }
}

licenseReport {
    renderers = arrayOf(JsonReportRenderer("license-details.json", false))
    excludeBoms = false
}

spotless {
    kotlin {
        licenseHeaderFile("${project.rootDir}/buildSrc/license-header.txt")
        ktfmt("0.49").kotlinlangStyle()
    }
    java {
        licenseHeaderFile("${project.rootDir}/buildSrc/license-header.txt")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(true)
    }
}

tasks.create<SonarQualitygate>("sonarQualitygate") { group = "verification" }
