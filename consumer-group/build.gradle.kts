import org.springframework.boot.gradle.tasks.run.BootRun
import java.nio.charset.StandardCharsets
import java.util.*

plugins {
	java
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.bmuschko.docker-spring-boot-application") version "6.1.4"
}

val dockerGroup = "docker"
val consumerGroupContainerName = "redis-consumer-group"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-json:3.4.4")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	implementation("org.apache.commons:commons-pool2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("com.redis:testcontainers-redis")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val buildImage = tasks.register("buildImage") {
	group = dockerGroup
	dependsOn("bootBuildImage")
}

tasks.named<BootRun>("bootRun") {
	val envProperties = Properties()
	envProperties.load(File("consumer-group/src/main/resources/dev.env").reader(StandardCharsets.UTF_8))
	jvmArgs = envProperties.map {(k, v) -> "-D$k=$v"}
	dependsOn(":composeUp")
	finalizedBy(":composeDown")
}