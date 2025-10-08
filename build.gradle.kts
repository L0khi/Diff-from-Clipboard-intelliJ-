plugins {
    id("org.jetbrains.intellij") version "1.17.2"
    kotlin("jvm") version "1.9.0"
}
kotlin {
    jvmToolchain(17)
}

group = "com.yourorg"
version = "0.3.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.2")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("224")
        untilBuild.set("255.*")
    }
    buildSearchableOptions {
        enabled = false
    }
}
