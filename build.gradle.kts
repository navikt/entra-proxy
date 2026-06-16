import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.lang.System.getProperty
import kotlin.KotlinVersion.Companion.CURRENT

val javaVersion = JavaLanguageVersion.of(26)
val git = grgit

group = "no.nav.sikkerhetstjenesten.entraproxy"
version = "1.0.1"


plugins {
    id("jacoco")
    alias(libs.plugins.grgit)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.kotest)
    application
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("**/tilgang/*Swagger*.class", "**/tilgang/dev/*.class")
            }
        })
    )
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
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


configurations.configureEach {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}

dependencies {
    // Kotlin
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.kotlinReflect)

    // Observability and logging
    implementation(libs.opentelemetryInstrumentationAnnotations)
    implementation(libs.opentelemetryLogbackMdc)
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logstashLogbackEncoder)

    // NAV and security
    implementation(libs.bootConditionals)
    implementation(libs.tokenClientSpring)
    implementation(libs.tokenValidationSpring)

    // HTTP and API documentation
    implementation(libs.httpclient5)
    implementation(libs.springdocOpenapiStarterWebmvcUi)

    // Spring Boot starters
    implementation(libs.springBootStarterActuator)
    implementation(libs.springBootStarterCache)
    implementation(libs.springBootStarterDataRedis)
    implementation(libs.springBootStarterJetty)
    implementation(libs.springBootStarterRestclient)
    implementation(libs.springBootStarterValidation)
    implementation(libs.springBootStarterWeb)
    implementation(libs.springBootStarterWebclient)
    implementation(libs.springBootStarterWebflux)
    implementation(libs.springAspects)

    // Testing
    testImplementation(libs.springMockk)
    testImplementation(libs.mockk)
    testImplementation(libs.junitJupiter)
    testImplementation(libs.bundles.springBootTest)
    testImplementation(libs.bundles.kotest)
    testImplementation(kotlin("test"))
}

dependencyManagement {
    imports {
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${libs.versions.otel.get()}")
    }
}

application {
    mainClass.set("no.nav.sikkerhetstjenesten.entraproxy.EntraProxyApplicationKt")
}
tasks.named<BootJar>("bootJar") {
    archiveFileName = "app.jar"
}

tasks.named<Test>("test") {
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
