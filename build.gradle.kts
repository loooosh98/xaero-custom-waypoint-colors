plugins {
    id("fabric-loom") version "1.16.1"
}

val mod_version: String by project
val maven_group: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project

version = mod_version
group = maven_group

base {
    archivesName.set(archives_base_name)
}

loom {
    accessWidenerPath.set(file("src/main/resources/xaerocustomcolors.accesswidener"))
}

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    compileOnly(files("../xaerominimap-fabric-1.21.11-25.3.10.jar"))
    compileOnly(files("../xaeroworldmap-fabric-1.21.11-1.40.11.jar"))
    compileOnly(files("../xaerolib-fabric-1.21.11-1.1.0.jar"))
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_$archives_base_name" }
    }
}
