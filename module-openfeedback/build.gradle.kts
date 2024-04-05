plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvm()

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.kotlinx.serialization)
                implementation(libs.atomicfu)
                implementation(project(":sessionize"))
            }
        }
    }
}

configureCompilerOptions(17)

