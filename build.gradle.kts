tasks.register("setupCredentials") {
    fun File.writeEnv(name:String) {
        parentFile.mkdirs()
        writeText(System.getenv(name))
    }
    doLast {
        if (System.getenv("CI")?.isNotEmpty() == true) {
            println("setting up google services...")
            file("service-graphql/src/main/resources/firebase_service_account_key.json").writeEnv("FIREBASE_SERVICES_JSON")
        }
    }
}

tasks.register("quickChecks") {
    dependsOn(
        ":service-graphql:build",
    )
}
