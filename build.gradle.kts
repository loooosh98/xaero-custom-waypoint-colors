plugins {
    id("net.fabricmc.fabric-loom") version "1.16.1"
}

val mod_version: String by project
val maven_group: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val loader_version: String by project
val fabric_version: String by project
val minimap_version: String by project
val worldmap_version: String by project

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
    maven("https://api.modrinth.com/maven")
}

val xaerolibSource: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    compileOnly("maven.modrinth:xaeros-minimap:fabric-$minecraft_version-$minimap_version")
    compileOnly("maven.modrinth:xaeros-world-map:fabric-$minecraft_version-$worldmap_version")

    xaerolibSource("maven.modrinth:xaeros-minimap:fabric-$minecraft_version-$minimap_version")
}

val unpackXaerolib by tasks.registering(Sync::class) {
    from(provider { xaerolibSource.map { zipTree(it).matching { include("META-INF/jars/xaerolib-*.jar") } } })
    eachFile { relativePath = RelativePath(true, name) }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("xaerolib"))
}

dependencies {
    compileOnly(fileTree(layout.buildDirectory.dir("xaerolib")) {
        include("*.jar")
        builtBy(unpackXaerolib)
    })
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minimap_version", minimap_version)

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minimap_version" to minimap_version
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_$archives_base_name" }
    }
}
