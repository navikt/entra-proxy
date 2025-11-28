import org.springframework.boot.gradle.tasks.bundling.BootJar

val javaVersion = JavaLanguageVersion.of(21)
val springdocVersion = "3.0.0"
val tokenSupportVersion = "6.0.0"
val mockkVersion = "1.14.6"
val logstashEncoderVersion = "9.0"
val springMockVersion = "4.0.2"
val conditionalsVersion = "5.1.11"
val coroutinesVersion = "1.10.2"

group = "no.nav.sikkerhetstjenesten.entraproxy"
version = "1.0.1"


plugins {
    val kotlinVersion = "2.2.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("java")
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.cyclonedx.bom") version "2.4.1"
    id("com.google.cloud.tools.jib") version "3.4.5"
    application
}
springBoot {
    buildInfo {
        properties {
            additional = mapOf(
                "kotlin.version" to "2.2.20",
                "jdk.version" to java.toolchain.languageVersion.get().toString(),
                "jdk.vendor" to System.getProperty("java.vendor")
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
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.20.1")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:2.20.1-alpha")
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework:spring-aspects")
    testImplementation("com.ninja-squad:springmockk:$springMockVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
}

dependencyManagement {
    imports {
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.20.1")
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
