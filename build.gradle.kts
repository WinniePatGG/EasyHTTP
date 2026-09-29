plugins {
    id("java")
    id("maven-publish")
}

group = "de.winniepat"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "reposilite"
            url = uri("https://maven.winniepat.de/releases")

            credentials {
                username = providers.gradleProperty("publish.username").get()
                password = providers.gradleProperty("publish.password").get()
            }
        }
    }
}