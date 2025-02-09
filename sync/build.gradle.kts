plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

configureCompilerOptions(17)

dependencies {
    implementation(libs.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(project(":sessionize"))
    implementation(project(":data"))
    implementation("com.opencsv:opencsv:5.7.1")
}


fun registerMainTask(name: String) {
    tasks.register(name, JavaExec::class.java) {
        mainClass.set("sync.$name")
        classpath(configurations.getByName("runtimeClasspath"))
        classpath(tasks.named("jar").map { it.outputs.files.singleFile })
    }
}

registerMainTask("GraphQLMainKt")
registerMainTask("OpenFeedbackMainKt")
registerMainTask("OpenFeedbackMainKtCSVMainKt")
