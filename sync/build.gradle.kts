plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.serialization")
}

configureCompilerOptions(17)

dependencies {
    implementation(libs.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(project(":sessionize"))
    implementation(project(":data"))
}

tasks.register("updateGraphQLData", JavaExec::class.java) {
    mainClass.set("sync.GraphQLMainKt")
    classpath(configurations.getByName("runtimeClasspath"))
    classpath(tasks.named("jar").map { it.outputs.files.singleFile })
}

tasks.register("updateOpenfeedbackData", JavaExec::class.java) {
    mainClass.set("sync.OpenFeedbackMainKt")
    classpath(configurations.getByName("runtimeClasspath"))
    classpath(tasks.named("jar").map { it.outputs.files.singleFile })
}