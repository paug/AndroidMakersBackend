val file = layout.buildDirectory.file("service-account.json").get().asFile

val gcpServiceAccountJson by lazy {
    System.getenv("GOOGLE_APPLICATION_CREDENTIALS_CONTENT") ?: error("No GOOGLE_APPLICATION_CREDENTIALS_CONTENT found")
}
val createGcpCredentials = tasks.register("createGcpCredentials") {
    doLast {
        file.parentFile.mkdirs()
        file.writeText(gcpServiceAccountJson)
    }
}

val init = tasks.register("init", Exec::class.java) {
    dependsOn(createGcpCredentials)
    environment("GOOGLE_APPLICATION_CREDENTIALS", file.absolutePath)
    commandLine("terraform", "init")
}

tasks.register("apply", Exec::class.java) {
    dependsOn(init)
    environment("GOOGLE_APPLICATION_CREDENTIALS", file.absolutePath)
    commandLine("terraform", "apply", "-auto-approve")
}

tasks.register("plan", Exec::class.java) {
    dependsOn(init)
    environment("GOOGLE_APPLICATION_CREDENTIALS", file.absolutePath)
    commandLine("terraform", "plan")
}
