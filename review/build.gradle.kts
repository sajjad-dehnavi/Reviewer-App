import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

android {
    compileSdk = 36
    namespace = "com.shiragin.review"

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

val sourceJar by tasks.registering(Jar::class) {
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.shiragin"
            artifactId = "review"
            version = "0.0.10"

            artifact(sourceJar.get())
            artifact("$buildDir/outputs/aar/review-release.aar")

            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")

                project.configurations.getByName("implementation").allDependencies.forEach {
                    // Note: The Groovy condition had a bug: 'if (it.group != null || ... ) return' prevents adding deps with valid info.
                    // Assuming you want to skip dependencies that have null group or name or version or name == "unspecified"
                    if (it.group == null || it.version == null || it.name == "unspecified") return@forEach

                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/sajjad-dehnavi/Reviewer-App")
            credentials {
                username = System.getenv("GITHUB_USER") ?: ""
                println("Password: ${System.getenv("GITHUB_PASS")}")
                password = System.getenv("GITHUB_PASS") ?: ""
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.play.review)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.ads)
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha13")
    implementation(libs.tapsell.plus.sdk.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
}