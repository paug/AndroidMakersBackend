#!/usr/bin/env kotlin

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.system.exitProcess

fun runCommand(vararg args: String): String {
    val builder = ProcessBuilder(*args)
        .redirectError(ProcessBuilder.Redirect.INHERIT)

    val process = builder.start()
    val ret = process.waitFor()

    val output = process.inputStream.bufferedReader().readText()
    if (ret != 0) {
        throw java.lang.Exception("command ${args.joinToString(" ")} failed:\n$output")
    }

    return output.trim()
}


if (runCommand("git", "status", "--porcelain").isEmpty()) {
    println("No change detected")
    exitProcess(0)
}

val now = LocalDateTime.ofEpochSecond(Instant.now().epochSecond, 0, ZoneOffset.UTC)
val slug = "${now.year}-${now.monthValue}-${now.dayOfMonth}_${now.hour}-${now.minute}"
val branchName = "update_$slug"
runCommand("git", "checkout", "-b", branchName)
runCommand("git", "add", ".")
runCommand("git", "commit", "-a", "-m", "Update data at $slug")
runCommand("git", "push", "origin", branchName)
runCommand("gh", "pr", "create", "--fill")