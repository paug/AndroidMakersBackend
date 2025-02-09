pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            google()
            gradlePluginPortal()
            maven("https://storage.googleapis.com/apollo-snapshots/m2/")
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
include(":sessionize")
include(":data")
include(":sync")
include(":terraform")
include(":module-openfeedback")
include(":service")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "This project needs to be run with Java 17 or higher (found: ${JavaVersion.current()})."
}
