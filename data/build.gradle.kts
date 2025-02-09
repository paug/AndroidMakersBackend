plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

configureCompilerOptions(17)

dependencies {
    implementation(libs.kotlinx.serialization)
    implementation(libs.okhttp)
    api(project(":sessionize"))
}
