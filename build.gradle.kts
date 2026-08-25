plugins {
    id("net.fabricmc.fabric-loom") version "1.16.1" apply false
}

val maven_group: String by project

subprojects {
    apply(plugin = "java")

    group = maven_group

    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://api.modrinth.com/maven")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}
