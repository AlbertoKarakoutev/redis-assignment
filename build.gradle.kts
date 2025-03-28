group = "com.akarakoutev"
version = "0.0.1-SNAPSHOT"

plugins {
    java
    id("com.bmuschko.docker-spring-boot-application") version "6.1.4"
    id ("com.avast.gradle.docker-compose") version "0.17.12"
}

docker {
    url = providers.gradleProperty("docker.socket")
}

dockerCompose {
    useComposeFiles.set(listOf("compose.yaml"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

subprojects {
    repositories {
        mavenCentral()
    }
}

tasks.named("composeUp") {
    dependsOn(":producer:buildImage")
}