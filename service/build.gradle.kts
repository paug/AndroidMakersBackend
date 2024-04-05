plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cors)
                implementation(project(":module-openfeedback"))
            }
        }

        getByName("jvmMain") {
            dependencies {
                implementation(libs.ktor.server.netty)
                implementation(libs.slf4j.simple)
            }
        }
    }
}

configureCompilerOptions(17)

tasks.register("run", JavaExec::class.java) {
    classpath(configurations.getByName("jvmRuntimeClasspath"))
    classpath(kotlin.targets.getByName("jvm").compilations.getByName("main").output.classesDirs)
    mainClass.set("androidmakers.service.MainKt")
}

configureDeploy("service", "androidmakers.service.MainKt")