import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.lang.System.getProperty
import kotlin.KotlinVersion.Companion.CURRENT

val javaVersion = JavaLanguageVersion.of(25)
val springdocVersion = "3.0.3"
val tokenSupportVersion = "6.0.3"
val kotestVersion = "6.1.11"
val mockkVersion = "1.14.6"
val logstashEncoderVersion = "9.0"
val springMockVersion = "4.0.2"
val conditionalsVersion = "6.0.3"
val coroutinesVersion = "1.10.2"
val otelVersion = "2.22.0"
val git = grgit

group = "no.nav.sikkerhetstjenesten.entraproxy"
version = "1.0.1"


plugins {
    val kotlinVersion = "2.3.10"
    id("jacoco")
    id("org.ajoberstar.grgit") version "5.2.2"
    id("org.jetbrains.dokka") version "2.1.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.cyclonedx.bom") version "3.2.4"
    id("io.kotest") version "6.1.11"
    id("com.google.cloud.tools.jib") version "3.4.5"
    application
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}
springBoot {
    buildInfo {
        properties {
            additional = mapOf(
                "kotlin.version" to "$CURRENT",
                "jdk.version" to "$javaVersion",
                "jdk.vendor" to getProperty("java.vendor"),
                "git.branch" to git.branch.current().name,
                "git.commit.id" to git.head().abbreviatedId,
                "git.commit.time" to git.head().dateTime.toString()
            )
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}


configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:$otelVersion-alpha")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("no.nav.boot:boot-conditionals:$conditionalsVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework:spring-aspects")
    testImplementation("com.ninja-squad:springmockk:$springMockVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("io.kotest:kotest-runner-junit5:${kotestVersion}")
    testImplementation("io.kotest:kotest-assertions-core:${kotestVersion}")
    testImplementation("io.kotest:kotest-extensions-spring:${kotestVersion}")
    testImplementation(kotlin("test"))
}

dependencyManagement {
    imports {
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:$otelVersion")
    }
}

application {
    mainClass.set("no.nav.sikkerhetstjenesten.entraproxy.EntraProxyApplicationKt")
}
tasks.withType<BootJar> {
    archiveFileName = "app.jar"
}

tasks.test {
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
}

kotlin {
    jvmToolchain(javaVersion.asInt())

    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
