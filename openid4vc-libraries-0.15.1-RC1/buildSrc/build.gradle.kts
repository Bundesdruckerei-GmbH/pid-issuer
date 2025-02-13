/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
plugins { `kotlin-dsl` }

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.1.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:6.0.1.5171")
    implementation("org.owasp:dependency-check-gradle:11.1.0")
    implementation("com.github.jk1:gradle-license-report:2.9")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
}

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
            println(it.url)
            setUrl(it.url)
            if (it.credentials != null) {
                credentials {
                    username = it.credentials.user
                    password = it.credentials.password
                }
            }
        }
    }
    maven("https://plugins.gradle.org/m2/")
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}
