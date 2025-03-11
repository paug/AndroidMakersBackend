import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

private fun Int.toJavaVersion(): JvmTarget = when(this) {
    8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.fromTarget(this.toString())
}
fun Project.configureCompilerOptions(jvmVersion: Int = 11) {
    tasks.withType(KotlinCompilationTask::class.java).configureEach {
        it.compilerOptions {
            (this as? KotlinJvmCompilerOptions)?.jvmTarget?.value(jvmVersion.toJavaVersion())
        }
    }

    project.tasks.withType(JavaCompile::class.java).configureEach {
        // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata of jvm-only modules.
        it.options.release.set(jvmVersion)
    }

    extensions.getByName("java").apply {
        this as JavaPluginExtension
        toolchain {
            it.languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}