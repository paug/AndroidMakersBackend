plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
    id("com.apollographql.execution")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.apollo.execution.ktor)
    implementation(libs.kotlinx.datetime)
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.kotlinx.serialization)
    implementation(libs.firebase.admin)
    implementation(libs.google.cloud.datastore)
    implementation(project(":module-openfeedback"))
    implementation(libs.ktor.server.netty)
    implementation(libs.slf4j.simple)
}

apolloExecution {
    service("AndroidMakers") {
        packageName.set("androidmakers.graphql")
    }
}

configureCompilerOptions(17)

tasks.register("run", JavaExec::class.java) {
    classpath(configurations.getByName("runtimeClasspath"))
    classpath(kotlin.target.compilations.getByName("main").output.classesDirs)
    mainClass.set("androidmakers.service.MainKt")
}

configureDeploy("service", "androidmakers.service.MainKt")
