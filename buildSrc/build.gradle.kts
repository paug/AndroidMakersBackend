plugins {
    `embedded-kotlin`
}

dependencies {
    implementation(platform(libs.google.cloud.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.cloud.storage)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.android.application)
    implementation(libs.plugin.apollo)
    implementation(libs.plugin.ksp)
    implementation(libs.plugin.kmp.nativecoroutines)
    implementation(libs.plugin.kotlin.serialization)
    implementation(libs.plugin.kotlin.spring)
    implementation(libs.plugin.spring.boot)
    implementation(libs.plugin.appengine)
    implementation(libs.plugin.kmmbridge)
    implementation(libs.plugin.google.services)
    implementation(libs.plugin.firebase.crashlytics)
    implementation(libs.plugin.wire)
    implementation(libs.jib.core)
    implementation(libs.google.cloud.storage)
    implementation(libs.google.cloud.run)
    implementation(libs.kotlinx.datetime)
    // See https://github.com/quarkiverse/quarkus-google-cloud-services/issues/547#issuecomment-1878765438
    implementation("io.grpc:grpc-netty:1.60.1")
}