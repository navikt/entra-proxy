
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_26
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.lang.System.getProperty

val javaVersion = JavaLanguageVersion.of(27)
val git = grgit

group = "no.nav.sikkerhetstjenesten.entraproxy"
version = "1.0.1"


plugins {
    id("jacoco")
    alias(libs.plugins.grgit)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.kotest)
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        sourceSets.main.map {
            it.output.classesDirs.asFileTree.matching {
                exclude("**/tilgang/*Swagger*.class", "**/tilgang/dev/*.class")
            }
        }
    )
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}
springBoot {
    mainClass.set("no.nav.sikkerhetstjenesten.entraproxy.EntraProxyApplicationKt")
    buildInfo {
        properties {
            additional = mapOf(
                "kotlin.version" to libs.versions.kotlin.get(),
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
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/sikkerhetstjenesten-felles")
        credentials {
            username = "x-access-token"
            password = providers.environmentVariable("READER_TOKEN").orElse(providers.gradleProperty("gpr.token")).orNull
        }
    }
}


configurations.configureEach {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
}

dependencies {
    implementation(libs.jacksonAnnotations)

    // Kotlin
    implementation(libs.sikkerhetstjenestenLib)
    implementation(libs.kotlinReflect)
    implementation(libs.jackson.module.kotlin)

    // Observability and logging
    implementation(libs.opentelemetryLogbackMdc)
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.logbook)

    // NAV and security
    implementation(libs.bootConditionals)
    implementation(libs.springBootStarterOauth2Client)
    implementation(libs.springBootStarterOauth2ResourceServer)

    // HTTP and API documentation
    implementation(libs.httpclient5)
    implementation(libs.springdocOpenapiStarterWebmvcUi)

    // Spring Boot starters
    implementation(libs.springBootStarterActuator)
    implementation(libs.springBootStarterCache)
    implementation(libs.springBootStarterDataRedis)
    implementation(libs.springSessionDataRedis)
    implementation(libs.springBootStarterGraphql)
    implementation(libs.springBootStarterJetty)
    implementation(libs.springBootStarterRestclient)
    implementation(libs.springBootStarterWeb)

    // Testing
    testImplementation(libs.springMockk)
    testImplementation(libs.mockk)
    testImplementation(libs.caffeine)
    testImplementation(libs.testcontainersRedis)
    testImplementation(libs.springBootTestcontainers)
    testImplementation(libs.springBootStarterDataRedisTest)
    testImplementation(libs.bundles.springBootTest)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.springSecurityTest)
}

dependencyManagement {
    imports {
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${libs.versions.otelAlpha.get()}")
    }
}

tasks.named<BootJar>("bootJar") {
    archiveFileName = "app.jar"
}

tasks.withType<Test>().configureEach {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
    useJUnitPlatform()
}

tasks.register<Test>("unitTest") {
    group = "verification"
    description = "Runs the unit test suite in parallel; skips the Redis/Testcontainers cache tests"
    maxParallelForks = 2
    include("**/*Test.class")
    exclude("**/ValkeyCacheOperationsTest.class")
}

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
}

// Kotlin 2.4.x supports JVM target up to 26, so Java bytecode must match.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(26)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JVM_26)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
