plugins {
    id("org.jetbrains.intellij") version "1.17.2"
    kotlin("jvm") version "1.9.0"
}

group = "com.kulwant"
version = "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.2") // works for IDEs built on 223–255
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
