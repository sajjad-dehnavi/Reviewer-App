import java.io.FileInputStream
import kotlin.apply
import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            val propsFile = file("github.properties")
            val props = Properties().apply {
                load(propsFile.inputStream())
            }
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/sajjad-dehnavi/Reviewer-App")
            credentials {
//                password = "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"
                username = props["username"] as String
                password = props["token"] as String
            }
        }
    }
}

rootProject.name = "Reviewer App"
include(":app")
include(":review")
