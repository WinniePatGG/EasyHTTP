plugins {
    id("java")
    id("maven-publish")
}

group = "de.winniepat"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = "easyhttp"
        }
    }

    repositories {
        maven {
            name = "reposilite"
            url = uri("https://maven.winniepat.de/releases")

            credentials {
                username = providers.gradleProperty("reposilite.username").orElse("").get()
                password = providers.gradleProperty("reposilite.password").orElse("").get()
            }
        }

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/WinniePatGG/EasyHTTP")

            credentials {
                username = providers.gradleProperty("gpr.user").orElse("").get()
                password = providers.gradleProperty("gpr.key").orElse("").get()
            }
        }
    }
}

val requiredCredentials = mapOf(
    "publishMavenJavaPublicationToReposiliteRepository" to
        listOf("reposilite.username", "reposilite.password"),
    "publishMavenJavaPublicationToGitHubPackagesRepository" to
        listOf("gpr.user", "gpr.key"),
)

tasks.withType<PublishToMavenRepository>().configureEach {
    val required = requiredCredentials[this.name] ?: emptyList()
    doFirst {
        val missing = required.filterNot { providers.gradleProperty(it).isPresent }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Cannot publish to $name: missing Gradle properties ${missing.joinToString()}. " +
                    "Add them to gradle.properties (gitignored) or pass -P<key>=<value>."
            )
        }
    }
}
