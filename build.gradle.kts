plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
    kotlin("jvm") version "2.3.0"
}

group = "mc.jonomore"
version = "3.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "codemc-releases"
        url = uri("https://repo.codemc.io/repository/maven-releases/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.143-beta")
    compileOnly("com.github.retrooper:packetevents-spigot:2.14.0")
    testImplementation(kotlin("test"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.runServer {
    minecraftVersion("26.3")
    downloadPlugins {
        modrinth("packetevents", "2.14.0+spigot")
    }
}