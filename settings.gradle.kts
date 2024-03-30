pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            google()
            gradlePluginPortal()

            exclusiveContent {
                forRepository {
                    maven {
                        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    }
                }
                filter {
                    // Use the snapshots repository for Apollo 4.0.0-SNAPSHOT, but not for 3.x, which is a dependency of 4.0.0
                    includeVersionByRegex(
                        "com\\.apollographql\\.apollo3",
                        ".+",
                        "4\\.0\\.0-SNAPSHOT"
                    )
                }
            }
        }
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                // Appengine plugin doesn't publish the marker
                "com.google.cloud.tools.appengine" -> useModule("com.google.cloud.tools:appengine-gradle-plugin:${requested.version}")
            }
        }
    }
}

rootProject.name = "AndroidMakersBackend"
include(":service-graphql")
include(":sessionize")
include(":data")
include(":sync")
include(":terraform")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "This project needs to be run with Java 17 or higher (found: ${JavaVersion.current()})."
}
