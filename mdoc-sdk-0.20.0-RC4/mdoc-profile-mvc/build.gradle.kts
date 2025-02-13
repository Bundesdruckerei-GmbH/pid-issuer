import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.JsonReportRenderer

plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.kotlin.serialization)
    `maven-publish`
    alias(libs.plugins.jk1.license.report)
    alias(libs.plugins.io.gitlab.detekt)
}

kotlin {
    applyDefaultHierarchyTemplate()
    withSourcesJar(false) // Note: No Sources means no Documentation

    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvmTarget.get()
            }
        }
    }

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
                api(project(":mdoc-profile"))
                implementation(libs.org.jetbrains.kotlinx.serialization.cbor)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
    }
}

android {
    namespace = "$group.profile.mvc"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testOptions {
            execution = "ANDROIDX_TEST_ORCHESTRATOR"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

detekt {
    ignoreFailures = true
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    source.setFrom("src/commonMain/kotlin")
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    nvd.datafeedUrl = "https://nistdata.partner.bdr.de/2.0/nvdcve-{0}.json.gz"
    hostedSuppressions.url = "https://nistdata.partner.bdr.de/additional/publishedSuppressions.xml"
    analyzers.knownExploitedURL =
        "https://nistdata.partner.bdr.de/additional/known_exploited_vulnerabilities.json"
    analyzers.retirejs.retireJsUrl = "https://nistdata.partner.bdr.de/additional/jsrepository.json"
    analyzers.ossIndex.enabled = false
}

licenseReport {
    configurations = arrayOf("commonMainResolvableDependenciesMetadata")
    outputDir =
        rootProject.layout.buildDirectory.dir("reports/dependency-license-profile-mvc")
            .get().asFile.toString()
    allowedLicensesFile = File("$projectDir/config/allowed-licenses.json")
    excludeBoms = true
    excludes = arrayOf(
        // these are BOMs, the licenses are attached to the actual implementations
        "org.jetbrains.kotlin:kotlin-stdlib-common",
    )
    renderers = arrayOf(
        JsonReportRenderer(
            "license-details.json",
            false
        )
    )
    filters = arrayOf(
        LicenseBundleNormalizer(
            "$projectDir/config/license-normalizer-bundle.json",
            true
        )
    )

}

publishing {
    repositories {
        val userName = extra.get("artifactoryUsername") as String
        val userPassword = extra.get("artifactoryPassword") as String
        val repoUri = uri(extra.get("artifactoryRepositoryUrl") as String)

        maven {
            url = repoUri
            credentials {
                username = userName
                password = userPassword
            }
        }
    }
}
