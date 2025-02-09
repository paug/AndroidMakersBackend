plugins {
    `embedded-kotlin`
}

dependencies {
    implementation(platform(libs.google.cloud.bom))
    implementation(libs.google.cloud.storage)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.ksp)
    implementation(libs.plugin.kotlin.serialization)
    implementation(libs.plugin.apollo.execution)
    implementation(libs.jib.core)
    implementation(libs.google.cloud.storage)
    implementation(libs.google.cloud.run)
    implementation(libs.kotlinx.datetime)
    // See https://github.com/quarkiverse/quarkus-google-cloud-services/issues/547#issuecomment-1878765438
    implementation("io.grpc:grpc-netty:1.60.1")
}