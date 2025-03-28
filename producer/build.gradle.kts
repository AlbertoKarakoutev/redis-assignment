import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.Dockerfile.RunCommandInstruction

plugins {
    id("com.bmuschko.docker-spring-boot-application") version "6.1.4"
}

val buildDir = "src/main/resources"
val buildGroup = "build"
val dockerGroup = "docker"
val producerContainerName = "redis-producer"

val copyResources = tasks.register<Copy>("copyResources") {
    into(layout.buildDirectory.dir(buildDir))
    from(buildDir) {
        include("**/*")
    }
}

val deleteDockerFile = tasks.register("removeDockerfile") {
    group = dockerGroup
    delete("${layout.buildDirectory}/Dockerfile")}

val createDockerfile = tasks.register<Dockerfile>("createDockerfile") {
    group = dockerGroup
    destFile = project.layout.buildDirectory.file("${buildDir}/Dockerfile")
    dependsOn(deleteDockerFile, copyResources)
    from("python:latest")
    copyFile("redis-producer.py", "/main.py")
    copyFile("requirements.txt", "/requirements.txt")
    instructions.add(RunCommandInstruction("pip install -r requirements.txt"))
    entryPoint("python", "main.py")
}

val buildImage = tasks.register<DockerBuildImage>("buildImage") {
    group = dockerGroup
    dependsOn(createDockerfile)
    inputDir.set(createDockerfile.get().destDir)
    images.set(listOf("redis-producer"))
}

// Compatibility

tasks.register("assemble") {
    group = buildGroup
    dependsOn(copyResources)
}

tasks.register("clean") {
    group = buildGroup
    delete(layout.buildDirectory)
}