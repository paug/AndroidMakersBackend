plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.google.devtools.ksp")
}

kotlin {
    jvm()

    sourceSets {
        getByName("commonMain") {
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
dependencies {
    add("kspCommonMainMetadata", libs.apollo.ksp.incubating)
    add("kspJvm", libs.apollo.ksp.incubating)
}

ksp {
    arg("apolloService", "androidmakers")
    arg("apolloPackageName", "androidmakers.graphql")
}
configureCompilerOptions(17)

tasks.register("run", JavaExec::class.java) {
    classpath(configurations.getByName("jvmRuntimeClasspath"))
    classpath(kotlin.targets.getByName("jvm").compilations.getByName("main").output.classesDirs)
    mainClass.set("androidmakers.service.MainKt")
}

tasks.register("dumpSchema", JavaExec::class.java) {
    classpath(configurations.getByName("jvmRuntimeClasspath"))
    classpath(kotlin.targets.getByName("jvm").compilations.getByName("main").output.classesDirs)
    mainClass.set("androidmakers.service.DumpSchemaKt")
    args("graphql/schema.graphqls")
}

configureDeploy("service", "androidmakers.service.MainKt")

tasks.all {
    if (name == "kspKotlinJvm") {
        finalizedBy("dumpSchema")
    }
}
