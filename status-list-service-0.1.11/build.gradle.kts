import com.google.common.io.Files
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

group = "de.bdr.statuslist"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.spring.data.redis)
    implementation(libs.lettuce)
    implementation(libs.spring.boot.starter.logbook)
    implementation(libs.bcpkix)
    implementation(libs.status.list)
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.aspectj:aspectjrt")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.postgresql:postgresql")

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly(libs.logstash.logback.encoder)

    testAndDevelopmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation(libs.bundles.test)
    testImplementation("org.awaitility:awaitility-kotlin")
}

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`

    jacoco

    alias(libs.plugins.spotless)
    id("io.freefair.aspectj.post-compile-weaving") version "8.12.1"
    alias(libs.plugins.open.api.generator)
    id("com.ncorti.ktfmt.gradle") version "0.22.0"
}

sourceSets { main { kotlin { srcDir("${buildDir}/generated/src/main/kotlin") } } }

version = "0.1.11"

java.sourceCompatibility = JavaVersion.VERSION_21

fun propertyOrEnv(name: String, default: String? = null): String =
    optionalPropertyOrEnv(name, default) ?: error("Missing property or env variable $name")

fun optionalPropertyOrEnv(name: String, default: String? = null): String? =
    project.properties[name]?.toString() ?: System.getenv(name) ?: default

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
            it.credentials?.let {
                credentials {
                    username = it.user
                    password = it.password
                }
            }
        }
    }
    mavenLocal()
    mavenCentral()
}

spotless {
    kotlin {
        licenseHeaderFile("${project.rootDir}/license-header.txt")
        //        TODO with kotlin > 2.0.10 ktfmt can not run inside spotless, see
        // https://github.com/facebook/ktfmt/issues/495
        //        But ktfmt works standalone, see below
        //        ktfmt("0.53").kotlinlangStyle()
        targetExclude("build/generated/**/*.*")
    }
}

ktfmt {
    // Google style - 2 space indentation & automatically adds/removes trailing commas
    // googleStyle()

    // KotlinLang style - 4 space indentation - From kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.register("generate-version-file") {
    group = "build"
    description = "Writes the version to version.txt resource"
    val versionFile = layout.buildDirectory.file("resources/main/version.txt")
    outputs.file(versionFile)
    doLast { Files.write(project.version.toString().toByteArray(), versionFile.get().asFile) }
}

tasks.processResources { dependsOn("generate-version-file") }

tasks.test {
    useJUnitPlatform()
    configure<JacocoTaskExtension> {
        destinationFile =
            layout.buildDirectory
                .file("jacoco/" + (System.getenv("JACOCO_FILENAME") ?: "test") + ".exec")
                .get()
                .asFile
    }
}

tasks.compileKotlin { dependsOn("openApiGenerateExtern", "openApiGenerateIntern") }

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(
    "openApiGenerateExtern"
) {
    description = "todo"
    group = "openapi tools"

    inputSpec.set("$rootDir/status-list-service.openapi.yml")
    outputDir.set("$buildDir/generated")
    generatorName.set("kotlin-spring")
    apiPackage.set("de.bdr.statuslist.web.api")
    modelPackage.set("de.bdr.statuslist.web.api.model")
    packageName.set("de.bdr.statuslist.web")
    templateDir.set("$rootDir/src/main/resources/templates")

    configOptions.set(
        mapOf(
            "documentationProvider" to "none",
            "interfaceOnly" to "true",
            "dateLibrary" to "java8",
            "useSpringBoot3" to "true",
            "exceptionHandler" to "false",
            "useTags" to "true",
        )
    )
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(
    "openApiGenerateIntern"
) {
    description = "todo"
    group = "openapi tools"

    inputSpec.set("$rootDir/status-list-service.openapi.intern.yml")
    outputDir.set("$buildDir/generated")
    generatorName.set("kotlin-spring")
    apiPackage.set("de.bdr.statuslist.web.api")
    modelPackage.set("de.bdr.statuslist.web.api.model")
    packageName.set("de.bdr.statuslist.web")

    configOptions.set(
        mapOf(
            "apiSuffix" to "InternalApi",
            "documentationProvider" to "none",
            "interfaceOnly" to "true",
            "dateLibrary" to "java8",
            "useSpringBoot3" to "true",
            "exceptionHandler" to "false",
            "useTags" to "true",
        )
    )
}

tasks.register("testRedis") {
    description = "Runs the integration tests on Redis"
    group = "verification"
    doFirst { tasks.test.configure { systemProperty("spring.profiles.active", "redis") } }
    finalizedBy(tasks.test)
}

tasks.register("testPostgres") {
    description = "Runs the integration tests on Postgres"
    group = "verification"
    doFirst { tasks.test.configure { systemProperty("spring.profiles.active", "postgres") } }
    finalizedBy(tasks.test)
}

tasks.register("bootRunRedis") {
    description = "Runs the application on Redis"
    group = "application"
    doFirst { tasks.bootRun.configure { systemProperty("spring.profiles.active", "redis, local") } }
    finalizedBy(tasks.bootRun)
}

tasks.register("bootRunPostgres") {
    description = "Runs the application on Postgres"
    group = "application"
    doFirst {
        tasks.bootRun.configure { systemProperty("spring.profiles.active", "postgres, local") }
    }
    finalizedBy(tasks.bootRun)
}

tasks.register("bootRunPostgresApiPort") {
    description = "Runs the application on Postgres with different internal api port"
    group = "application"
    doFirst {
        tasks.bootRun.configure {
            systemProperty("spring.profiles.active", "postgres, local, api-port")
        }
    }
    finalizedBy(tasks.bootRun)
}

tasks.jacocoTestReport {
    getExecutionData().setFrom(fileTree(buildDir).include("jacoco/*.exec"))
    reports {
        xml.required.set(true)
        csv.required.set(true)
    }
}

tasks.withType<BootJar> { this.archiveFileName.set("app.jar") }

jacoco { applyTo(tasks.bootRun.get()) }
