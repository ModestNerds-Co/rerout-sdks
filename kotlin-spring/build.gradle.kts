import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    `java-library`
    `maven-publish`
}

group = "co.rerout"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Pinned versions — a library/starter must not drag the Spring Boot Gradle
// plugin (and its fat-jar packaging) onto consumers.
val springBootVersion = "3.3.5"

dependencies {
    // The base Rerout SDK. Resolved from the sibling composite build during
    // development; from Maven Central on release.
    api("co.rerout:rerout-kotlin:0.1.0")

    api("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    implementation("org.springframework.boot:spring-boot:$springBootVersion")
    implementation("org.springframework:spring-web:6.1.14")
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Spring Boot's Kotlin constructor binding for @ConfigurationProperties
    // reads the primary constructor via kotlin-reflect.
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "rerout-spring-boot-starter"

            pom {
                name.set("Rerout Spring Boot Starter")
                description.set(
                    "Spring Boot auto-configuration for the Rerout branded-link API.",
                )
                url.set("https://github.com/ModestNerds-Co/rerout-sdks")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("codecraft-solutions")
                        name.set("Codecraft Solutions")
                        email.set("hello@codecraftsolutions.co.za")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/ModestNerds-Co/rerout-sdks.git")
                    developerConnection.set("scm:git:ssh://git@github.com/ModestNerds-Co/rerout-sdks.git")
                    url.set("https://github.com/ModestNerds-Co/rerout-sdks")
                }
            }
        }
    }
}
