plugins {
    id("java")
    id("maven-publish")
}

group = "de.winniepat"
version = "2.0"

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
        val publishUsername = providers.gradleProperty("publish.username")
        val publishPassword = providers.gradleProperty("publish.password")
        if (publishUsername.isPresent && publishPassword.isPresent) {
            maven {
                name = "reposilite"
                url = uri("https://maven.winniepat.de/releases")

                credentials {
                    username = publishUsername.get()
                    password = publishPassword.get()
                }
            }
        }

        val gprUser = providers.gradleProperty("gpr.user")
        val gprKey = providers.gradleProperty("gpr.key")
        if (gprUser.isPresent && gprKey.isPresent) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/WinniePatGG/EasyHTTP")

                credentials {
                    username = gprUser.get()
                    password = gprKey.get()
                }
            }
        }
    }
}
