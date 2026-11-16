import dev.detekt.gradle.Detekt
import dev.detekt.gradle.plugin.getSupportedKotlinVersion
import org.gradle.api.JavaVersion.toVersion
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.Companion.fromTarget
import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOOT_JAR_TASK_NAME
import org.springframework.boot.gradle.tasks.bundling.BootJar

val javaVersion = JavaLanguageVersion.of(27)
val jvmTargetVersion = 26

group = "no.nav.sikkerhetstjenesten.entraproxy"
version = "1.0.1"
extra["jackson-2-bom.version"] = providers.gradleProperty("jackson2Version").get()
extra["logback.version"] = "1.6.7"

plugins {
    jacoco
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.git.properties)
    alias(libs.plugins.kotest)
}

detekt {
    buildUponDefaultConfig = true
    ignoreFailures = true
    config.setFrom(file("config/detekt/detekt.yml"))
    baseline = file(if (providers.gradleProperty("detektUtenBaseline").isPresent)
        "build/ingen-baseline.xml" else "detekt-baseline.xml")
}

configurations.named("detekt") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(getSupportedKotlinVersion())
        }
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        markdown.required = false
        sarif.required = true
    }
}

java {
    sourceCompatibility = toVersion(jvmTargetVersion)
    targetCompatibility = toVersion(jvmTargetVersion)

    toolchain {
        languageVersion.set(javaVersion)
    }
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
            val javaMetadata = javaToolchains.launcherFor(java.toolchain).get().metadata
            additional = mapOf(
                "kotlin.version" to libs.versions.kotlin.get(),
                "jdk.version" to javaMetadata.languageVersion.asInt().toString(),
                "jdk.vendor" to javaMetadata.vendor,
                "spring-boot.version" to libs.versions.spring.boot.get(),
            )
        }
    }
}

gitProperties {
    keys = listOf(
        "git.branch",
        "git.commit.id",
        "git.commit.id.abbrev",
        "git.commit.time",
        "git.dirty",
        "git.build.version",
    )
    dateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}


configurations.configureEach {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
}

dependencies {
    implementation(libs.boot.conditionals)
    implementation(libs.bundles.observability)
    implementation(libs.httpclient5)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.logbook.spring.boot.starter)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.sikkerhetstjenesten.lib)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.graphql)
    implementation(libs.spring.boot.starter.jetty)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.restclient)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.session.data.redis)
    implementation(libs.springdoc.openapi.webmvc.ui)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.caffeine)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.mockk)
    testImplementation(libs.spring.boot.starter.data.redis.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.springmockk)
}

dependencyManagement {
    imports {
        mavenBom(
            "io.opentelemetry.instrumentation:" +
                "opentelemetry-instrumentation-bom-alpha:" +
                "${libs.versions.opentelemetry.get()}-alpha"
        )
    }
}

tasks.named<BootJar>(BOOT_JAR_TASK_NAME) {
    archiveFileName = "app.jar"
}

val npmInstall = tasks.register<Exec>("npmInstall") {
    description = "Installs the frontend dependencies using npm"
    inputs.files("package.json", "package-lock.json")
    outputs.dir("node_modules")
    commandLine("npm", "ci", "--no-audit", "--no-fund")
}

val compileTypeScript = tasks.register<Exec>("compileTypeScript") {
    description = "Compiles the TypeScript frontend code"
    dependsOn(npmInstall)
    inputs.files("package.json", "package-lock.json", "tsconfig.json")
    inputs.dir("src/main/typescript")
    outputs.dir(layout.buildDirectory.dir("generated/frontend"))
    commandLine("npm", "run", "build")
}

val frontendTest = tasks.register<Exec>("frontendTest") {
    group = "verification"
    description = "Runs the frontend tests"
    dependsOn(compileTypeScript)
    commandLine("npm", "run", "test:frontend")
}

tasks.processResources {
    dependsOn(compileTypeScript)
    from(layout.buildDirectory.dir("generated/frontend"))
}

tasks.named(CHECK_TASK_NAME) {
    dependsOn(frontendTest)
}

tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
        "-Xshare:off",
    )
    useJUnitPlatform()
}

tasks.register<Test>("unitTest") {
    group = "verification"
    description = "Runs the unit test suite in parallel; skips the Redis/Testcontainers cache tests"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    maxParallelForks = 2
    include("**/*Test.class")
    exclude("**/ValkeyCacheOperationsTest.class")
}

kotlin {
    compilerOptions {
        jvmTarget.set(fromTarget(jvmTargetVersion.toString()))
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
