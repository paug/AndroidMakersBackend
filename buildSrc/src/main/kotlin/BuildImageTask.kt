import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.name

abstract class BuildImageTask : DefaultTask() {

    @get:InputFiles
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:InputFile
    abstract val jarFile: RegularFileProperty

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val imageName: Property<String>

    @get:Input
    @get:Optional
    abstract val gcpServiceAccountJson: Property<String>

    @TaskAction
    fun taskAction() {
        val path = jarFile.get().asFile.toPath()
        val repo = "${imageName.get()}-images"
        val imageRef = "$gcpRegion-docker.pkg.dev/$gcpProjectName/$repo/${imageName.get()}"
        val containerizer = Containerizer.to(RegistryImage.named(imageRef).addCredential("_json_key", gcpServiceAccountJson.get()))


        Jib.from("openjdk:17-alpine")
            .addLayer(listOf(path), AbsoluteUnixPath.get("/"))
            .addLayer(runtimeClasspath.files.map { it.toPath() }, AbsoluteUnixPath.get("/classpath"))
            .setEntrypoint(
                "java",
                "-cp",
                (runtimeClasspath.files.map { "classpath/${it.name}" } + path.name).joinToString(":"),
                mainClass.get())
            .containerize(containerizer)

        logger.lifecycle("Image deployed to '$imageRef'")
    }
}